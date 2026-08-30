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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // Baseline (pre-fix) behavior: AccountService.deleteAccount only deletes the account's
    // transactions directly, never touches FileUpload rows. FileUpload.bankAccount is a real FK
    // (account_id) with no cascade, so once the account's transactions are gone, deleting the
    // BankAccount row itself violates that FK because a FileUpload row still points at it -
    // deleteAccount throws (confirmed here via a real FK constraint violation on flush) rather
    // than succeeding or silently leaving a dangling reference. Note: AccountController.deleteAccount
    // catches this as a generic Exception and returns 404, misreporting an FK failure as "account
    // not found" - flagged separately, not changed here.
    @Test
    void deletingAccountWithAFileUploadThrowsDueToForeignKeyConstraintBeforeFix() {
        FileUpload fileUpload = fileUploadRepository.save(new FileUpload("statement.csv", account));
        Transaction transaction = new Transaction(
                LocalDate.now(), account, BigDecimal.TEN, "Debit", "Some Payee", "memo");
        transaction.setFileUpload(fileUpload);
        transactionRepository.save(transaction);

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class, () -> {
            accountService.deleteAccount(account.getId());
            accountRepository.flush();
        });

        assertTrue(exception.getMostSpecificCause().getMessage().contains("FILE_UPLOADS"),
                "the violation must come from the FileUpload -> BankAccount FK, confirming FileUpload rows are the blocker");
    }
}
