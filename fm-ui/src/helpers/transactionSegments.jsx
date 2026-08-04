import { BACKEND_URL } from "../config";

// Both new endpoints only write a deliberate, human-readable plain-text
// body for 400 (validation) and 404 (unknown transaction id) - see
// TransactionController. Any other status (e.g. an unhandled 500) isn't
// guaranteed to have a readable body (could be a JSON error object or an
// HTML error page), so those fall back to the generic message rather than
// risking showing raw server internals to the user.
async function readErrorMessage(response, genericMessage) {
  if (response.status !== 400 && response.status !== 404) {
    return genericMessage;
  }
  try {
    const reason = (await response.text()).trim();
    return reason || genericMessage;
  } catch (readError) {
    return genericMessage;
  }
}

// FM-19: read-only preview of how many *other* transactions share this
// transaction's paid_to and would be renamed if the caller applies this
// segment change to existing matches too (AC-4). No side effects - safe to
// call on every segment pick before anything is persisted. The candidate
// `segmentName` is sent per the documented API contract but doesn't affect
// the count (matching is purely on paid_to) - it doesn't need to already
// exist as a real Segment.
export async function fetchSegmentPreviewCount(transactionId, segmentName) {
  const response = await fetch(
    `${BACKEND_URL}/transactions/transaction/${transactionId}/segment-preview?segment=${encodeURIComponent(
      segmentName
    )}`
  );

  if (!response.ok) {
    throw new Error(
      await readErrorMessage(
        response,
        "Couldn't check how many other transactions would be affected. Please try again."
      )
    );
  }

  const data = await response.json();
  return data.matchingTransactionCount;
}

// FM-19: commits the actual segment change (AC-5). Always saves the target
// transaction's own segment; additionally renames every other transaction
// system-wide with the same paid_to when applyToExisting is true. The
// paid_to -> segment rule for future transactions is established
// server-side unconditionally - nothing extra to do here for that part.
//
// `segmentName` may be a brand-new name that doesn't exist as a Segment yet
// (e.g. typed via the "+ Add new segment" affordance) - the backend creates
// it (with case-insensitive dedup) as part of this same call and returns
// the canonical name in the response's `transaction.segment` field, which
// callers should treat as the source of truth rather than assuming
// `segmentName` was saved verbatim.
export async function updateTransactionSegment(
  transactionId,
  segmentName,
  applyToExisting
) {
  const response = await fetch(
    `${BACKEND_URL}/transactions/transaction/${transactionId}/segment`,
    {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ segment: segmentName, applyToExisting }),
    }
  );

  if (!response.ok) {
    throw new Error(
      await readErrorMessage(
        response,
        "Couldn't save the segment change. Please try again."
      )
    );
  }

  return response.json();
}
