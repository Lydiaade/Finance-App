package com.dto.response;

// FM-19 follow-up: response body for GET /segments/segment/{id}/usage.
// transactionCount is the number of Transaction rows whose denormalized segment string exactly
// equals this segment's name. Read-only - backs the "what's linked to this segment" confirmation
// modal shown before a user renames or deletes a segment that's actually in use.
public record SegmentUsageResponse(
        int transactionCount
) {
}
