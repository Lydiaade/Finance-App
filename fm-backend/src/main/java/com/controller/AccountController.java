package com.controller;

import com.dto.Account;
import com.dto.request.NewAccountRequest;
import com.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
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
    public Object getAccountTransactions(@PathVariable("id") Integer id) {
        return new ResponseEntity<>(accountService.getAccountTransactions(id), HttpStatus.OK);
    }

    @PostMapping("/account")
    public void addAccount(@RequestBody NewAccountRequest request) {
        LocalDate currentBalanceDate = LocalDate.now();
        Account account = new Account(request.name(), request.sortCode(), request.accountNumber(), request.currentBalance(), currentBalanceDate);
        accountService.addAccount(account);
    }
}
