package com.controller;

import com.dto.BankAccount;
import com.dto.BankAccountType;
import com.dto.BankName;
import com.dto.Transaction;
import com.dto.request.NewBankAccountRequest;
import com.service.AccountService;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/")
    public ResponseEntity<List<BankAccount>> getAccounts() {
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

    @GetMapping("/types")
    public ResponseEntity<EnumSet<BankAccountType>> getAccountTypes() {
        return new ResponseEntity<>(EnumSet.allOf(BankAccountType.class), HttpStatus.OK);
    }

    @GetMapping("/banks")
    public ResponseEntity<EnumSet<BankName>> getBankAccounts() {
        return new ResponseEntity<>(EnumSet.allOf(BankName.class), HttpStatus.OK);
    }

    @GetMapping("/currencies")
    public ResponseEntity<Set<Currency>> getCurrencies() {
        return new ResponseEntity<>(Currency.getAvailableCurrencies(), HttpStatus.OK);
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
    public void addAccount(@RequestBody NewBankAccountRequest request) {
        BankAccount account = new BankAccount(request.name(), request.sortCode(), request.accountNumber(), request.accountType(), request.accountBank(), request.currency(), request.currentBalance(), request.balanceDate());
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
