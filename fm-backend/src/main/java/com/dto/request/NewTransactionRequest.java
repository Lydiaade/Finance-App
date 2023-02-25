package com.dto.request;

import com.dto.Account;

import java.math.BigDecimal;

public record NewTransactionRequest(
        String date,
        Account account,
        BigDecimal amount,
        String category,
        String paid_to,
        String memo
) {

}