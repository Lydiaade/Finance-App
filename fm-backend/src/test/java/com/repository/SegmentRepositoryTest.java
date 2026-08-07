package com.repository;

import com.dto.Segment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// FM-19 follow-up: real-database coverage for SegmentRepository.findByNameIgnoreCaseAndIdNot -
// the rename-collision check backing SegmentService.renameSegment. Complements the
// Mockito-based SegmentServiceTest, which never exercises the actual derived query against a
// real (H2, test-scope) database.
@DataJpaTest
class SegmentRepositoryTest {

    @Autowired
    private SegmentRepository segmentRepository;

    @Test
    void findsADifferentSegmentWithACaseInsensitivelyMatchingName() {
        Segment groceries = segmentRepository.save(new Segment("Groceries"));
        Segment food = segmentRepository.save(new Segment("Food"));

        Optional<Segment> found = segmentRepository.findByNameIgnoreCaseAndIdNot("food", groceries.getId());

        assertTrue(found.isPresent());
        assertTrue(found.get().getId() == food.getId());
    }

    @Test
    void excludesTheSegmentBeingRenamedItselfFromTheCollisionCheck() {
        Segment groceries = segmentRepository.save(new Segment("Groceries"));

        // renaming "Groceries" to a name that only case-differs from its OWN current name must
        // not report a collision against itself.
        Optional<Segment> found = segmentRepository.findByNameIgnoreCaseAndIdNot("groceries", groceries.getId());

        assertFalse(found.isPresent());
    }

    @Test
    void returnsEmptyWhenNoSegmentHasAMatchingName() {
        Segment groceries = segmentRepository.save(new Segment("Groceries"));

        Optional<Segment> found = segmentRepository.findByNameIgnoreCaseAndIdNot("Entertainment", groceries.getId());

        assertFalse(found.isPresent());
    }
}
