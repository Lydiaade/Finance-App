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

// FM-53: real-database (H2) coverage for AccountService.getPaginatedAccountTransactions's
// Specification-based rewrite. A Mockito-based test (AccountServiceTest) stubs the repository call
// entirely, so it can never prove the Specification actually filters correctly, that
// account-scoping and the date/segment filters combine with AND, or that pagination is free of
// duplicates/omissions across pages under the new explicit Sort (AC-4) - only a real
// Pageable/Specification round trip against a real query can, which is what this class does.
//
// @Transactional at the class level rolls back each test's writes so tests don't leak state into
// each other via the shared H2 context, matching UploadControllerIntegrationTest's pattern.
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

    // AC-10: a deterministic, multi-segment, multi-date transaction set on one account, plus at
    // least one transaction on a *different* account that would otherwise match every filter
    // combination below (same date-range window, same "Groceries" segment) - this is the
    // regression proof that account-scoping isn't accidentally dropped by the Specification
    // rewrite.
    //
    // Main account (10 transactions, 2024-01-01 .. 2024-01-10, one per day):
    //   01: Groceries   02: Groceries   03: Bills   04: Bills   05: Undefined
    //   06: Undefined   07: Groceries   08: Bills   09: Undefined  10: Groceries
    //
    // Date range used below, [2024-01-03, 2024-01-08] inclusive, covers ids for 03-08:
    //   Bills, Bills, Undefined, Undefined, Groceries, Bills -> 6 transactions, of which exactly
    //   one (01-07) is segment "Groceries".
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

        // Same date window, same "Groceries" segment as several main-account transactions - if
        // account scoping were ever dropped from the Specification, this transaction would leak
        // into every combination tested below.
        Transaction otherAccountTransaction = new Transaction(
                LocalDate.of(2024, 1, 5), otherAccount, BigDecimal.TEN, null, "Other Payee", "memo");
        otherAccountTransaction.setSegment("Groceries");
        transactionRepository.save(otherAccountTransaction);
    }

    // Drains every page (small page size, deliberately smaller than the expected result set) and
    // returns the full set of transaction ids returned across all pages, while asserting
    // totalElements/totalPages consistency and that no id is returned twice.
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

    // AC-2/AC-10: no filters - every one of the main account's 10 transactions is returned exactly
    // once across pages, and the other account's transaction is never returned.
    @Test
    void noFiltersReturnsAllTenMainAccountTransactionsExactlyOnceAcrossPages() {
        Set<Integer> ids = collectAllIdsAcrossAllPages(null, null, null, 10);
        assertEquals(10, ids.size());
    }

    // AC-3/AC-6/AC-7/AC-10: date-only - inclusive range [01-03, 01-08] matches exactly the 6
    // transactions dated 03 through 08.
    @Test
    void dateOnlyFilterReturnsExactlyTheTransactionsInTheInclusiveRange() {
        Set<Integer> ids = collectAllIdsAcrossAllPages(LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 8), null, 6);

        for (Transaction transaction : transactionRepository.findAllById(ids)) {
            assertFalse(transaction.getDate().isBefore(LocalDate.of(2024, 1, 3)));
            assertFalse(transaction.getDate().isAfter(LocalDate.of(2024, 1, 8)));
        }
    }

    // AC-5/AC-6/AC-10: segment-only, exact match on "Groceries" - matches days 01, 02, 07, 10 (4
    // transactions), regardless of date.
    @Test
    void segmentOnlyFilterReturnsExactlyTheMatchingSegmentTransactions() {
        Set<Integer> ids = collectAllIdsAcrossAllPages(null, null, "Groceries", 4);

        for (Transaction transaction : transactionRepository.findAllById(ids)) {
            assertEquals("Groceries", transaction.getSegment());
        }
    }

    // AC-8/AC-10: date range AND segment combine - within [01-03, 01-08], only day 07 is
    // "Groceries", so exactly 1 transaction matches (not the 6 that match the date range alone,
    // nor the 4 that match the segment alone).
    @Test
    void dateAndSegmentFiltersCombineWithAnd() {
        Set<Integer> ids = collectAllIdsAcrossAllPages(LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 8), "Groceries", 1);

        Transaction onlyMatch = transactionRepository.findById(ids.iterator().next()).orElseThrow();
        assertEquals(LocalDate.of(2024, 1, 7), onlyMatch.getDate());
        assertEquals("Groceries", onlyMatch.getSegment());
    }

    // AC-11: a legitimately-empty combination - a valid segment combined with a date range that
    // has zero matches for that segment - is 200 with totalElements=0, totalPages=0, empty
    // content, not an error.
    @Test
    void combinationWithZeroMatchesReturnsEmptyPageNotAnError() {
        Page<Transaction> result = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 0, 10, LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 4), "Groceries");

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.getContent().isEmpty());
    }

    // AC-6/AC-12: segment matching is exact and case-sensitive - "groceries" (lowercase) must not
    // match any of the four "Groceries" transactions.
    @Test
    void segmentFilterIsCaseSensitiveAndDoesNotMatchDifferentCasing() {
        Page<Transaction> result = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 0, 10, null, null, "groceries");

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    // AC-6: segment="Undefined" is plain equality against the literal string "Undefined", no
    // special-case branch - matches days 05, 06, 09 (3 transactions).
    @Test
    void segmentEqualsUndefinedMatchesLiterally() {
        Set<Integer> ids = collectAllIdsAcrossAllPages(null, null, "Undefined", 3);

        for (Transaction transaction : transactionRepository.findAllById(ids)) {
            assertEquals("Undefined", transaction.getSegment());
        }
    }

    // AC-7: blank/whitespace-only segment means "no filter", not a literal empty-string match -
    // same result as passing no segment at all (10 matches, not 0).
    @Test
    void blankSegmentMeansNoFilterNotALiteralEmptyMatch() {
        Page<Transaction> result = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 0, 10, null, null, "   ");

        assertEquals(10, result.getTotalElements());
    }

    // AC-7: a non-blank, non-matching segment value is a valid request, not an error.
    @Test
    void nonMatchingSegmentValueIsAValidEmptyResultNotAnError() {
        Page<Transaction> result = accountService.getPaginatedAccountTransactions(
                mainAccount.getId(), 0, 10, null, null, "NonExistentSegment");

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.getContent().isEmpty());
    }

    // AC-4: explicit deterministic sort (date descending, then id descending as a tiebreaker)
    // means the first page's first row is always the most recent transaction, and consecutive
    // pages never repeat or skip a row - this cross-checks the no-duplicates/no-omissions
    // assertion in collectAllIdsAcrossAllPages with an explicit ordering check on unfiltered data.
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
}
