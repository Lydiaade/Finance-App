package com.dto.response;

import com.dto.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

// FM-23: response body for POST /transactions/transaction. Represents the persisted transaction
// (backend is the source of truth for the save, including the generated id) rather than just
// echoing back the client's submitted form values.
public record TransactionResponse(
        int id,
        LocalDate date,
        AccountSummary account,
        BigDecimal amount,
        String category,
        String paid_to,
        String memo
) {

    public static TransactionResponse from(Transaction transaction) {
        AccountSummary account = new AccountSummary(
                transaction.getAccount().getId(), transaction.getAccount().getName());
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDate(),
                account,
                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getPaid_to(),
                transaction.getMemo()
        );
    }
}
