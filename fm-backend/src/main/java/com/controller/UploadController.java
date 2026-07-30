package com.controller;

import com.dto.response.FileInfoResponse;
import com.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private UploadService uploadService;

    @GetMapping
    public ResponseEntity<List<FileInfoResponse>> getUploads() {
        return new ResponseEntity<>(uploadService.getAllUploads(), HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadTransactions(@RequestParam("file") MultipartFile file, @RequestParam("bankAccount") int bankAccountId) {
        try {
            FileInfoResponse response = uploadService.saveFile(file, bankAccountId);
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
        } catch (IllegalArgumentException e) {
            log.warn("Upload rejected - account mismatch for accountId={}: {}", bankAccountId, e.getMessage());
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (Exception e) {
            log.error("Upload failed for accountId={}", bankAccountId, e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
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
