package com.controller;

import com.dto.Account;
import com.dto.request.NewAccountRequest;
import com.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<Account>> getAccount() {
        return new ResponseEntity<>(accountService.getAllAccounts(), HttpStatus.OK);
    }

    @PostMapping("/account")
    public void addAccount(@RequestBody NewAccountRequest request) {
        Account account = new Account(request.name(), request.sortCode(), request.accountNumber());
        accountService.addAccount(account);
    }
}
