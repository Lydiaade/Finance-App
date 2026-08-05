package com.dto.response;

import com.dto.Segment;

// FM-19 follow-up: response body for PATCH /segments/segment/{id}.
// segment is the renamed Segment itself (new name reflected). updatedTransactionCount and
// updatedRuleCount report how many Transaction/PayeeSegmentRule rows were cascaded to the new name,
// so a caller can confirm the scope of what just changed without a separate follow-up call.
public record RenameSegmentResponse(
        Segment segment,
        int updatedTransactionCount,
        int updatedRuleCount
) {
}
