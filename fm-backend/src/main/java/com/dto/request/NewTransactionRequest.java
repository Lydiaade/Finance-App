package com.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

// FM-23: date is a LocalDate (not String) so Jackson's jsr310 module parses the ISO yyyy-MM-dd
// produced by a native <input type="date"> directly - the old String + Transaction.transformStringToDate
// (d/M/yyyy split on "/") could not handle that format.
// accountId is a real id looked up server-side via AccountRepository - the old shape accepted a
// full client-supplied BankAccount object and persisted it as-is, which is not safe/correct.
// accountId is boxed (Integer, not int) so a missing "accountId" key in the JSON payload
// deserializes to null rather than silently defaulting to 0 - this lets the service distinguish
// "accountId missing" from "accountId doesn't match any account" instead of relying on the
// coincidence that generated ids start at 1.
public record NewTransactionRequest(
        LocalDate date,
        Integer accountId,
        BigDecimal amount,
        String category,
        String paid_to,
        String memo
) {

}
