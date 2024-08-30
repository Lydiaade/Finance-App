package com.dto.request;

import com.dto.BankName;

import java.math.BigDecimal;
import java.util.Currency;

public record NewBankAccountRequest(
        String name,
        String sortCode,
        String accountNumber,
        BigDecimal currentBalance,
        String AccountType,
        String BankName,
        String Currency,
        String currentBalanceDate,
        Boolean isMainBankAccount
) {
}
