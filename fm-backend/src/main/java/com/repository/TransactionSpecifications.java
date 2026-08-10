package com.repository;

import com.dto.Transaction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

public class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> hasAccountId(int accountId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("account").get("id"), accountId);
    }

    public static Specification<Transaction> dateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("date"), startDate, endDate);
    }

    public static Specification<Transaction> hasSegment(String segment) {
        if (!StringUtils.hasText(segment)) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("segment"), segment);
    }
}
