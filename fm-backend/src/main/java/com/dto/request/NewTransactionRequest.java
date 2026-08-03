package com.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

// segment holds the value selected from the segment dropdown and maps to Transaction.segment
// (the user-facing budget category) - NOT Transaction.category, which is a bank-provided
// transaction-type descriptor (e.g. "Debit"/"Bill Payment") populated only by CSVHelper on
// import and left null for manually-added transactions.
public record NewTransactionRequest(
        LocalDate date,
        Integer accountId,
        BigDecimal amount,
        String segment,
        String paid_to,
        String memo
) {

}
