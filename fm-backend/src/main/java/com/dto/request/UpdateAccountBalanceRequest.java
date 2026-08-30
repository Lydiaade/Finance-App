package com.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateAccountBalanceRequest(
        BigDecimal currentBalance,
        LocalDate currentBalanceDate
) {
}
