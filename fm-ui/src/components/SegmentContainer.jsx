import React, { useState } from "react";
import { Modal, Button, Form, Alert, Spinner } from "react-bootstrap";
import { BACKEND_URL } from "../config";
import { fetchSegmentUsage, renameSegment, deleteSegmentById } from "../helpers/segments";
import "./SegmentContainer.css";

// FM-19 follow-up: a segment that's actually in use by transactions should
// no longer be a single silent DELETE click. `onSegmentDeleted` and
// `onSegmentRenamed` let the parent (Segments.jsx) own the segment list and
// update it in place - this component never mutates `segments` itself and
// never reloads the page for these two flows.
const SegmentContainer = ({ segments, onSegmentDeleted, onSegmentRenamed }) => {
  const [segmentValue, setSegmentValue] = useState("");

  // Per-row "checking usage" state, keyed by segment id, so only the row
  // that was clicked shows a spinner/disabled state while the usage check
  // is in flight - the rest of the list stays interactive.
  const [checkingId, setCheckingId] = useState(null);
  // Surfaces a failed usage-check (or a failed zero-usage delete) above the
  // table. Deliberately never treated as "0 usages" - see fetchSegmentUsage.
  const [checkError, setCheckError] = useState("");

  // usageModal: null | { id, name, count } - only set once we know from the
  // backend that transactionCount > 0 for the clicked segment.
  const [usageModal, setUsageModal] = useState(null);
  const [renaming, setRenaming] = useState(false);
  const [renameValue, setRenameValue] = useState("");
  const [renameError, setRenameError] = useState("");
  const [modalError, setModalError] = useState("");
  const [modalBusy, setModalBusy] = useState(false);

  // Existing add-segment flow - unchanged/out of scope for this ticket.
  const handleKeyPress = async (e) => {
    if (e.key === "Enter") {
      try {
        fetch(`${BACKEND_URL}/segments/segment`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ name: segmentValue }),
        });
        window.location.reload();
      } catch (error) {
        console.error("Error saving data:", error);
      }
    }
  };

  const closeUsageModal = () => {
    setUsageModal(null);
    setRenaming(false);
    setRenameValue("");
    setRenameError("");
    setModalError("");
    setModalBusy(false);
  };

  // Zero-usage path: no modal, no friction - same immediate delete the
  // segment had before this ticket. The one behavioural fix is that a
  // failure is now surfaced instead of silently doing nothing (or racing a
  // page reload ahead of the request, as the old code did).
  const deleteImmediately = async (segmentId) => {
    try {
      await deleteSegmentById(segmentId);
      onSegmentDeleted(segmentId);
    } catch (error) {
      setCheckError(error.message || "Couldn't delete this segment. Please try again.");
    }
  };

  const handleDeleteClick = async (segment) => {
    setCheckError("");
    setCheckingId(segment.id);
    try {
      const transactionCount = await fetchSegmentUsage(segment.id);
      if (transactionCount === 0) {
        await deleteImmediately(segment.id);
      } else {
        setUsageModal({ id: segment.id, name: segment.name, count: transactionCount });
      }
    } catch (error) {
      // A failed usage check must never be treated as "0 usages" - refuse to
      // delete rather than silently proceeding, since that would defeat the
      // whole point of this safety check.
      setCheckError(
        error.message ||
          "Couldn't check how many transactions use this segment. Nothing was deleted."
      );
    } finally {
      setCheckingId(null);
    }
  };

  const handleLeaveIt = () => {
    closeUsageModal();
  };

  const openRename = () => {
    setRenaming(true);
    setRenameValue(usageModal.name);
    setRenameError("");
    setModalError("");
  };

  const cancelRename = () => {
    setRenaming(false);
    setRenameValue("");
    setRenameError("");
  };

  const handleRenameSubmit = async () => {
    const trimmed = renameValue.trim();
    if (!trimmed) {
      setRenameError("Segment name can't be blank.");
      return;
    }
    setModalBusy(true);
    setRenameError("");
    try {
      const response = await renameSegment(usageModal.id, trimmed);
      onSegmentRenamed(usageModal.id, response.segment.name);
      closeUsageModal();
    } catch (error) {
      // 400 (blank name / collision) is shown inline here rather than
      // closing the modal, so the user can immediately try a different name.
      setRenameError(error.message || "Couldn't rename this segment. Please try again.");
      setModalBusy(false);
    }
  };

  const handleDeleteAnyway = async () => {
    setModalBusy(true);
    setModalError("");
    try {
      await deleteSegmentById(usageModal.id);
      onSegmentDeleted(usageModal.id);
      closeUsageModal();
    } catch (error) {
      setModalError(error.message || "Couldn't delete this segment. Please try again.");
      setModalBusy(false);
    }
  };

  return (
    <div>
      {checkError && (
        <Alert variant="danger" dismissible onClose={() => setCheckError("")}>
          {checkError}
        </Alert>
      )}
      <table className="table container-fluid">
        <thead>
          <tr className="segmentHeader">
            <th scope="col" className="SegmentName">
              Segment Name
            </th>
            <th scope="col" className="SegmentDelete"></th>
          </tr>
        </thead>
        <tbody>
          {!(segments && Array.isArray(segments))
            ? null
            : segments.map((segment) => (
                <tr className="segment" key={segment.id}>
                  <td className="segmentName">{segment.name}</td>
                  <td className="segmentDelete">
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => handleDeleteClick(segment)}
                      disabled={checkingId === segment.id}
                    >
                      {checkingId === segment.id ? (
                        <>
                          <Spinner
                            as="span"
                            animation="border"
                            size="sm"
                            role="status"
                            aria-hidden="true"
                          />
                          <span className="visually-hidden">Checking usage…</span>
                        </>
                      ) : (
                        "Delete"
                      )}
                    </Button>
                  </td>
                </tr>
              ))}
        </tbody>
      </table>
      <div className="form-group">
        <input
          type="text"
          className="form-control"
          id="inputBox"
          placeholder="Add a new segment"
          value={segmentValue}
          onChange={(e) => setSegmentValue(e.target.value)}
          onKeyDown={handleKeyPress}
        />
      </div>

      {usageModal && (
        <Modal
          show
          onHide={() => {
            if (!modalBusy) closeUsageModal();
          }}
          aria-labelledby="segment-usage-modal-title"
        >
          <Modal.Header closeButton>
            <Modal.Title id="segment-usage-modal-title">
              &quot;{usageModal.name}&quot; is in use
            </Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <p>
              {usageModal.count} transaction{usageModal.count === 1 ? "" : "s"} are currently
              using this segment.
            </p>
            {renaming && (
              <Form.Group controlId="renameSegmentInput">
                <Form.Label>New name</Form.Label>
                <Form.Control
                  type="text"
                  value={renameValue}
                  onChange={(e) => setRenameValue(e.target.value)}
                  disabled={modalBusy}
                  autoFocus
                />
              </Form.Group>
            )}
            {renameError && (
              <Alert variant="danger" className="mt-2 mb-0">
                {renameError}
              </Alert>
            )}
            {modalError && (
              <Alert variant="danger" className="mt-2 mb-0">
                {modalError}
              </Alert>
            )}
          </Modal.Body>
          <Modal.Footer>
            {renaming ? (
              <>
                <Button variant="secondary" onClick={cancelRename} disabled={modalBusy}>
                  Cancel
                </Button>
                <Button variant="primary" onClick={handleRenameSubmit} disabled={modalBusy}>
                  {modalBusy ? (
                    <>
                      <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" />{" "}
                      Saving...
                    </>
                  ) : (
                    "Save name"
                  )}
                </Button>
              </>
            ) : (
              <>
                <Button variant="secondary" onClick={handleLeaveIt} disabled={modalBusy}>
                  Leave it
                </Button>
                <Button variant="outline-primary" onClick={openRename} disabled={modalBusy}>
                  Rename it
                </Button>
                <Button variant="danger" onClick={handleDeleteAnyway} disabled={modalBusy}>
                  {modalBusy ? (
                    <>
                      <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" />{" "}
                      Deleting...
                    </>
                  ) : (
                    "Delete anyway"
                  )}
                </Button>
              </>
            )}
          </Modal.Footer>
        </Modal>
      )}
    </div>
  );
};

export default SegmentContainer;
