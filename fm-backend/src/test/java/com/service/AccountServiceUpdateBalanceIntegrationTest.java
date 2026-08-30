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

    @Test
    void updatingBalanceForNonExistentAccountDoesNotCreateANewRow() {
        long accountCountBefore = accountRepository.count();
        int nonExistentId = account.getId() + 1000;

        org.junit.jupiter.api.Assertions.assertThrows(FileNotFoundException.class,
                () -> accountService.updateAccountBalance(nonExistentId, BigDecimal.TEN, LocalDate.now()));

        assertEquals(accountCountBefore, accountRepository.count());
    }
}
