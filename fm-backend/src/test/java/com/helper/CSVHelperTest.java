package com.helper;

import com.dto.BankAccount;
import com.dto.FileUpload;
import com.dto.PayeeSegmentRule;
import com.dto.Transaction;
import com.repository.PayeeSegmentRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CSVHelperTest {

    @InjectMocks
    private CSVHelper csvHelper;

    @Mock
    private PayeeSegmentRuleRepository payeeSegmentRuleRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFileRetrieval() {
        String path = "src/test/resources/textData.csv";

        File file = new File(path);
        String absolutePath = file.getAbsolutePath();

        System.out.println(absolutePath);

        assertTrue(absolutePath.contains("src/test/resources"));
    }

    private BankAccount testAccount() {
        return new BankAccount("Current Account", "SORTNUMBER", "ACCNUMBER", new BigDecimal(2000), LocalDate.now());
    }

    // FM-19 AC-9 - a transaction whose paid_to matches an existing rule gets the rule's segment
    // on import, not the entity's "Undefined" default.
    @Test
    public void importedTransactionUsesMatchingPayeeRuleSegment() {
        BankAccount account = testAccount();
        when(payeeSegmentRuleRepository.findByPaidTo("BAR BRUNO"))
                .thenReturn(Optional.of(new PayeeSegmentRule("BAR BRUNO", "Eating Out")));

        File file = new File("src/test/resources/testData.csv");
        FileUpload fileUpload = new FileUpload("testData.csv", account);

        FileUpload result = csvHelper.transformFileToTransactions(file, fileUpload, account);

        Transaction matched = result.getTransactions().stream()
                .filter(t -> "BAR BRUNO".equals(t.getPaid_to()))
                .findFirst()
                .orElseThrow();
        assertEquals("Eating Out", matched.getSegment());
    }

    // FM-19 AC-9 regression - a transaction with no matching rule still defaults to "Undefined",
    // exactly as before this ticket's changes.
    @Test
    public void importedTransactionWithNoMatchingRuleStillDefaultsToUndefined() {
        BankAccount account = testAccount();
        when(payeeSegmentRuleRepository.findByPaidTo("BAR BRUNO")).thenReturn(Optional.empty());

        File file = new File("src/test/resources/testData.csv");
        FileUpload fileUpload = new FileUpload("testData.csv", account);

        FileUpload result = csvHelper.transformFileToTransactions(file, fileUpload, account);

        Transaction unmatched = result.getTransactions().stream()
                .filter(t -> "BAR BRUNO".equals(t.getPaid_to()))
                .findFirst()
                .orElseThrow();
        assertEquals("Undefined", unmatched.getSegment());
    }
}
