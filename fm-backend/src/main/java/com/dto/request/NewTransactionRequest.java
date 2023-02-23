package com.dto.request;

import java.math.BigDecimal;

public record NewTransactionRequest(
        String date,
        BigDecimal amount,
        String category,
        String paid_to,
        String memo
) {

}