package com.repository;

import com.dto.BankAccount;
import com.dto.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// FM-52: real-database (H2) coverage for TransactionRepository.findAllByAccount_IdAndDateBetweenWithPagination.
// AC-8 is the important case here - this repository method's countQuery is a completely separate,
// hand-written native SQL string from its row-fetching query (Spring Data does not derive one from
// the other for native queries), so a bug where the WHERE clause was added to only one of them
// would still return the right page of rows but silently wrong totalElements/totalPages. A
// Mockito-based test (AccountServiceTest) can never catch that, since it stubs the whole Page
// return value - only a real Pageable/Page round trip against a real query can.
@DataJpaTest
class TransactionDateRangePaginationRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private BankAccount newAccount(String sortCode, String accountNumber) {
        return accountRepository.save(
                new BankAccount("Account " + sortCode, sortCode, accountNumber, new BigDecimal(2000), LocalDate.now()));
    }

    private Transaction newTransaction(BankAccount account, LocalDate date) {
        return transactionRepository.save(
                new Transaction(date, account, BigDecimal.TEN, null, "Tesco", "Weekly shop"));
    }

    // AC-8: seed an account with transactions spanning multiple pages (5 total, page size 2 ->
    // 3 unfiltered pages), where only a subset (2 of the 5) falls inside the filtered date range.
    // totalElements/totalPages must reflect the filtered subset (2 elements, 1 page at size 2),
    // not the account's full unfiltered total (5 elements, 3 pages).
    @Test
    void pagesAndCountsReflectOnlyTheFilteredSubsetNotTheAccountsFullTotal() {
        BankAccount account = newAccount("SORTCODE20", "ACCNUMBER20");

        // Outside the filtered range (before it).
        newTransaction(account, LocalDate.of(2024, 1, 1));
        newTransaction(account, LocalDate.of(2024, 1, 5));
        // Inside the filtered range [2024-01-10, 2024-01-20].
        newTransaction(account, LocalDate.of(2024, 1, 10));
        newTransaction(account, LocalDate.of(2024, 1, 15));
        // Outside the filtered range (after it).
        newTransaction(account, LocalDate.of(2024, 1, 25));

        Page<Transaction> unfilteredFirstPage = transactionRepository.findAllByAccount_IdWithPagination(
                account.getId(), PageRequest.of(0, 2));
        assertEquals(5, unfilteredFirstPage.getTotalElements());
        assertEquals(3, unfilteredFirstPage.getTotalPages());

        Page<Transaction> filteredFirstPage = transactionRepository.findAllByAccount_IdAndDateBetweenWithPagination(
                account.getId(), LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 20), PageRequest.of(0, 2));

        assertEquals(2, filteredFirstPage.getTotalElements());
        assertEquals(1, filteredFirstPage.getTotalPages());
        assertEquals(2, filteredFirstPage.getContent().size());
        assertTrue(filteredFirstPage.getContent().stream()
                .allMatch(t -> !t.getDate().isBefore(LocalDate.of(2024, 1, 10))
                        && !t.getDate().isAfter(LocalDate.of(2024, 1, 20))));
    }

    // AC-7: the range is inclusive on both ends - transactions dated exactly startDate or
    // exactly endDate must be included, not just strictly-between dates.
    @Test
    void rangeIsInclusiveOfBothTheStartDateAndTheEndDate() {
        BankAccount account = newAccount("SORTCODE21", "ACCNUMBER21");
        Transaction onStartDate = newTransaction(account, LocalDate.of(2024, 2, 1));
        Transaction onEndDate = newTransaction(account, LocalDate.of(2024, 2, 10));
        newTransaction(account, LocalDate.of(2024, 2, 11)); // just outside the range

        Page<Transaction> result = transactionRepository.findAllByAccount_IdAndDateBetweenWithPagination(
                account.getId(), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 10), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(t -> t.getId() == onStartDate.getId()));
        assertTrue(result.getContent().stream().anyMatch(t -> t.getId() == onEndDate.getId()));
    }

    // AC-6: startDate == endDate (single-day range) returns exactly the transactions on that
    // exact date.
    @Test
    void singleDayRangeReturnsOnlyTransactionsOnThatExactDate() {
        BankAccount account = newAccount("SORTCODE22", "ACCNUMBER22");
        newTransaction(account, LocalDate.of(2024, 3, 4));
        newTransaction(account, LocalDate.of(2024, 3, 5));
        newTransaction(account, LocalDate.of(2024, 3, 6));

        Page<Transaction> result = transactionRepository.findAllByAccount_IdAndDateBetweenWithPagination(
                account.getId(), LocalDate.of(2024, 3, 5), LocalDate.of(2024, 3, 5), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(LocalDate.of(2024, 3, 5), result.getContent().get(0).getDate());
    }

    // AC-9: a valid range with zero matching transactions is not an error - just an empty page
    // with totalElements 0.
    @Test
    void validRangeWithNoMatchingTransactionsReturnsAnEmptyPageNotAnError() {
        BankAccount account = newAccount("SORTCODE23", "ACCNUMBER23");
        newTransaction(account, LocalDate.of(2024, 4, 1));

        Page<Transaction> result = transactionRepository.findAllByAccount_IdAndDateBetweenWithPagination(
                account.getId(), LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31), PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.getContent().isEmpty());
    }
}
