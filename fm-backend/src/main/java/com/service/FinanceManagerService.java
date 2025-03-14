package com.service;

import com.dto.Transaction;
import com.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

@Service
public class FinanceManagerService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Dictionary<String, BigDecimal> getAccountOverview() {
        Dictionary<String, BigDecimal> accountOverview = new Hashtable<>();
        accountOverview.put("startingBalance", getStartingBalance());
        accountOverview.put("currentBalance", getCurrentBalance());
        accountOverview.put("totalInflow", getTotalInflow());
        accountOverview.put("totalOutflow", getTotalOutflow());
        return accountOverview;
    }

    public BigDecimal getTotalAmount(List<Transaction> transactions) {
        BigDecimal inflow = new BigDecimal(0);
        for (Transaction transaction : transactions) {
            inflow = inflow.add(transaction.getAmount());
        }
        return inflow;
    }

    private BigDecimal getStartingBalance() {
        return new BigDecimal(3000);
    }

    private BigDecimal getTotalInflow() {
        List<Transaction> transactions = transactionRepository.findByAmountGreaterThan(0.0);
        return getTotalAmount(transactions);
    }

    private BigDecimal getTotalOutflow() {
        List<Transaction> transactions = transactionRepository.findByAmountLessThan(0.0);
        return getTotalAmount(transactions).negate();
    }

    private BigDecimal getCurrentBalance() {
        List<Transaction> transactions = transactionRepository.findAll();
        BigDecimal totalSpend = getTotalAmount(transactions);
        return getStartingBalance().subtract(totalSpend);
    }
}
