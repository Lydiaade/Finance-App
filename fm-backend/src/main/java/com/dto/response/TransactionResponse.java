package com.dto.response;

import com.dto.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

// FM-23: response body for POST /transactions/transaction. Represents the persisted transaction
// (backend is the source of truth for the save, including the generated id) rather than just
// echoing back the client's submitted form values.
// segment (not category) is exposed here - it is the user-facing budget category the segment
// dropdown represents (real selected value, e.g. "Groceries", or the entity's "Undefined"
// default). category is intentionally omitted: it is a bank-provided transaction-type descriptor
// populated only by CSV import and is always null for manually-added transactions.
public record TransactionResponse(
        int id,
        LocalDate date,
        AccountSummary account,
        BigDecimal amount,
        String segment,
        String paid_to,
        String memo
) {

    public static TransactionResponse from(Transaction transaction) {
        // transaction.getAccount() is assumed non-null here: every caller (TransactionService)
        // validates the account exists before constructing/saving the Transaction, so this is
        // safe today but relies on that invariant holding at every call site.
        AccountSummary account = new AccountSummary(
                transaction.getAccount().getId(), transaction.getAccount().getName());
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDate(),
                account,
                transaction.getAmount(),
                transaction.getSegment(),
                transaction.getPaid_to(),
                transaction.getMemo()
        );
    }
}
