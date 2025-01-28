package com.controller;

import com.dto.Segment;
import com.dto.Transaction;
import com.dto.request.NewSegmentRequest;
import com.dto.request.NewTransactionRequest;
import com.service.SegmentService;
import com.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/segments")
public class SegmentController {

    private final SegmentService segmentService;

    public SegmentController(SegmentService segmentService) {
        this.segmentService = segmentService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Segment>> getSegments() {
        return new ResponseEntity<>(segmentService.getAllSegments(), HttpStatus.OK);
    }

    @PostMapping("/segment")
    public void addSegment(@RequestBody NewSegmentRequest request) {
        Segment segment = new Segment(request.name());
        segmentService.addSegment(segment);
    }

    @DeleteMapping("/segment/{id}")
    public void deleteSegment(@PathVariable("id") Integer id) {
        segmentService.deleteSegment(id);
    }
}
