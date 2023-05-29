package com;

import com.dto.Segment;
import com.repository.SegmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements ApplicationRunner {
    private final SegmentRepository segmentRepository;

    @Autowired
    public DataLoader(SegmentRepository segmentRepository) {
        this.segmentRepository = segmentRepository;
    }

    public void run(ApplicationArguments args) {
        segmentRepository.save(new Segment("Undefined"));
    }
}
