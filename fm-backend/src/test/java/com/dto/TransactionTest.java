package com.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionTest {

    @Test
    public void transactionTransformsDateToDateObject() {
        Transaction transaction = new Transaction("31/03/2022", BigDecimal.valueOf(-9), "Debit", "BAR BRUNO", "ON 29 MAR CPM");
        LocalDate expectedDate = LocalDate.of(2022, 3, 31);
        assertEquals(expectedDate, transaction.getDate());
    }
}