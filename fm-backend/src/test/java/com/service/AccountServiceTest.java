package com.service;

import com.dto.BankAccount;
import com.dto.Transaction;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    // ---- FM-52: getPaginatedAccountTransactions date-range filtering ----

    // AC-2: with neither date supplied, the original unfiltered/paginated repository method must
    // still be the one invoked (not the new date-range query), so today's behavior stays
    // byte-for-byte unchanged.
    @Test
    public void usesTheUnfilteredQueryWhenNeitherDateIsSupplied() {
        Page<Transaction> stubbedPage = new PageImpl<>(List.of());
        when(transactionRepository.findAllByAccount_IdWithPagination(eq(1), any(Pageable.class)))
                .thenReturn(stubbedPage);

        Page<Transaction> result = service.getPaginatedAccountTransactions(1, 0, 10, null, null);

        assertEquals(stubbedPage, result);
        verify(transactionRepository).findAllByAccount_IdWithPagination(1, PageRequest.of(0, 10));
        verify(transactionRepository, never())
                .findAllByAccount_IdAndDateBetweenWithPagination(anyInt(), any(), any(), any());
    }

    // AC-1/AC-7: with both dates supplied, the date-range query must be used, with both bounds
    // passed through unchanged (inclusive range, no swapping).
    @Test
    public void usesTheDateRangeQueryWhenBothDatesAreSupplied() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        Page<Transaction> stubbedPage = new PageImpl<>(List.of());
        when(transactionRepository.findAllByAccount_IdAndDateBetweenWithPagination(
                eq(1), eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(stubbedPage);

        Page<Transaction> result = service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate);

        assertEquals(stubbedPage, result);
        verify(transactionRepository)
                .findAllByAccount_IdAndDateBetweenWithPagination(1, startDate, endDate, PageRequest.of(0, 10));
        verify(transactionRepository, never()).findAllByAccount_IdWithPagination(anyInt(), any());
    }

    // AC-6: startDate == endDate is a valid single-day range, not an error.
    @Test
    public void allowsAndFiltersOnASingleDayRange() {
        LocalDate sameDay = LocalDate.now().minusDays(1);
        Page<Transaction> stubbedPage = new PageImpl<>(List.of());
        when(transactionRepository.findAllByAccount_IdAndDateBetweenWithPagination(
                eq(1), eq(sameDay), eq(sameDay), any(Pageable.class)))
                .thenReturn(stubbedPage);

        Page<Transaction> result = service.getPaginatedAccountTransactions(1, 0, 10, sameDay, sameDay);

        assertEquals(stubbedPage, result);
    }

    // AC-3: exactly one of the two params supplied must be rejected, not treated as an
    // open-ended filter, regardless of which one is missing.
    @Test
    public void rejectsWhenOnlyStartDateIsSupplied() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, LocalDate.now(), null));

        assertEquals("Both start date and end date are required", exception.getMessage());
    }

    @Test
    public void rejectsWhenOnlyEndDateIsSupplied() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, null, LocalDate.now()));

        assertEquals("Both start date and end date are required", exception.getMessage());
    }

    // AC-4: an inverted range is rejected outright, never silently swapped.
    @Test
    public void rejectsWhenStartDateIsAfterEndDate() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(1);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate));

        assertEquals("Start date cannot be after end date", exception.getMessage());
    }

    // AC-5: strictly-in-the-future dates are rejected...
    @Test
    public void rejectsWhenStartDateIsInTheFuture() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(2);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate));

        assertEquals("Date cannot be in the future", exception.getMessage());
    }

    @Test
    public void rejectsWhenEndDateIsInTheFuture() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(1);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate));

        assertEquals("Date cannot be in the future", exception.getMessage());
    }

    // ...but endDate == today is inclusive/valid, matching
    // TransactionService.addManualTransaction's isAfter(LocalDate.now()) pattern.
    @Test
    public void allowsEndDateEqualToToday() {
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now();
        Page<Transaction> stubbedPage = new PageImpl<>(List.of());
        when(transactionRepository.findAllByAccount_IdAndDateBetweenWithPagination(
                eq(1), eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(stubbedPage);

        Page<Transaction> result = service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate);

        assertEquals(stubbedPage, result);
    }

}