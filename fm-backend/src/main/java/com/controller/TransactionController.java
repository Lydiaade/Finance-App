package com.controller;

import com.dto.Transaction;
import com.dto.request.NewTransactionRequest;
import com.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Transaction>> getTransactions() {
        return new ResponseEntity<>(transactionService.getAllTransactions(), HttpStatus.OK);
    }

    @PostMapping("/transaction")
    public void addTransaction(@RequestBody NewTransactionRequest request) {
        Transaction transaction = new Transaction(
                request.date(), request.account(), request.amount(), request.category(), request.paid_to(), request.memo());
        transactionService.addTransaction(transaction);
    }

    @PostMapping("/upload/csv")
    public ResponseEntity<String> uploadTransactions(@RequestBody MultipartFile file) {
        String message;
        try {
            transactionService.saveFile(file);
            message = "File uploaded: " + file.getOriginalFilename() + "!";
            return new ResponseEntity<>(message, HttpStatus.OK);
        } catch (Exception e) {
            message = "Could not upload the file: " + file.getOriginalFilename() + "!";
            return new ResponseEntity<>(message, HttpStatus.EXPECTATION_FAILED);
        }
    }

    @DeleteMapping("/transaction/{customerId}")
    public void deleteTransaction(@PathVariable("customerId") Integer id) {
        transactionService.deleteTransaction(id);
    }
}
