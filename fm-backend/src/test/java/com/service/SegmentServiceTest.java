package com.service;

import com.dto.Segment;
import com.repository.SegmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// FM-19 AC-11/AC-12 - getOrCreateSegment backs the two new "create inline" affordances
// (inline segment edit, AddTransactionForm's "+ Add new segment"). Deliberately separate from
// addSegment()/POST /segments/segment, which stays a bare save with no dedup (Flag F4 - the
// pre-existing Segments page add-flow is unchanged by this ticket).
@ExtendWith(MockitoExtension.class)
class SegmentServiceTest {

    @InjectMocks
    private SegmentService segmentService;

    @Mock
    private SegmentRepository segmentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // AC-11 - a not-yet-existing segment name is created.
    @Test
    void createsANewSegmentWhenNoCaseInsensitiveMatchExists() {
        when(segmentRepository.findByNameIgnoreCase("Groceries")).thenReturn(Optional.empty());
        when(segmentRepository.save(any(Segment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Segment result = segmentService.getOrCreateSegment("Groceries");

        assertEquals("Groceries", result.getName());
        verify(segmentRepository).save(any(Segment.class));
    }

    // AC-12 - typing a name matching an existing segment case-insensitively reuses it rather
    // than creating a duplicate, and the existing row's stored casing is preserved.
    @Test
    void reusesExistingSegmentCaseInsensitivelyWithoutCreatingADuplicate() {
        Segment existing = new Segment("Groceries");
        existing.setId(7);
        when(segmentRepository.findByNameIgnoreCase("groceries")).thenReturn(Optional.of(existing));

        Segment result = segmentService.getOrCreateSegment("groceries");

        assertEquals(7, result.getId());
        assertEquals("Groceries", result.getName());
        verify(segmentRepository, never()).save(any(Segment.class));
    }
}
