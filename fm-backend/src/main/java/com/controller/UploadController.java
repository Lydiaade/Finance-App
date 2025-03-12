package com.controller;

import com.dto.response.FileInfoResponse;
import com.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/uploads")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    @GetMapping
    public ResponseEntity<List<FileInfoResponse>> getUploads() {
        return new ResponseEntity<>(uploadService.getAllUploads(), HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadTransactions(@RequestBody MultipartFile file) {
        try {
            FileInfoResponse response = uploadService.saveFile(file);
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @GetMapping("/upload/{id}")
    public ResponseEntity<?> getUpload(@PathVariable("id") long id) {
        try {
            return new ResponseEntity<>(uploadService.getFile(id), HttpStatus.OK);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/upload/{id}")
    public ResponseEntity<HttpStatus> deleteUpload(@PathVariable("id") long id) {
        uploadService.deleteFile(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
