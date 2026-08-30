package com.service;

import com.dto.BankAccount;
import com.dto.Transaction;
import com.repository.AccountRepository;
import com.repository.FileUploadRepository;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

// Deliberately not @Transactional at the class level: that would wrap the whole test method in
// one already-open DB transaction, making every repository call commit or roll back together
// regardless of whether AccountService.deleteAccount itself is @Transactional - which would hide
// the exact gap this test exists to prove. Running without it means each repository call here
// commits independently, same as production, so only deleteAccount's own @Transactional boundary
// determines whether its internal deletes survive a failure partway through.
@SpringBootTest
class AccountServiceDeleteAccountTransactionalRollbackTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockBean
    private FileUploadRepository fileUploadRepository;

    private BankAccount account;

    @BeforeEach
    void seedAccount() {
        account = accountRepository.save(
                new BankAccount("Account For Rollback Test", "SORTCODE70", "ACCNUMBER70", new BigDecimal(1000), LocalDate.now()));
    }

    @AfterEach
    void cleanUp() {
        transactionRepository.findAllByAccount_Id(account.getId())
                .forEach(transaction -> transactionRepository.deleteById(transaction.getId()));
        accountRepository.findById(account.getId()).ifPresent(accountRepository::delete);
    }

    @Test
    void aFailureAfterTransactionsAreDeletedRollsBackThoseDeletesTooInsteadOfLeavingAGhostAccount() {
        Transaction transaction = transactionRepository.save(
                new Transaction(LocalDate.now(), account, BigDecimal.TEN, "Debit", "Some Payee", "memo"));

        when(fileUploadRepository.findAllByBankAccount_Id(anyInt()))
                .thenThrow(new RuntimeException("simulated failure between the transaction-delete step and the file-upload-delete step"));

        assertThrows(RuntimeException.class, () -> accountService.deleteAccount(account.getId()));

        assertTrue(transactionRepository.findById(transaction.getId()).isPresent());
        assertTrue(accountRepository.findById(account.getId()).isPresent());
    }
}
