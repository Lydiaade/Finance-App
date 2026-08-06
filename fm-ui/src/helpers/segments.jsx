import { BACKEND_URL } from "../config";

// Sentinel <option> value for the "+ Add new segment" affordance shared by
// AddTransactionForm and the inline transaction-list segment editor (FM-19).
// Never a real segment name, so it can't collide with an actual segment.
export const ADD_NEW_SEGMENT_OPTION = "__ADD_NEW_SEGMENT__";

// FM-19 follow-up (Segments page): the usage-check, rename, and delete
// endpoints only write a deliberate, human-readable plain-text body for 400
// (validation/collision) and 404 (unknown segment id) - see
// SegmentController. Any other status (e.g. an unhandled 500) isn't
// guaranteed to have a readable body, so those fall back to a generic
// message rather than risking showing raw server internals to the user.
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

// FM-19 follow-up: read-only usage check backing the "what's linked to this
// segment" confirmation modal shown before a segment is renamed or deleted.
// Deliberately never swallows a failure into "0 usages" - the caller must
// treat a failed check as "unknown" and refuse to proceed with a delete.
export async function fetchSegmentUsage(segmentId) {
  const response = await fetch(`${BACKEND_URL}/segments/segment/${segmentId}/usage`);
  if (!response.ok) {
    throw new Error(
      await readErrorMessage(
        response,
        "Couldn't check how many transactions use this segment. Please try again."
      )
    );
  }
  const data = await response.json();
  return data.transactionCount;
}

// FM-19 follow-up: cascading rename. See SegmentService.renameSegment for
// exactly what's updated server-side; a 400 here means a blank name or a
// case-insensitive collision with a different existing segment (rejected,
// not merged) - the caller shows that message inline rather than closing
// whatever confirmation UI is open.
export async function renameSegment(segmentId, newName) {
  const response = await fetch(`${BACKEND_URL}/segments/segment/${segmentId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name: newName }),
  });
  if (!response.ok) {
    throw new Error(
      await readErrorMessage(response, "Couldn't rename this segment. Please try again.")
    );
  }
  return response.json();
}

// FM-19 follow-up: resets any transactions using this segment back to
// "Undefined" (does NOT delete those transactions) and cleans up any related
// future-classification rules server-side before deleting the segment
// itself - see SegmentService.deleteSegment. 204 on success, no body to
// parse.
export async function deleteSegmentById(segmentId) {
  const response = await fetch(`${BACKEND_URL}/segments/segment/${segmentId}`, {
    method: "DELETE",
  });
  if (!response.ok) {
    throw new Error(
      await readErrorMessage(response, "Couldn't delete this segment. Please try again.")
    );
  }
}

// Case-insensitive lookup, matching AC-12's dedup rule: "groceries" should
// find an existing "Groceries" segment rather than being treated as new.
export function findExistingSegment(segments, name) {
  const trimmed = (name || "").trim();
  if (!trimmed) return undefined;
  return segments.find(
    (segment) => segment.name.toLowerCase() === trimmed.toLowerCase()
  );
}

// FM-19: segment creation and case-insensitive dedup (AC-11/AC-12) happens
// server-side - both PATCH /transactions/transaction/{id}/segment and
// POST /transactions/transaction (addManualTransaction) call
// SegmentService.getOrCreateSegment internally and return the canonical,
// deduped segment name in their response. The frontend doesn't need to (and
// deliberately doesn't) call POST /segments/segment itself for these two
// "create inline" affordances - that endpoint is the older bare add-flow
// with no dedup, which this ticket intentionally leaves untouched (Flag F4).
//
// This helper just merges a canonical name the backend already returned
// into a local segment list, so it's immediately selectable elsewhere in
// the same session (AC-11) without a duplicate option if it was already
// known (AC-12).
export function mergeSegmentByName(segments, name) {
  if (!name || findExistingSegment(segments, name)) {
    return segments;
  }
  return [...segments, { id: `server-${name}`, name }];
}
