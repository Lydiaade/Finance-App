package com.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NewTransactionRequest(
        LocalDate date,
        Integer accountId,
        BigDecimal amount,
        String category,
        String paid_to,
        String memo
) {

}
