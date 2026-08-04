package com.dto.response;

// FM-19: response body for GET /transactions/transaction/{id}/segment-preview.
// matchingTransactionCount is the number of OTHER transactions system-wide (all bank accounts)
// whose paid_to exactly matches the target transaction's own paid_to, excluding the transaction
// being edited itself. Read-only - computing this must have zero side effects.
public record SegmentPreviewResponse(
        int matchingTransactionCount
) {
}
