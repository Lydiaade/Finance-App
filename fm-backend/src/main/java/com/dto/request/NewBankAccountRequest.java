package com.dto.request;

import java.math.BigDecimal;

public record NewBankAccountRequest(
        String name,
        String sortCode,
        String accountNumber,
        BigDecimal currentBalance
) {
}
