package com.service;

import com.dto.BankAccount;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// Note: this test previously called `new AccountService(accountRepository, transactionRepository,
// financeManagerService)`, but AccountService only has a no-arg constructor (it uses field
// injection) - that call never compiled. Pre-existing breakage found on this branch while working
// FM-23, unrelated to FM-23 itself; fixed here (switched to @InjectMocks, the idiomatic Mockito
// equivalent of the field-injection wiring AccountService already uses) only so the module's test
// suite compiles and can actually be run.
@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @InjectMocks
    private AccountService service;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FinanceManagerService financeManagerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getAccountWhenItExists() throws FileNotFoundException {
        LocalDate currentBalanceDate = LocalDate.now();
        BankAccount account = new BankAccount("Account Name", "SORT NUMBER", "ACCOUNT NUMBER", new BigDecimal(1000), currentBalanceDate);
        Integer id = 1;
        // arrange
        when(accountRepository.findById(id)).thenReturn(java.util.Optional.of(account));

        // act
        BankAccount actualResult = service.getAccount(id);

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