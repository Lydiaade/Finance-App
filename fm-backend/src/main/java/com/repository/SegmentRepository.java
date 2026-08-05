package com.repository;

import com.dto.Segment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SegmentRepository extends JpaRepository<Segment, Integer> {

    // FM-19: case-insensitive lookup backing the new "create inline" dedup affordances
    // (AC-11/AC-12). "name" has no underscore, so a plain derived query is unambiguous here
    // (unlike paid_to elsewhere in this ticket).
    Optional<Segment> findByNameIgnoreCase(String name);

    // FM-19 follow-up: rename-collision check - is there a DIFFERENT segment (id != the one being
    // renamed) that already has this name, case-insensitively? Used to reject a rename that would
    // otherwise silently merge two segments into one (see SegmentService.renameSegment).
    Optional<Segment> findByNameIgnoreCaseAndIdNot(String name, int id);
}
