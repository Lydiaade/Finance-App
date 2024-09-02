package com.dto.request;

import java.math.BigDecimal;

public record NewBankAccountRequest(
        String name,
        String sortCode,
        String accountNumber,
        BigDecimal currentBalance,
        String accountType,
        String accountBank,
        String currency,
        String balanceDate,
        Boolean isMainAccount
) {
}
