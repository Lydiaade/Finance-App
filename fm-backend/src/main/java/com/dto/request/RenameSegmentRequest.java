package com.dto.request;

// FM-19 follow-up: body for PATCH /segments/segment/{id}. name is the new segment name - the
// cascading rename (Segment itself, every matching Transaction.segment, every matching
// PayeeSegmentRule.segment) happens in SegmentService.renameSegment.
public record RenameSegmentRequest(
        String name
) {
}
