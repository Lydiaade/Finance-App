package com.controller;

import com.dto.BankAccount;
import com.dto.PayeeSegmentRule;
import com.dto.Transaction;
import com.repository.AccountRepository;
import com.repository.PayeeSegmentRuleRepository;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// FM-19 AC doc §7 "Integration" - the one checklist item that wasn't covered by any existing
// test: uploading a CSV containing a paid_to that already has a rule must result in the imported
// transactions carrying the rule's segment, end to end (real UploadController -> UploadService ->
// CSVHelper -> real repositories against the H2 test database), not just CSVHelperTest's direct,
// mocked-repository call into CSVHelper.transformFileToTransactions.
//
// @Transactional at the class level: each @Test method's DB writes (seeded BankAccount/
// PayeeSegmentRule, imported Transaction rows) are rolled back after the method completes, so the
// two tests here don't leak state into each other via the shared H2 test database/Spring context
// (there's no per-test-class schema reset - ddl-auto: create-drop only fires once per context).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UploadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PayeeSegmentRuleRepository payeeSegmentRuleRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // Matches the position-based column layout CSVHelper expects (see CSVHelper javadoc): the
    // account column must match the seeded BankAccount's sortCode/accountNumber, and the sixth
    // column packs "paid_to\tmemo\t" inside quotes, same shape as src/test/resources/testData.csv.
    private static final String CSV_CONTENT =
            "Number,Date,Account,Amount,Subcategory,Memo\n"
                    + "0,31/03/2022,SORTNUMBER ACCNUMBER,-9,Debit,\"BAR BRUNO             \tON 29 MAR CPM\t\"\n";

    @Test
    void csvUploadWithExistingPayeeRuleAppliesRuleSegmentToImportedTransactions() throws Exception {
        BankAccount account = accountRepository.save(
                new BankAccount("Current Account", "SORTNUMBER", "ACCNUMBER", BigDecimal.valueOf(2000), LocalDate.now()));
        payeeSegmentRuleRepository.save(new PayeeSegmentRule("BAR BRUNO", "Eating Out"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fm19IntegrationUpload.csv",
                "text/csv",
                CSV_CONTENT.getBytes(StandardCharsets.UTF_8));

        // Assert on the upload response itself: one successful transaction, no failures.
        mockMvc.perform(multipart("/uploads/upload")
                        .file(file)
                        .param("bankAccount", String.valueOf(account.getId())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.successfulTransactions").value(1))
                .andExpect(jsonPath("$.failedTransactions").value(0));

        // Assert on the persisted state directly.
        Transaction persisted = transactionRepository.findAll().stream()
                .filter(t -> "BAR BRUNO".equals(t.getPaid_to()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Imported transaction was not persisted"));
        assertEquals("Eating Out", persisted.getSegment());

        // Assert via the follow-up GET /transactions the AC doc explicitly calls out.
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.paid_to == 'BAR BRUNO')].segment").value("Eating Out"));
    }

    // Regression companion to the above, mirroring CSVHelperTest's existing no-rule case but
    // exercised through the full HTTP upload path rather than calling CSVHelper directly - proves
    // the DB-backed rule lookup wired into CSVHelper doesn't accidentally apply a segment when no
    // rule exists for the paid_to.
    @Test
    void csvUploadWithNoMatchingPayeeRuleStillDefaultsToUndefined() throws Exception {
        BankAccount account = accountRepository.save(
                new BankAccount("Current Account", "SORTNUMBER", "ACCNUMBER", BigDecimal.valueOf(2000), LocalDate.now()));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fm19IntegrationUploadNoRule.csv",
                "text/csv",
                CSV_CONTENT.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/uploads/upload")
                        .file(file)
                        .param("bankAccount", String.valueOf(account.getId())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.successfulTransactions").value(1));

        Transaction persisted = transactionRepository.findAll().stream()
                .filter(t -> "BAR BRUNO".equals(t.getPaid_to()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Imported transaction was not persisted"));
        assertEquals("Undefined", persisted.getSegment());
    }
}
