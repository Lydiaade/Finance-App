package com.controller;

import com.dto.*;
import com.dto.request.NewBankAccountRequest;
import com.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping
    public ResponseEntity<List<BankAccount>> getAccounts() {
        return new ResponseEntity<>(accountService.getAllAccounts(), HttpStatus.OK);
    }

    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccount(@PathVariable("id") int id) {
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

    // FM-52: startDate/endDate are optional, ISO yyyy-MM-dd (bound via Spring's default LocalDate
    // conversion, same format the manual-add-transaction JSON path already uses). All range
    // validation (both-required, start-after-end, future-date) lives in AccountService per the
    // existing controller-thin/service-holds-logic convention - the controller only translates the
    // service's IllegalArgumentException into a 400, matching TransactionController's pattern for
    // addTransaction/updateTransactionSegment.
    // FM-53: segment is a new optional query param (AC-5). A blank/whitespace-only/absent value
    // means "no segment filter" - that's handled in TransactionSpecifications.hasSegment, not
    // here, so the controller stays thin and passes the raw param straight through.
    @GetMapping("/account/{id}/transactions")
    public ResponseEntity<?> getAccountTransactions(
            @PathVariable("id") int id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String segment) {
        try {
            Page<Transaction> transactions = accountService.getPaginatedAccountTransactions(id, page, size, startDate, endDate, segment);
            return new ResponseEntity<>(transactions, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/account/{id}/transactions/monthly")
    public ResponseEntity<List<MonthlyTransactionTotal>> getAccountAnnualMonthlyTransactions(@PathVariable("id") int id) {
        return new ResponseEntity<>(accountService.getAccountAnnualMonthlyTransactions(id), HttpStatus.OK);
    }

    @PostMapping("/account")
    public ResponseEntity<HttpStatus> addAccount(@RequestBody NewBankAccountRequest request) {
        BankAccount account = new BankAccount(request.name(), request.sortCode(), request.accountNumber(), request.accountType(), request.accountBank(), request.currency(), request.currentBalance(), request.balanceDate());
        accountService.addAccount(account);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/account/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable("id") int id) {
        try {
            accountService.deleteAccount(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
