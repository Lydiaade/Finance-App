package com.service;

import com.dto.Account;
import com.dto.Transaction;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccount(Integer id) throws FileNotFoundException {
        Optional<Account> account = accountRepository.findById(id);
        if (account.isEmpty()) {
            throw new FileNotFoundException("This account does not exist");
        }
        return account.get();
    }

    public List<Transaction> getAccountTransactions(Integer id){
        return transactionRepository.findAllByAccount_Id(id);
    }

    public void addAccount(Account account) {
        accountRepository.save(account);
    }
}
