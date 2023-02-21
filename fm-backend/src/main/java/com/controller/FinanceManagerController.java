package com.controller;

import com.dto.Transaction;
import com.repository.TransactionRepository;
import com.service.FinanceManagerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class FinanceManagerController {

    private final TransactionRepository transactionRepository;

    public FinanceManagerController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/")
    public ResponseEntity<String> status_check() {
        return new ResponseEntity<>("Finance Manager is running", HttpStatus.OK);
    }

    @GetMapping("/salary")
    public ResponseEntity<Integer> current_salary() {
        FinanceManagerService service = new FinanceManagerService();
        return new ResponseEntity<>(service.getCurrentSalary(), HttpStatus.OK);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactions() {
        System.out.print(transactionRepository.findAll());
        return new ResponseEntity<>(transactionRepository.findAll(), HttpStatus.OK);
    }

    record NewTransactionRequest(
            String date,
            Float amount,
            String category,
            String paid_to,
            String memo
    ) {

    }
    @PostMapping("/transaction")
    public void addTransaction(@RequestBody NewTransactionRequest request) {
        Transaction transaction = new Transaction(
                request.date(), request.amount(), request.category(), request.paid_to(), request.memo());
        transactionRepository.save(transaction);
    }

    @PostMapping("/upload/transactions")
    public void uploadTransactions(@RequestBody MultipartFile file) {
        System.out.print("Transaction uploaded");
        // TODO: Generate transactions from uploaded csv!
        System.out.print(file);
    }

    @DeleteMapping("/transaction/{customerId}")
    public void deleteTransaction(@PathVariable("customerId") Integer id) {
        transactionRepository.deleteById(id);
    }

}
