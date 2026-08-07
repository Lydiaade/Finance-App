package com.controller;

import com.dto.Segment;
import com.dto.Transaction;
import com.dto.request.NewSegmentRequest;
import com.dto.request.NewTransactionRequest;
import com.dto.request.RenameSegmentRequest;
import com.dto.response.RenameSegmentResponse;
import com.dto.response.SegmentUsageResponse;
import com.service.SegmentService;
import com.service.TransactionService;
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
@RequestMapping("/segments")
public class SegmentController {

    private static final Logger log = LoggerFactory.getLogger(SegmentController.class);

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

    // FM-19 follow-up: read-only usage check backing the "what's linked to this segment"
    // confirmation modal shown before a rename or delete.
    @GetMapping("/segment/{id}/usage")
    public ResponseEntity<?> getSegmentUsage(@PathVariable("id") int id) {
        try {
            SegmentUsageResponse response = segmentService.getSegmentUsage(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // FM-19 follow-up: cascading rename - see SegmentService.renameSegment for exactly what's
    // updated and the case-insensitive-collision rejection.
    @PatchMapping("/segment/{id}")
    public ResponseEntity<?> renameSegment(@PathVariable("id") int id, @RequestBody RenameSegmentRequest request) {
        try {
            RenameSegmentResponse response = segmentService.renameSegment(id, request.name());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Rejected segment rename request for segment {}: {}", id, e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // FM-19 follow-up: no longer a bare deleteById with zero checks - resets any linked
    // Transaction.segment values to "Undefined" and removes any matching PayeeSegmentRule rows
    // before deleting the Segment row itself. See SegmentService.deleteSegment.
    @DeleteMapping("/segment/{id}")
    public ResponseEntity<?> deleteSegment(@PathVariable("id") Integer id) {
        try {
            segmentService.deleteSegment(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
