// Sentinel <option> value for the "+ Add new segment" affordance shared by
// AddTransactionForm and the inline transaction-list segment editor (FM-19).
// Never a real segment name, so it can't collide with an actual segment.
export const ADD_NEW_SEGMENT_OPTION = "__ADD_NEW_SEGMENT__";

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
