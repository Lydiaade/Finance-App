package com.service;

import com.dto.Segment;
import com.repository.SegmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SegmentService {
    private final SegmentRepository segmentRepository;

    public SegmentService(SegmentRepository segmentRepository) {
        this.segmentRepository = segmentRepository;
    }

    public List<Segment> getAllSegments() {
        return segmentRepository.findAll();
    }

    public void addSegment(Segment segment) {
        segmentRepository.save(segment);
    }

    public void deleteSegment(int segmentId) {
        segmentRepository.deleteById(segmentId);
    }
}
