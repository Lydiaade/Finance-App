package com.dto.request;

import com.dto.BankAccount;

import java.math.BigDecimal;

public record NewTransactionRequest(
        String date,
        BankAccount account,
        BigDecimal amount,
        String category,
        String paid_to,
        String memo
) {

}