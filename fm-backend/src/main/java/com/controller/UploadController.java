package com.controller;

import com.dto.FileUpload;
import com.dto.response.FileInfoResponse;
import com.service.TransactionService;
import com.service.UploadService;
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
    private UploadService uploadService;

    @GetMapping
    public ResponseEntity<List<FileUpload>> getUploads() {
        return new ResponseEntity<>(uploadService.getAllUploads(), HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity uploadTransactions(@RequestBody MultipartFile file) {
        try {
            FileInfoResponse response = uploadService.saveFile(file);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @DeleteMapping("/upload/{id}")
    public HttpStatus deleteUpload(@PathVariable("id") long id) {
        uploadService.deleteFile(id);
        return HttpStatus.NO_CONTENT;
    }

}
