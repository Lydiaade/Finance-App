package com.dto.response;

// FM-19: response body for PATCH /transactions/transaction/{id}/segment.
// updatedTransactionCount is the number of OTHER transactions that were actually renamed in this
// request (0 when applyToExisting was false, or when there were no other matches either way).
public record UpdateTransactionSegmentResponse(
        TransactionResponse transaction,
        int updatedTransactionCount
) {
}
