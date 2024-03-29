package com.controller;

import com.dto.Account;
import com.dto.Transaction;
import com.dto.request.NewAccountRequest;
import com.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.*;


@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Account>> getAccounts() {
        return new ResponseEntity<>(accountService.getAllAccounts(), HttpStatus.OK);
    }

    @GetMapping("/account/{id}")
    public Object getAccount(@PathVariable("id") Integer id) {
        try {
            return new ResponseEntity<>(accountService.getAccount(id), HttpStatus.OK);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/account/{id}/transactions")
    public ResponseEntity<List<Transaction>> getAccountTransactions(@PathVariable("id") Integer id) {
        return new ResponseEntity<>(accountService.getAccountTransactions(id), HttpStatus.OK);
    }

    @GetMapping("/account/{id}/transactions/monthly")
    public Object getAccountAnnualMonthlyTransactions(@PathVariable("id") Integer id) {
        return new ResponseEntity<>(accountService.getAccountAnnualMonthlyTransactions(id), HttpStatus.OK);
    }

    @PostMapping("/account")
    public void addAccount(@RequestBody NewAccountRequest request) {
        LocalDate currentBalanceDate = LocalDate.now();
        Account account = new Account(request.name(), request.sortCode(), request.accountNumber(), request.currentBalance(), currentBalanceDate);
        accountService.addAccount(account);
    }

    @DeleteMapping("/account/{id}")
    public Object deleteAccount(@PathVariable("id") Integer id) {
        try {
            accountService.deleteAccount(id);
            return HttpStatus.NO_CONTENT;
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
