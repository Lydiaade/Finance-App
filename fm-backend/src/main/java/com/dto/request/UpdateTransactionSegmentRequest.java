package com.dto.request;

// FM-19: body for PATCH /transactions/transaction/{id}/segment.
// applyToExisting governs ONLY whether other transactions sharing the same paid_to are
// retroactively renamed in this same request (AC-5 step 3) - it does NOT gate whether the
// paid_to -> segment rule is (re)established for future transactions, which happens
// unconditionally (AC-6/Flag F2, flagged at PR time as a judgment call).
public record UpdateTransactionSegmentRequest(
        String segment,
        boolean applyToExisting
) {
}
