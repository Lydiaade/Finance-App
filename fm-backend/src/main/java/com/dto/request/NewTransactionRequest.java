package com.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

// FM-23: date is a LocalDate (not String) so Jackson's jsr310 module parses the ISO yyyy-MM-dd
// produced by a native <input type="date"> directly - the old String + Transaction.transformStringToDate
// (d/M/yyyy split on "/") could not handle that format.
// accountId is a real id looked up server-side via AccountRepository - the old shape accepted a
// full client-supplied BankAccount object and persisted it as-is, which is not safe/correct.
public record NewTransactionRequest(
        LocalDate date,
        int accountId,
        BigDecimal amount,
        String category,
        String paid_to,
        String memo
) {

}
