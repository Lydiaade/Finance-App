package com.dto.request;

import java.math.BigDecimal;

public record NewAccountRequest(
        String name,
        String sortCode,
        String accountNumber,
        BigDecimal currentBalance
) {
}
