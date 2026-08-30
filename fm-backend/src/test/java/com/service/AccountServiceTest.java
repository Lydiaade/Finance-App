package com.service;

import com.dto.BankAccount;
import com.dto.Transaction;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    // ---- updateAccountBalance ----

    @Test
    public void updateAccountBalanceSavesOnlyBalanceAndDateAndReturnsUpdatedAccount() throws FileNotFoundException {
        BankAccount account = new BankAccount("Account Name", "SORT NUMBER", "ACCOUNT NUMBER", new BigDecimal("1000.00"), LocalDate.of(2024, 1, 1));
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        BigDecimal newBalance = new BigDecimal("123.45");
        LocalDate newDate = LocalDate.of(2024, 6, 1);

        BankAccount result = service.updateAccountBalance(1, newBalance, newDate);

        assertEquals(newBalance, result.getCurrentBalance());
        assertEquals(newDate, result.getCurrentBalanceDate());
        assertEquals("Account Name", result.getName());
        assertEquals("SORT NUMBER", result.getSortCode());
        assertEquals("ACCOUNT NUMBER", result.getAccountNumber());
    }

    @Test
    public void updateAccountBalanceLeavesUnrelatedFieldsUntouched() throws FileNotFoundException {
        BankAccount account = new BankAccount("Original Name", "111111", "22222222", "DEBIT", "BARCLAYS", "GBP", new BigDecimal("500.00"), "2024-01-01");
        account.setMainBankAccount(true);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        BankAccount result = service.updateAccountBalance(1, new BigDecimal("999.99"), LocalDate.of(2024, 12, 25));

        assertEquals("Original Name", result.getName());
        assertEquals("111111", result.getSortCode());
        assertEquals("22222222", result.getAccountNumber());
        assertEquals(com.dto.BankAccountType.DEBIT, result.getAccountType());
        assertEquals(com.dto.BankName.BARCLAYS, result.getBankName());
        assertEquals(java.util.Currency.getInstance("GBP"), result.getCurrency());
        assertTrue(result.isMainBankAccount());
    }

    @Test
    public void updateAccountBalanceForNonExistentAccountThrowsFileNotFoundException() {
        when(accountRepository.findById(999)).thenReturn(Optional.empty());

        Exception exception = assertThrows(FileNotFoundException.class,
                () -> service.updateAccountBalance(999, BigDecimal.TEN, LocalDate.now()));

        assertTrue(exception.getMessage().contains("This account does not exist"));
        org.mockito.Mockito.verify(accountRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    public void updateAccountBalanceRejectsNullBalance() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateAccountBalance(1, null, LocalDate.now()));

        assertEquals("Both currentBalance and currentBalanceDate are required", exception.getMessage());
        org.mockito.Mockito.verifyNoInteractions(accountRepository);
    }

    @Test
    public void updateAccountBalanceRejectsNullDate() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateAccountBalance(1, BigDecimal.TEN, null));

        assertEquals("Both currentBalance and currentBalanceDate are required", exception.getMessage());
        org.mockito.Mockito.verifyNoInteractions(accountRepository);
    }

    @Test
    public void updateAccountBalanceAllowsANegativeBalance() throws FileNotFoundException {
        BankAccount account = new BankAccount("Account Name", "SORT NUMBER", "ACCOUNT NUMBER", new BigDecimal("100.00"), LocalDate.now());
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        BigDecimal overdrawnBalance = new BigDecimal("-250.75");

        BankAccount result = service.updateAccountBalance(1, overdrawnBalance, LocalDate.now());

        assertEquals(overdrawnBalance, result.getCurrentBalance());
    }

    @Test
    public void updateAccountBalanceAllowsAFutureBalanceDate() throws FileNotFoundException {
        BankAccount account = new BankAccount("Account Name", "SORT NUMBER", "ACCOUNT NUMBER", new BigDecimal("100.00"), LocalDate.now());
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        LocalDate futureDate = LocalDate.now().plusYears(1);

        BankAccount result = service.updateAccountBalance(1, BigDecimal.TEN, futureDate);

        assertEquals(futureDate, result.getCurrentBalanceDate());
    }

    @Test
    public void updateAccountBalanceSucceedsWhenValuesAreIdenticalToExisting() throws FileNotFoundException {
        BigDecimal existingBalance = new BigDecimal("100.00");
        LocalDate existingDate = LocalDate.of(2024, 3, 1);
        BankAccount account = new BankAccount("Account Name", "SORT NUMBER", "ACCOUNT NUMBER", existingBalance, existingDate);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        BankAccount result = service.updateAccountBalance(1, existingBalance, existingDate);

        assertEquals(existingBalance, result.getCurrentBalance());
        assertEquals(existingDate, result.getCurrentBalanceDate());
    }

    @Test
    public void updateAccountBalancePreservesDecimalPrecisionExactly() throws FileNotFoundException {
        BankAccount account = new BankAccount("Account Name", "SORT NUMBER", "ACCOUNT NUMBER", new BigDecimal("1.00"), LocalDate.now());
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        BigDecimal preciseBalance = new BigDecimal("123.45");

        BankAccount result = service.updateAccountBalance(1, preciseBalance, LocalDate.now());

        assertEquals("123.45", result.getCurrentBalance().toPlainString());
    }

    @Test
    public void updateAccountBalanceHandlesVeryLargeAndVerySmallValuesWithoutLoss() throws FileNotFoundException {
        BankAccount account = new BankAccount("Account Name", "SORT NUMBER", "ACCOUNT NUMBER", BigDecimal.ZERO, LocalDate.now());
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        BigDecimal verySmall = new BigDecimal("0.01");
        BankAccount smallResult = service.updateAccountBalance(1, verySmall, LocalDate.now());
        assertEquals("0.01", smallResult.getCurrentBalance().toPlainString());

        BigDecimal veryLarge = new BigDecimal("999999999999.99");
        BankAccount largeResult = service.updateAccountBalance(1, veryLarge, LocalDate.now());
        assertEquals("999999999999.99", largeResult.getCurrentBalance().toPlainString());
    }

    // ---- FM-53: getPaginatedAccountTransactions ----
    //
    // FM-53 replaced the two hand-written native pagination queries (each with its own separate
    // repository method) with a single transactionRepository.findAll(Specification, Pageable)
    // call, so there's no longer a distinct method per filter combination to verify via
    // Mockito's `verify(...)`. What a Mockito-level unit test *can* still prove is: (a) the
    // service's date-range validation is unaffected by FM-53 (AC-9, still exercised below with the
    // new method signature per AC-28), and (b) the Pageable passed to the repository carries the
    // deterministic Sort required by AC-4. Proving the Specification itself actually filters
    // correctly (account scope, date range, segment, AND-combination, pagination correctness
    // across pages) needs a real database/query round trip - that's covered by
    // AccountServiceFilteredTransactionsIntegrationTest, not here.

    @Test
    public void callsRepositoryFindAllWithSpecificationAndDeterministicSort() {
        Page<Transaction> stubbedPage = new PageImpl<>(List.of());
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(stubbedPage);

        Page<Transaction> result = service.getPaginatedAccountTransactions(1, 0, 10, null, null, null);

        assertEquals(stubbedPage, result);

        ArgumentCaptor<Specification> specCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(transactionRepository).findAll(specCaptor.capture(), pageableCaptor.capture());

        assertNotNull(specCaptor.getValue());
        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(10, capturedPageable.getPageSize());
        // AC-4: explicit deterministic sort - date descending, then id as a tiebreaker - not the
        // undefined/incidental order the old native queries left row order to.
        assertEquals(Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id")), capturedPageable.getSort());
    }

    @Test
    public void passesSegmentAndDateRangeThroughToTheSpecificationBuildRegardlessOfCombination() {
        Page<Transaction> stubbedPage = new PageImpl<>(List.of());
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(stubbedPage);

        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        Page<Transaction> result = service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate, "Groceries");

        assertEquals(stubbedPage, result);
        org.mockito.Mockito.verify(transactionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // AC-6: startDate == endDate is a valid single-day range, not an error.
    @Test
    public void allowsAndFiltersOnASingleDayRange() {
        LocalDate sameDay = LocalDate.now().minusDays(1);
        Page<Transaction> stubbedPage = new PageImpl<>(List.of());
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(stubbedPage);

        Page<Transaction> result = service.getPaginatedAccountTransactions(1, 0, 10, sameDay, sameDay, null);

        assertEquals(stubbedPage, result);
    }

    // AC-3/AC-9: exactly one of the two date params supplied must be rejected, not treated as an
    // open-ended filter, regardless of which one is missing - and this is unaffected by whether a
    // segment filter is also present.
    @Test
    public void rejectsWhenOnlyStartDateIsSupplied() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, LocalDate.now(), null, null));

        assertEquals("Both start date and end date are required", exception.getMessage());
    }

    @Test
    public void rejectsWhenOnlyEndDateIsSupplied() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, null, LocalDate.now(), null));

        assertEquals("Both start date and end date are required", exception.getMessage());
    }

    // AC-9: a lone date param is still rejected even when a segment filter is also present -
    // segment presence must not bypass the existing date validation.
    @Test
    public void rejectsWhenOnlyStartDateIsSuppliedEvenWithASegmentFilterPresent() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, LocalDate.now(), null, "Groceries"));

        assertEquals("Both start date and end date are required", exception.getMessage());
    }

    // AC-4: an inverted range is rejected outright, never silently swapped.
    @Test
    public void rejectsWhenStartDateIsAfterEndDate() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(1);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate, null));

        assertEquals("Start date cannot be after end date", exception.getMessage());
    }

    // AC-5: strictly-in-the-future dates are rejected...
    @Test
    public void rejectsWhenStartDateIsInTheFuture() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(2);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate, null));

        assertEquals("Date cannot be in the future", exception.getMessage());
    }

    @Test
    public void rejectsWhenEndDateIsInTheFuture() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(1);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate, null));

        assertEquals("Date cannot be in the future", exception.getMessage());
    }

    // ...but endDate == today is inclusive/valid, matching
    // TransactionService.addManualTransaction's isAfter(LocalDate.now()) pattern.
    @Test
    public void allowsEndDateEqualToToday() {
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now();
        Page<Transaction> stubbedPage = new PageImpl<>(List.of());
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(stubbedPage);

        Page<Transaction> result = service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate, null);

        assertEquals(stubbedPage, result);
    }

}
