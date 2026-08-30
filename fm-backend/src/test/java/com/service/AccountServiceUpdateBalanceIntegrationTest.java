package com.service;

import com.dto.BankAccount;
import com.dto.FileUpload;
import com.dto.Transaction;
import com.repository.AccountRepository;
import com.repository.FileUploadRepository;
import com.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class AccountServiceUpdateBalanceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FileUploadRepository fileUploadRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private BankAccount account;

    @BeforeEach
    void seedAccount() {
        account = accountRepository.save(
                new BankAccount("Account With History", "SORTCODE70", "ACCNUMBER70", new BigDecimal("1000.00"), LocalDate.of(2024, 1, 1)));
    }

    @Test
    void updatingBalanceLeavesExistingTransactionsAndFileUploadsUnchanged() throws FileNotFoundException {
        FileUpload fileUpload = fileUploadRepository.save(new FileUpload("statement.csv", account));
        Transaction transaction = new Transaction(LocalDate.of(2024, 1, 15), account, new BigDecimal("-25.00"), "Debit", "Some Payee", "memo");
        transaction.setFileUpload(fileUpload);
        transaction = transactionRepository.save(transaction);

        accountService.updateAccountBalance(account.getId(), new BigDecimal("2000.00"), LocalDate.of(2024, 7, 1));

        Transaction reloadedTransaction = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertEquals(new BigDecimal("-25.00"), reloadedTransaction.getAmount());
        assertEquals(LocalDate.of(2024, 1, 15), reloadedTransaction.getDate());
        assertEquals("Some Payee", reloadedTransaction.getPaid_to());
        assertEquals("memo", reloadedTransaction.getMemo());

        FileUpload reloadedFileUpload = fileUploadRepository.findById(fileUpload.getId()).orElseThrow();
        assertEquals("statement.csv", reloadedFileUpload.getFileName());

        assertEquals(1, transactionRepository.findAllByAccount_Id(account.getId()).size());
        assertEquals(1, fileUploadRepository.findAllByBankAccount_Id(account.getId()).size());
    }

    // The service-level unit tests (AccountServiceTest) mock accountRepository.save() to return
    // the same in-memory object, so they can't actually prove the value survives a real database
    // round trip. This flushes and clears the persistence context so the reload below is a genuine
    // SELECT against the H2 database, not a return of the cached managed entity - the only way to
    // prove the `numeric(38,2)` column genuinely preserves a boundary-large and boundary-small
    // value rather than silently rounding/truncating on write.
    @Test
    void updatingBalancePersistsExactPrecisionThroughARealDatabaseRoundTrip() throws FileNotFoundException {
        BigDecimal veryLargeBalance = new BigDecimal("999999999999.99");
        accountService.updateAccountBalance(account.getId(), veryLargeBalance, LocalDate.of(2024, 7, 1));
        entityManager.flush();
        entityManager.clear();

        BankAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals("999999999999.99", reloaded.getCurrentBalance().toPlainString());
        assertEquals(LocalDate.of(2024, 7, 1), reloaded.getCurrentBalanceDate());

        BigDecimal verySmallBalance = new BigDecimal("0.01");
        accountService.updateAccountBalance(account.getId(), verySmallBalance, LocalDate.of(2024, 8, 1));
        entityManager.flush();
        entityManager.clear();

        BankAccount reloadedAgain = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals("0.01", reloadedAgain.getCurrentBalance().toPlainString());
    }

    @Test
    void updatingBalanceForNonExistentAccountDoesNotCreateANewRow() {
        long accountCountBefore = accountRepository.count();
        int nonExistentId = account.getId() + 1000;

        org.junit.jupiter.api.Assertions.assertThrows(FileNotFoundException.class,
                () -> accountService.updateAccountBalance(nonExistentId, BigDecimal.TEN, LocalDate.now()));

        assertEquals(accountCountBefore, accountRepository.count());
    }
}
