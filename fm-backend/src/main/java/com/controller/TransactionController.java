package com.controller;

import com.dto.FileUpload;
import com.dto.Transaction;
import com.dto.request.NewTransactionRequest;
import com.dto.response.FileInfoResponse;
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

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions() {
        return new ResponseEntity<>(transactionService.getAllTransactions(), HttpStatus.OK);
    }

    @PostMapping("/transaction")
    public ResponseEntity<HttpStatus> addTransaction(@RequestBody NewTransactionRequest request) {
        Transaction transaction = new Transaction(
                request.date(), request.account(), request.amount(), request.category(), request.paid_to(), request.memo());
        transactionService.addTransaction(transaction);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/transaction/{id}")
    public ResponseEntity<HttpStatus> deleteTransaction(@PathVariable("id") Integer id) {
        transactionService.deleteTransaction(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
