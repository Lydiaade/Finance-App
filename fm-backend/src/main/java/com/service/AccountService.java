package com.service;

import com.dto.Account;
import com.dto.Transaction;
import com.helper.CSVHelper;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public void addAccount(Account account) {
        accountRepository.save(account);
    }
}
