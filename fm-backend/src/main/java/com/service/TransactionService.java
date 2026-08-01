package com.service;

import com.dto.BankAccount;
import com.dto.Transaction;
import com.dto.request.NewTransactionRequest;
import com.dto.response.TransactionResponse;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public void addTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
    }

    // FM-23: manual "add transaction" entry point. Validation lives here (not the controller)
    // per CLAUDE.md layering - the frontend also validates, but there is no auth, so anything
    // hitting this endpoint directly must still be checked server-side.
    public TransactionResponse addManualTransaction(NewTransactionRequest request) {
        if (request.amount() == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Amount cannot be zero");
        }
        if (request.paid_to() == null || request.paid_to().isBlank()) {
            throw new IllegalArgumentException("Paid to is required");
        }
        if (request.date() == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (request.date().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date cannot be in the future");
        }

        BankAccount account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account does not exist"));

        // fileUpload intentionally left null - this transaction was not created via CSV import.
        Transaction transaction = new Transaction(
                request.date(), account, request.amount(), request.category(), request.paid_to(), request.memo());
        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.from(saved);
    }

    public void deleteTransaction(int id){
        transactionRepository.deleteById(id);
    }
}
