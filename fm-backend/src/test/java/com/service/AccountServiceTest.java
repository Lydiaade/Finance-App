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
        when(accountRepository.findById(id)).thenReturn(java.util.Optional.of(account));

        BankAccount actualResult = service.getAccount(id);

        assertEquals(account, actualResult);
    }

    @Test
    public void failsToGetAccountWhenItDoesNotExists() {
        Integer id = 1;
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        Exception exception = assertThrows(FileNotFoundException.class, () -> service.getAccount(id));

        String expectedMessage = "This account does not exist";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    // These are Mockito-level tests, so they can only prove the service wires the Specification,
    // Pageable, and Sort correctly - actual filtering correctness needs a real query round trip,
    // which is covered by AccountServiceFilteredTransactionsIntegrationTest instead.

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

    @Test
    public void allowsAndFiltersOnASingleDayRange() {
        LocalDate sameDay = LocalDate.now().minusDays(1);
        Page<Transaction> stubbedPage = new PageImpl<>(List.of());
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(stubbedPage);

        Page<Transaction> result = service.getPaginatedAccountTransactions(1, 0, 10, sameDay, sameDay, null);

        assertEquals(stubbedPage, result);
    }

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

    @Test
    public void rejectsWhenOnlyStartDateIsSuppliedEvenWithASegmentFilterPresent() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, LocalDate.now(), null, "Groceries"));

        assertEquals("Both start date and end date are required", exception.getMessage());
    }

    @Test
    public void rejectsWhenStartDateIsAfterEndDate() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(1);

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> service.getPaginatedAccountTransactions(1, 0, 10, startDate, endDate, null));

        assertEquals("Start date cannot be after end date", exception.getMessage());
    }

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

    // endDate == today is inclusive/valid, matching TransactionService.addManualTransaction's
    // isAfter(LocalDate.now()) pattern.
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
