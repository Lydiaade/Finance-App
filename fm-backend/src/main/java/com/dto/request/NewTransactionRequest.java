package com.dto.request;

public record NewTransactionRequest(
        String date,
        Double amount,
        String category,
        String paid_to,
        String memo
) {

}