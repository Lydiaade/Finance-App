package com.service;

import com.dto.BankAccount;
import com.dto.FileUpload;
import com.dto.Transaction;
import com.repository.AccountRepository;
import com.repository.FileUploadRepository;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Baseline (pre-fix) behavior for this ticket, established before any of the fix below was
// written: AccountService.deleteAccount deleted the account's transactions directly, but never
// touched FileUpload rows. Since FileUpload.bankAccount is a real FK (account_id) with no
// cascade, attempting to then delete the BankAccount row while a FileUpload row still pointed at
// it threw a DataIntegrityViolationException (FK1QBS8QJWMOLVT9UE8LLVO4UHV on file_uploads.account_id)
// rather than succeeding or silently leaving a dangling reference. AccountController.deleteAccount
// caught this as a generic Exception and returned 404, misreporting an FK failure as "account not
// found". The tests below cover the fixed behavior.
@SpringBootTest
@Transactional
class AccountServiceDeleteAccountIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FileUploadRepository fileUploadRepository;

    private BankAccount account;

    @BeforeEach
    void seedAccount() {
        account = accountRepository.save(
                new BankAccount("Account With Uploads", "SORTCODE60", "ACCNUMBER60", new BigDecimal(1000), LocalDate.now()));
    }

    private Transaction saveTransaction(String payee, FileUpload fileUpload) {
        Transaction transaction = new Transaction(
                LocalDate.now(), account, BigDecimal.TEN, "Debit", payee, "memo");
        transaction.setFileUpload(fileUpload);
        return transactionRepository.save(transaction);
    }

    @Test
    void deletingAccountWithAFileUploadAndItsTransactionsSucceedsAndRemovesEverything() {
        FileUpload fileUpload = fileUploadRepository.save(new FileUpload("statement.csv", account));
        Transaction transaction = saveTransaction("Some Payee", fileUpload);

        assertDoesNotThrow(() -> accountService.deleteAccount(account.getId()));

        assertTrue(accountRepository.findById(account.getId()).isEmpty());
        assertTrue(fileUploadRepository.findAllByBankAccount_Id(account.getId()).isEmpty());
        assertTrue(transactionRepository.findAllByAccount_Id(account.getId()).isEmpty());
        assertTrue(transactionRepository.findById(transaction.getId()).isEmpty());
    }

    @Test
    void deletingAccountWithOnlyManuallyAddedTransactionsAndNoFileUploadsSucceeds() {
        Transaction manualTransaction = saveTransaction("Manual Payee", null);

        assertDoesNotThrow(() -> accountService.deleteAccount(account.getId()));

        assertTrue(accountRepository.findById(account.getId()).isEmpty());
        assertTrue(fileUploadRepository.findAllByBankAccount_Id(account.getId()).isEmpty());
        assertTrue(transactionRepository.findById(manualTransaction.getId()).isEmpty());
    }

    @Test
    void deletingAccountWithZeroUploadsAndZeroTransactionsSucceedsCleanly() {
        assertDoesNotThrow(() -> accountService.deleteAccount(account.getId()));

        assertTrue(accountRepository.findById(account.getId()).isEmpty());
    }

    @Test
    void deletingAccountWithMultipleFileUploadsMultipleTransactionsAndManualTransactionsRemovesAllOfThem() {
        FileUpload firstUpload = fileUploadRepository.save(new FileUpload("statement1.csv", account));
        FileUpload secondUpload = fileUploadRepository.save(new FileUpload("statement2.csv", account));

        Transaction firstUploadTransactionA = saveTransaction("Payee A", firstUpload);
        Transaction firstUploadTransactionB = saveTransaction("Payee B", firstUpload);
        Transaction secondUploadTransactionA = saveTransaction("Payee C", secondUpload);
        Transaction secondUploadTransactionB = saveTransaction("Payee D", secondUpload);
        Transaction manualTransaction = saveTransaction("Manual Payee", null);

        assertDoesNotThrow(() -> accountService.deleteAccount(account.getId()));

        assertTrue(accountRepository.findById(account.getId()).isEmpty());
        assertTrue(fileUploadRepository.findAllByBankAccount_Id(account.getId()).isEmpty());
        assertTrue(transactionRepository.findAllById(List.of(
                firstUploadTransactionA.getId(), firstUploadTransactionB.getId(),
                secondUploadTransactionA.getId(), secondUploadTransactionB.getId(),
                manualTransaction.getId())).isEmpty());
    }

    // Preserves today's contract for a non-existent account id: accountRepository.deleteById still
    // throws EmptyResultDataAccessException, unaffected by the new upload-cleanup step (which finds
    // zero FileUpload rows and does nothing) running ahead of it.
    @Test
    void deletingNonExistentAccountStillThrowsEmptyResultDataAccessExceptionNotAnUnhandledError() {
        int nonExistentId = account.getId() + 1000;

        assertThrows(EmptyResultDataAccessException.class, () -> accountService.deleteAccount(nonExistentId));
    }
}
