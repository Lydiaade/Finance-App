package com.controller;

import com.dto.Transaction;
import com.dto.request.NewTransactionRequest;
import com.dto.request.UpdateTransactionSegmentRequest;
import com.dto.response.SegmentPreviewResponse;
import com.dto.response.TransactionResponse;
import com.dto.response.UpdateTransactionSegmentResponse;
import com.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions() {
        return new ResponseEntity<>(transactionService.getAllTransactions(), HttpStatus.OK);
    }

    @PostMapping("/transaction")
    public ResponseEntity<?> addTransaction(@RequestBody NewTransactionRequest request) {
        try {
            TransactionResponse response = transactionService.addManualTransaction(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.warn("Rejected new transaction request: {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/transaction/{id}")
    public ResponseEntity<HttpStatus> deleteTransaction(@PathVariable("id") Integer id) {
        transactionService.deleteTransaction(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // FM-19 AC-4: read-only preview, no side effects - safe to call before the user confirms
    // anything (e.g. on every inline-edit selection change).
    @GetMapping("/transaction/{id}/segment-preview")
    public ResponseEntity<?> previewSegmentChange(
            @PathVariable("id") int id,
            @RequestParam("segment") String segment) {
        try {
            SegmentPreviewResponse response = transactionService.previewSegmentChange(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // FM-19 AC-5: commits the segment edit - always updates the target transaction and the
    // paid_to -> segment rule; only bulk-renames other matching transactions if
    // applyToExisting is true.
    @PatchMapping("/transaction/{id}/segment")
    public ResponseEntity<?> updateTransactionSegment(
            @PathVariable("id") int id,
            @RequestBody UpdateTransactionSegmentRequest request) {
        try {
            UpdateTransactionSegmentResponse response = transactionService.updateTransactionSegment(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Rejected segment update request for transaction {}: {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
