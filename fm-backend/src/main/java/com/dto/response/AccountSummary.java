package com.dto.response;

// FM-23: minimal id + name view of a BankAccount for embedding in TransactionResponse - the
// success view only needs enough to display which account the transaction was saved against,
// not the full BankAccount entity.
public record AccountSummary(int id, String name) {
}
