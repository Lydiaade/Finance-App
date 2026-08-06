import React, { useState } from "react";
import { Form, Button, Modal, Alert, Spinner } from "react-bootstrap";
import { ADD_NEW_SEGMENT_OPTION } from "../helpers/segments";
import {
  fetchSegmentPreviewCount,
  updateTransactionSegment,
} from "../helpers/transactionSegments";

// FM-19: converted from a class component to a function component. The
// segment cell needs local editing/loading/error state (inline edit +
// confirmation popup), which is what the ticket specifically asked this
// file to grow - this isn't a drive-by rewrite of an unrelated file.
function Transaction({ transaction, segments, onSegmentAdded, onSegmentUpdated }) {
  const { id, date, amount, category, paid_to, memo, segment } = transaction;

  const [showAddInput, setShowAddInput] = useState(false);
  const [newSegmentDraft, setNewSegmentDraft] = useState("");
  const [status, setStatus] = useState("idle"); // "idle" | "checking" | "saving"
  const [error, setError] = useState("");
  const [modal, setModal] = useState(null); // null | { count, segmentName }
  const [retryHandler, setRetryHandler] = useState(null);
  // FM-19 review follow-up: once a transaction's segment has been explicitly
  // set (i.e. it's no longer the default "Undefined"), the row no longer
  // shows an always-open <Form.Select> - it's treated as "classified" and
  // shown as plain text. Changing it after that point requires opening this
  // "Change Segment" modal, which reuses the exact same picker UI/preview/
  // persist flow, just entered from a click instead of always being visible.
  const [showChangeModal, setShowChangeModal] = useState(false);

  const isUndefined = segment === "Undefined";

  const optionNames = segments.map((option) => option.name);
  // Guarantees the current value is always a valid, pre-selected <option>,
  // even for values like "Undefined" that aren't a real Segment record
  // (AC-13: "current segment value is visible and pre-selected").
  const dropdownOptions = optionNames.includes(segment)
    ? optionNames
    : [segment, ...optionNames];

  const busy = status !== "idle";
  const disabled = busy || modal !== null;

  // Commits the change via PATCH .../segment (AC-5). `segmentName` may be a
  // brand-new name typed via "+ Add new segment" - segment creation and
  // case-insensitive dedup (AC-11/AC-12) happen server-side
  // (SegmentService.getOrCreateSegment), so this never calls
  // POST /segments/segment itself. The response's canonical segment name is
  // treated as the source of truth for both the row's own display and for
  // making the (possibly new) segment immediately selectable elsewhere.
  const persist = async (segmentName, applyToExisting) => {
    setStatus("saving");
    setError("");
    setRetryHandler(() => () => persist(segmentName, applyToExisting));
    try {
      const response = await updateTransactionSegment(id, segmentName, applyToExisting);
      const canonicalName = response.transaction.segment;
      onSegmentUpdated(id, canonicalName);
      onSegmentAdded(canonicalName);
      setShowAddInput(false);
      setNewSegmentDraft("");
      setModal(null);
      setShowChangeModal(false);
      setRetryHandler(null);
      setStatus("idle");
    } catch (err) {
      setError(err.message || "Something went wrong saving this change. Please try again.");
      setStatus("idle");
    }
  };

  const runPreviewAndMaybePersist = async (segmentName) => {
    setError("");
    setStatus("checking");
    setRetryHandler(() => () => runPreviewAndMaybePersist(segmentName));
    try {
      const matchingTransactionCount = await fetchSegmentPreviewCount(id, segmentName);
      setRetryHandler(null);
      if (matchingTransactionCount > 0) {
        setStatus("idle");
        setModal({ count: matchingTransactionCount, segmentName });
      } else {
        await persist(segmentName, false);
      }
    } catch (err) {
      setError(err.message || "Something went wrong checking this change. Please try again.");
      setStatus("idle");
    }
  };

  const handleSelectChange = (event) => {
    const value = event.target.value;
    if (value === ADD_NEW_SEGMENT_OPTION) {
      setShowAddInput(true);
      setError("");
      return;
    }
    if (value === segment) return;
    runPreviewAndMaybePersist(value);
  };

  const handleAddNewConfirm = () => {
    const trimmed = newSegmentDraft.trim();
    if (!trimmed) return;
    setError("");
    runPreviewAndMaybePersist(trimmed);
  };

  const handleAddNewCancel = () => {
    setShowAddInput(false);
    setNewSegmentDraft("");
    setError("");
  };

  const handleModalConfirm = () => {
    if (!modal) return;
    persist(modal.segmentName, true);
  };

  const handleModalDecline = () => {
    if (!modal) return;
    persist(modal.segmentName, false);
  };

  // AC-17: dismissing the modal without an explicit Confirm/Decline click
  // (Escape, backdrop click, header close button) is treated as Decline -
  // the edited transaction's own change is still saved; other matching
  // transactions are not renamed. Flagged as adjustable (F6) if the project
  // lead wants a true no-op cancel instead.
  const handleModalHide = () => {
    handleModalDecline();
  };

  const openChangeModal = () => {
    setError("");
    setShowChangeModal(true);
  };

  // Unlike the confirmation popup's AC-17 behaviour, nothing has been
  // committed yet when this modal is open on its own (no pick has been made,
  // or the preview call hasn't resolved) - dismissing it (Escape, backdrop,
  // header close button, or its own Cancel button) is a true no-op cancel,
  // not an implicit save. Ignored entirely while a preview/save is in
  // flight so an in-progress request can't be abandoned mid-air.
  const handleChangeModalHide = () => {
    if (busy) return;
    setShowChangeModal(false);
    setShowAddInput(false);
    setNewSegmentDraft("");
    setError("");
  };

  const retry = () => {
    if (retryHandler) retryHandler();
  };

  // Shared picker UI - the same <Form.Select> (+ "add new segment" input,
  // busy spinner, and inline error/retry) used for both entry points:
  // always-visible inline (still-"Undefined" rows) and inside the "Change
  // Segment" modal (already-classified rows). The preview -> direct-save-or-
  // confirmation-popup logic behind it (handleSelectChange/persist) is
  // identical either way - only where this markup is mounted differs.
  const renderPicker = () => (
    <>
      {showAddInput ? (
        <div className="d-flex gap-1 align-items-center flex-wrap">
          <Form.Control
            size="sm"
            type="text"
            aria-label={`New segment name for transaction ${id}`}
            placeholder="New segment name"
            value={newSegmentDraft}
            onChange={(event) => setNewSegmentDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault();
                handleAddNewConfirm();
              }
            }}
            disabled={busy}
          />
          <Button size="sm" variant="primary" onClick={handleAddNewConfirm} disabled={busy}>
            Add
          </Button>
          <Button size="sm" variant="outline-secondary" onClick={handleAddNewCancel} disabled={busy}>
            Cancel
          </Button>
        </div>
      ) : (
        <Form.Select
          size="sm"
          aria-label={`Segment for transaction ${id}`}
          value={segment}
          onChange={handleSelectChange}
          disabled={disabled}
        >
          {dropdownOptions.map((name) => (
            <option value={name} key={name}>
              {name}
            </option>
          ))}
          <option value={ADD_NEW_SEGMENT_OPTION}>+ Add new segment</option>
        </Form.Select>
      )}
      {busy && (
        <Spinner
          animation="border"
          size="sm"
          className="ms-2"
          role="status"
          aria-label="Saving segment change"
        />
      )}
      {error && (
        <div className="mt-1">
          <Alert variant="danger" className="py-1 px-2 mb-1">
            {error}
          </Alert>
          <Button size="sm" variant="outline-danger" onClick={retry}>
            Retry
          </Button>
        </div>
      )}
    </>
  );

  return (
    <tr className="transaction">
      <td className="transactionDate">{date}</td>
      <td className="transactionAmount">
        {amount < 0 ? `- £${amount * -1}` : `£${amount}`}
      </td>
      <td className="transactionCategory">{category}</td>
      <td className="transactionSegment">
        {isUndefined ? (
          renderPicker()
        ) : (
          <div className="d-flex align-items-center gap-2">
            <span>{segment}</span>
            <Button
              size="sm"
              variant="outline-secondary"
              aria-label={`Change segment for transaction ${id}`}
              onClick={openChangeModal}
            >
              Edit
            </Button>
          </div>
        )}
      </td>
      <td className="transactionPaidTo">{paid_to}</td>
      <td className="transactionMemo">{memo}</td>

      {/* Only rendered for already-classified rows (segment !== "Undefined").
          Deliberately left mounted (not hidden) while the confirmation
          popup below is also open, so react-bootstrap stacks them rather
          than one replacing the other - that keeps this modal's own error/
          retry UI (inside renderPicker()) visible/reachable if the persist
          call the popup triggers fails, instead of it being torn down
          underneath a modal with no error display of its own. Both close
          together on a successful persist() (see setShowChangeModal(false)
          there). */}
      {showChangeModal && (
        <Modal
          show
          onHide={handleChangeModalHide}
          aria-labelledby={`change-segment-modal-title-${id}`}
        >
          <Modal.Header closeButton>
            <Modal.Title id={`change-segment-modal-title-${id}`}>
              Change Segment
            </Modal.Title>
          </Modal.Header>
          <Modal.Body>{renderPicker()}</Modal.Body>
        </Modal>
      )}

      {modal && (
        <Modal
          show
          onHide={handleModalHide}
          aria-labelledby={`segment-update-modal-title-${id}`}
        >
          <Modal.Header closeButton>
            <Modal.Title id={`segment-update-modal-title-${id}`}>
              Update other transactions?
            </Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {modal.count} other transaction{modal.count === 1 ? "" : "s"} from {paid_to} will
            also be updated to &quot;{modal.segmentName}&quot;.
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={handleModalDecline}>
              Decline
            </Button>
            <Button variant="primary" onClick={handleModalConfirm}>
              Confirm
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </tr>
  );
}

export default Transaction;
