package com.service;

import com.dto.BankAccount;
import com.dto.Transaction;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Real-database (H2) round trip: AccountServiceTest mocks the repository entirely, so it can't
// prove the Specification actually filters, combines account/date/segment with AND, or paginates
// without duplicates/omissions - only a real query round trip can.
//
// Rolls back each test's writes so state doesn't leak between tests, matching
// UploadControllerIntegrationTest's pattern.
@SpringBootTest
@Transactional
class AccountServiceFilteredTransactionsIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private BankAccount mainAccount;
    private BankAccount otherAccount;

    @BeforeEach
    void seedTransactions() {
        mainAccount = accountRepository.save(
                new BankAccount("Main Account", "SORTCODE50", "ACCNUMBER50", new BigDecimal(2000), LocalDate.now()));
        otherAccount = accountRepository.save(
                new BankAccount("Other Account", "SORTCODE51", "ACCNUMBER51", new BigDecimal(2000), LocalDate.now()));

        String[] segments = {
                "Groceries", "Groceries", "Bills", "Bills", "Undefined",
                "Undefined", "Groceries", "Bills", "Undefined", "Groceries"
        };
        for (int day = 1; day <= 10; day++) {
            Transaction transaction = new Transaction(
                    LocalDate.of(2024, 1, day), mainAccount, BigDecimal.TEN, null, "Payee " + day, "memo");
            transaction.setSegment(segments[day - 1]);
            transactionRepository.save(transaction);
        }

        // Shares the date window and "Groceries" segment with several main-account transactions,
        // so it would leak into every filter combination below if account-scoping ever broke.
        Transaction otherAccountTransaction = new Transaction(
                LocalDate.of(2024, 1, 5), otherAccount, BigDecimal.TEN, null, "Other Payee", "memo");
        otherAccountTransaction.setSegment("Groceries");
        transactionRepository.save(otherAccountTransaction);
    }

    // Page size is deliberately smaller than the expected result set, to force traversal across
    // multiple pages.
    private Set<Integer> collectAllIdsAcrossAllPages(LocalDate startDate, LocalDate endDate, String segment, long expectedTotalElements) {
        int pageSize = 3;
        Set<Integer> seenIds = new HashSet<>();
        int page = 0;
        Long totalElements = null;
        int totalPages = -1;

        while (true) {
            Page<Transaction> result = accountService.getPaginatedAccountTransactions(
                    mainAccount.getId(), page, pageSize, startDate, endDate, segment);

            if (totalElements == null) {
                totalElements = result.getTotalElements();
                totalPages = result.getTotalPages();
                assertEquals(expectedTotalElements, totalElements, "totalElements should equal the exact matching count");
                long expectedPages = expectedTotalElements == 0 ? 0 : (expectedTotalElements + pageSize - 1) / pageSize;
                assertEquals(expectedPages, totalPages, "totalPages should be consistent with totalElements/pageSize");
            } else {
                assertEquals(totalElements, result.getTotalElements(), "totalElements must stay consistent across pages");
                assertEquals(totalPages, result.getTotalPages(), "totalPages must stay consistent across pages");
            }

            for (Transaction transaction : result.getContent()) {
                assertFalse(seenIds.contains(transaction.getId()), "transaction id " + transaction.getId() + " was returned on more than one page");
                seenIds.add(transaction.getId());
                assertNotEqualsOtherAccount(transaction);
            }

            page++;
            if (page >= totalPages) {
                break;
            }
        }

        return seenIds;
    }

    private void assertNotEqualsOtherAccount(Transaction transaction) {
        assertFalse(transaction.getAccount().getId() == otherAccount.getId(),
                "the other account's transaction must never be returned, under any filter combination");
    }

    @Test
    void noFiltersReturnsAllTenMainAccountTransactionsExactlyOnceAcrossPages() {
        Set<Integer> ids = collectAllIdsAcrossAllPages(null, null, null, 10);
        assertEquals(10, ids.size());
    }

    @Test
    void dateOnlyFilterReturnsExactlyTheTransactionsInTheInclusiveRange() {
        Set<Integer> ids = collectAllIdsAcrossAllPages(LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 8), null, 6);

        for (Transaction transaction : transactionRepository.findAllById(ids)) {
            assertFalse(transaction.getDate().isBefore(LocalDate.of(2024, 1, 3)));
            assertFalse(transaction.getDate().isAfter(LocalDate.of(2024, 1, 8)));
        }
    }

    @Test
    void segmentOnlyFilterReturnsExactlyTheMatchingSegmentTransactions() {
        Set<Integer> ids = collectAllIdsAcrossAllPages(null, null, "Groceries", 4);

        for (Transaction transaction : transactionRepository.findAllById(ids)) {
            assertEquals("Groceries", transaction.getSegment());
        }
    }

    @Test
    void dateAndSegmentFiltersCombineWithAnd() {
        Set<Integer> ids = collectAllIdsAcrossAllPages(LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 8), "Groceries", 1);

        Transaction onlyMatch = transactionRepository.findById(ids.iterator().next()).orElseThrow();
        assertEquals(LocalDate.of(2024, 1, 7), onlyMatch.getDate());
        assertEquals("Groceries", onlyMatch.getSegment());
    }

    @Test
    void combinationWithZeroMatchesReturnsEmptyPageNotAnError() {
        Page<Transaction> result = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 0, 10, LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 4), "Groceries");

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void segmentFilterIsCaseSensitiveAndDoesNotMatchDifferentCasing() {
        Page<Transaction> result = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 0, 10, null, null, "groceries");

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    // "Undefined" is a literal string match, not a special no-filter case like null/blank.
    @Test
    void segmentEqualsUndefinedMatchesLiterally() {
        Set<Integer> ids = collectAllIdsAcrossAllPages(null, null, "Undefined", 3);

        for (Transaction transaction : transactionRepository.findAllById(ids)) {
            assertEquals("Undefined", transaction.getSegment());
        }
    }

    @Test
    void blankSegmentMeansNoFilterNotALiteralEmptyMatch() {
        Page<Transaction> result = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 0, 10, null, null, "   ");

        assertEquals(10, result.getTotalElements());
    }

    @Test
    void nonMatchingSegmentValueIsAValidEmptyResultNotAnError() {
        Page<Transaction> result = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 0, 10, null, null, "NonExistentSegment");

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void resultsAreOrderedDeterministicallyByDateDescendingThenIdDescending() {
        Page<Transaction> firstPage = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 0, 10, null, null, null);

        LocalDate previousDate = null;
        for (Transaction transaction : firstPage.getContent()) {
            if (previousDate != null) {
                assertFalse(transaction.getDate().isAfter(previousDate),
                        "results must be sorted by date descending");
            }
            previousDate = transaction.getDate();
        }
        assertEquals(LocalDate.of(2024, 1, 10), firstPage.getContent().get(0).getDate());
    }

    // Two same-dated transactions are forced onto adjacent single-row pages: without the id-desc
    // tiebreaker, their relative order across pages would be unspecified, risking a duplicate or
    // omitted row.
    @Test
    void tiedDateTransactionsAreSplitAcrossAPageBoundaryWithoutDuplicationOrOmission() {
        Transaction tiedA = new Transaction(
                LocalDate.of(2024, 1, 11), mainAccount, BigDecimal.TEN, null, "Tied Payee A", "memo");
        tiedA.setSegment("Groceries");
        tiedA = transactionRepository.save(tiedA);

        Transaction tiedB = new Transaction(
                LocalDate.of(2024, 1, 11), mainAccount, BigDecimal.TEN, null, "Tied Payee B", "memo");
        tiedB.setSegment("Groceries");
        tiedB = transactionRepository.save(tiedB);

        int higherId = Math.max(tiedA.getId(), tiedB.getId());
        int lowerId = Math.min(tiedA.getId(), tiedB.getId());

        Page<Transaction> firstPage = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 0, 1, null, null, null);
        Page<Transaction> secondPage = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 1, 1, null, null, null);

        assertEquals(12, firstPage.getTotalElements());
        assertEquals(1, firstPage.getContent().size());
        assertEquals(1, secondPage.getContent().size());
        assertEquals(higherId, firstPage.getContent().get(0).getId(),
                "within a date tie, the higher id must sort first (id desc tiebreaker)");
        assertEquals(lowerId, secondPage.getContent().get(0).getId(),
                "the lower id of the tied pair must land on the very next page, not be skipped or repeated");

        Set<Integer> allIds = collectAllIdsAcrossAllPages(null, null, null, 12);
        assertTrue(allIds.contains(higherId));
        assertTrue(allIds.contains(lowerId));
    }
}
