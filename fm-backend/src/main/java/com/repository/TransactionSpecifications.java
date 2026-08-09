package com.repository;

import com.dto.Transaction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

// FM-53: predicate builders for the account-scoped transactions query
// (AccountService.getPaginatedAccountTransactions), replacing the two hand-written native
// pagination queries introduced by FM-52. Each method returns a Specification<Transaction> that
// AccountService composes with Specification.where(...).and(...) - Spring Data derives both the
// row-fetching query and the count query from the same predicate, so there's no separate
// countQuery string to keep in sync (AC-1).
public class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    // Always required - every call into this path is scoped to one bank account.
    public static Specification<Transaction> hasAccountId(int accountId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("account").get("id"), accountId);
    }

    // AC-3: both bounds inclusive. Callers are expected to pass either both dates or neither -
    // that "both or neither" validation lives in AccountService, not here.
    public static Specification<Transaction> dateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("date"), startDate, endDate);
    }

    // AC-5/AC-6/AC-7/AC-12: exact, case-sensitive match, no trimming/normalization. A blank or
    // absent segment means "no filter" - callers should not add this predicate at all in that
    // case, which is why blank/null returns null here rather than an empty-string equality
    // predicate.
    public static Specification<Transaction> hasSegment(String segment) {
        if (!StringUtils.hasText(segment)) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("segment"), segment);
    }
}
