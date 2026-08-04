package com.service;

import com.dto.Segment;
import com.repository.SegmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SegmentService {

    @Autowired
    private SegmentRepository segmentRepository;

    public List<Segment> getAllSegments() {
        return segmentRepository.findAll();
    }

    public void addSegment(Segment segment) {
        segmentRepository.save(segment);
    }

    public void deleteSegment(int segmentId) {
        segmentRepository.deleteById(segmentId);
    }

    // FM-19: backs the two new "create inline" affordances (inline segment edit,
    // AddTransactionForm's "+ Add new segment") - deliberately NOT wired into addSegment()/the
    // existing POST /segments/segment flow, which stays a bare save with zero dedup
    // (SegmentContainer.jsx's existing add-flow is unchanged by this ticket - AC-12/Flag F4).
    // Case-insensitive: typing "groceries" when "Groceries" already exists reuses the existing
    // row (and its stored casing) rather than creating a duplicate.
    public Segment getOrCreateSegment(String name) {
        return segmentRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> segmentRepository.save(new Segment(name)));
    }
}
