package com.dto.request;

public record NewAccountRequest(
        String name,
        String sortCode,
        String accountNumber
) {
}
