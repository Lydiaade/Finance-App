package com.service;

import com.dto.Account;
import com.dto.MonthlyTransactionTotal;
import com.dto.Transaction;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    private AccountService service;
    private FinanceManagerService financeManagerService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AccountService(accountRepository, transactionRepository, financeManagerService);
    }

    @Test
    public void getAccountWhenItExists() throws FileNotFoundException {
        LocalDate currentBalanceDate = LocalDate.now();
        Account account = new Account("Account Name", "SORT NUMBER", "ACCOUNT NUMBER", new BigDecimal(1000), currentBalanceDate);
        Integer id = 1;
        // arrange
        when(accountRepository.findById(id)).thenReturn(java.util.Optional.of(account));

        // act
        Account actualResult = service.getAccount(id);

        // assert
        assertEquals(account, actualResult);
    }

    @Test
    public void failsToGetAccountWhenItDoesNotExists() {
        Integer id = 1;
        // arrange
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        // act
        Exception exception = assertThrows(FileNotFoundException.class, () -> service.getAccount(id));

        String expectedMessage = "This account does not exist";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

}