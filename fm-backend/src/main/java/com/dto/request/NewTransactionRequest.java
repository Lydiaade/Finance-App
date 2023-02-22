package com.dto.request;

public record NewTransactionRequest(
        String date,
        Float amount,
        String category,
        String paid_to,
        String memo
) {

}