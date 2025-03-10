package com.controller;

import com.dto.FileUpload;
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
@RequestMapping("/uploads")
public class UploadController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/upload/csv")
    public ResponseEntity uploadTransactions(@RequestBody MultipartFile file) {
        try {
            FileInfoResponse response = transactionService.saveFile(file);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @GetMapping
    public ResponseEntity<List<FileUpload>> getUploads() {
        return new ResponseEntity<>(transactionService.getAllUploads(), HttpStatus.OK);
    }

}
