package com.controller;

import com.dto.Segment;
import com.dto.Transaction;
import com.dto.request.NewSegmentRequest;
import com.dto.request.NewTransactionRequest;
import com.service.SegmentService;
import com.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/segments")
public class SegmentController {

    @Autowired
    private SegmentService segmentService;

    @GetMapping
    public ResponseEntity<List<Segment>> getSegments() {
        return new ResponseEntity<>(segmentService.getAllSegments(), HttpStatus.OK);
    }

    @PostMapping("/segment")
    public ResponseEntity<HttpStatus> addSegment(@RequestBody NewSegmentRequest request) {
        Segment segment = new Segment(request.name());
        segmentService.addSegment(segment);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/segment/{id}")
    public ResponseEntity<HttpStatus> deleteSegment(@PathVariable("id") Integer id) {
        segmentService.deleteSegment(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
