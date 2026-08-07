import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Alert, Button, Form } from "react-bootstrap";
import { BACKEND_URL } from "../config";
import { formatIsoDateForDisplay, getTodayIsoDate } from "../helpers/utils";
import { ADD_NEW_SEGMENT_OPTION, mergeSegmentByName } from "../helpers/segments";

const BLANK_FORM = {
  date: "",
  amount: "",
  accountId: "",
  paidTo: "",
  direction: "",
  segment: "",
  memo: "",
};

function AddTransactionForm({ accounts }) {
  const navigate = useNavigate();
  const [segments, setSegments] = useState([]);
  const [form, setForm] = useState(BLANK_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [view, setView] = useState("form"); // "form" | "success" | "error"
  const [savedTransaction, setSavedTransaction] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [showNewSegmentInput, setShowNewSegmentInput] = useState(false);
  const [newSegmentDraft, setNewSegmentDraft] = useState("");
  // Distinct from "loaded, zero segments" - lets us surface a visible error
  // instead of silently rendering a segment dropdown that looks like a
  // genuine empty-segments state when the GET actually failed.
  const [segmentsLoadFailed, setSegmentsLoadFailed] = useState(false);

  useEffect(() => {
    fetch(`${BACKEND_URL}/segments`)
      .then((response) => response.json())
      .then((data) => {
        setSegments(data);
        setSegmentsLoadFailed(false);
      })
      .catch(() => {
        setSegments([]);
        setSegmentsLoadFailed(true);
      });
  }, []);

  const clearFieldError = (field) => {
    setFieldErrors((previous) => {
      const { [field]: _removed, ...rest } = previous;
      return rest;
    });
  };

  const updateField = (field) => (event) => {
    setForm((previous) => ({ ...previous, [field]: event.target.value }));
    clearFieldError(field);
  };

  const handleDirectionChange = (direction) => {
    setForm((previous) => ({ ...previous, direction }));
    clearFieldError("direction");
  };

  const handleSegmentChange = (event) => {
    const value = event.target.value;
    if (value === ADD_NEW_SEGMENT_OPTION) {
      setShowNewSegmentInput(true);
      setForm((previous) => ({ ...previous, segment: "" }));
      return;
    }
    setShowNewSegmentInput(false);
    setNewSegmentDraft("");
    setForm((previous) => ({ ...previous, segment: value }));
    clearFieldError("segment");
  };

  const validate = () => {
    const errors = {};

    const magnitude = parseFloat(form.amount);
    if (form.amount === "" || Number.isNaN(magnitude)) {
      errors.amount = "Enter an amount.";
    } else if (magnitude <= 0) {
      errors.amount = "Enter an amount greater than 0.";
    }

    if (!form.accountId) {
      errors.accountId = "Select an account.";
    } else if (!accounts.some((account) => String(account.id) === form.accountId)) {
      errors.accountId = "Select a valid account.";
    }

    if (!form.paidTo.trim()) {
      errors.paidTo = "Enter who was paid or who paid you.";
    }

    if (form.direction !== "in" && form.direction !== "out") {
      errors.direction = "Select whether money came in or went out.";
    }

    if (showNewSegmentInput && !newSegmentDraft.trim()) {
      errors.segment =
        "Enter a name for the new segment, or choose an existing one from the list.";
    }

    if (!form.date) {
      errors.date = "Enter a date.";
    } else if (form.date > getTodayIsoDate()) {
      errors.date = "Date cannot be in the future.";
    }

    return errors;
  };

  const buildPayload = (segmentOverride) => {
    const magnitude = Math.abs(parseFloat(form.amount));
    const signedAmount = form.direction === "out" ? -magnitude : magnitude;

    return {
      date: form.date,
      accountId: parseInt(form.accountId, 10),
      amount: signedAmount,
      segment: segmentOverride !== undefined ? segmentOverride : form.segment || null,
      paid_to: form.paidTo.trim(),
      memo: form.memo.trim() || null,
    };
  };

  const resetForm = () => {
    setForm(BLANK_FORM);
    setFieldErrors({});
    setSavedTransaction(null);
    setErrorMessage("");
    setShowNewSegmentInput(false);
    setNewSegmentDraft("");
    setView("form");
  };

  const onSubmit = async () => {
    const errors = validate();
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    // AC-21: a segment typed into the "+ Add new segment" input didn't exist
    // in the dropdown's options at load time. Creation and case-insensitive
    // dedup (AC-11/AC-12) happen server-side inside addManualTransaction
    // (SegmentService.getOrCreateSegment) - submit the typed name as-is
    // rather than pre-creating it via a separate POST /segments/segment
    // call (that endpoint is the older bare add-flow with no dedup).
    const segmentToSubmit = showNewSegmentInput
      ? newSegmentDraft.trim() || null
      : form.segment || null;

    try {
      const response = await fetch(`${BACKEND_URL}/transactions/transaction`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(buildPayload(segmentToSubmit)),
      });

      if (response.status === 201) {
        const data = await response.json();
        setSavedTransaction(data);
        // Makes a brand-new segment immediately selectable if the user adds
        // another transaction in the same session (AC-11), using the
        // canonical name the backend actually saved rather than assuming
        // the typed value was used verbatim.
        if (showNewSegmentInput) {
          setSegments((previous) => mergeSegmentByName(previous, data.segment));
        }
        setView("success");
        return;
      }

      const genericMessage =
        "We couldn't save this transaction. Please check the details and try again.";
      let reason = "";
      try {
        reason = (await response.text()).trim();
      } catch (readError) {
        reason = "";
      }
      setErrorMessage(reason || genericMessage);
      setView("error");
    } catch (error) {
      setErrorMessage(
        "We couldn't reach the server. Please check your connection and try again."
      );
      setView("error");
    }
  };

  if (view === "success" && savedTransaction) {
    const amount = Number(savedTransaction.amount);
    const sign = amount > 0 ? "+" : "";
    const accountName = savedTransaction.account?.name ?? "Unknown account";

    return (
      <div>
        <Alert variant="success">
          <Alert.Heading>Transaction added</Alert.Heading>
          <p className="mb-1">
            Amount: {sign}
            {amount.toFixed(2)}
          </p>
          <p className="mb-1">Account: {accountName}</p>
          <p className="mb-1">Paid to: {savedTransaction.paid_to}</p>
          <p className="mb-1">
            Segment: {savedTransaction.segment || "None"}
          </p>
          <p className="mb-1">
            Date: {formatIsoDateForDisplay(savedTransaction.date)}
          </p>
          {savedTransaction.memo && (
            <p className="mb-0">Memo: {savedTransaction.memo}</p>
          )}
        </Alert>
        <Button variant="primary" className="me-2" onClick={resetForm}>
          Add another transaction
        </Button>
        <Button variant="secondary" onClick={() => navigate("/")}>
          Return to dashboard
        </Button>
      </div>
    );
  }

  if (view === "error") {
    return (
      <div>
        <Alert variant="danger">
          <Alert.Heading>Couldn't save transaction</Alert.Heading>
          <p className="mb-0">{errorMessage}</p>
        </Alert>
        <Button variant="primary" onClick={() => setView("form")}>
          Back to form
        </Button>
      </div>
    );
  }

  return (
    <Form>
      <Form.Group className="mb-3" controlId="formAmount">
        <Form.Label>Amount</Form.Label>
        <Form.Control
          type="number"
          step="0.01"
          value={form.amount}
          onChange={updateField("amount")}
          isInvalid={!!fieldErrors.amount}
        />
        <Form.Control.Feedback type="invalid">
          {fieldErrors.amount}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group
        className="mb-3"
        as="fieldset"
        aria-describedby={
          fieldErrors.direction ? "direction-error" : undefined
        }
      >
        <Form.Label as="legend">Money in or out</Form.Label>
        <div>
          <Form.Check
            inline
            type="radio"
            label="Money in"
            name="direction"
            id="direction-in"
            checked={form.direction === "in"}
            onChange={() => handleDirectionChange("in")}
            isInvalid={!!fieldErrors.direction}
          />
          <Form.Check
            inline
            type="radio"
            label="Money out"
            name="direction"
            id="direction-out"
            checked={form.direction === "out"}
            onChange={() => handleDirectionChange("out")}
            isInvalid={!!fieldErrors.direction}
          />
        </div>
        {fieldErrors.direction && (
          <div className="invalid-feedback d-block" id="direction-error">
            {fieldErrors.direction}
          </div>
        )}
      </Form.Group>

      <Form.Group className="mb-3" controlId="formAccount">
        <Form.Label>Account</Form.Label>
        <Form.Select
          value={form.accountId}
          onChange={updateField("accountId")}
          isInvalid={!!fieldErrors.accountId}
        >
          <option value="">Select an account</option>
          {accounts.map((account) => (
            <option value={account.id} key={account.id}>
              {account.name}
            </option>
          ))}
        </Form.Select>
        <Form.Control.Feedback type="invalid">
          {fieldErrors.accountId}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3" controlId="formPaidTo">
        <Form.Label>Paid to</Form.Label>
        <Form.Control
          type="text"
          value={form.paidTo}
          onChange={updateField("paidTo")}
          isInvalid={!!fieldErrors.paidTo}
        />
        <Form.Control.Feedback type="invalid">
          {fieldErrors.paidTo}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3" controlId="formSegment">
        <Form.Label>Segment</Form.Label>
        {segmentsLoadFailed && (
          <Alert variant="warning" className="py-1 px-2">
            Couldn't load segments. You can still add a transaction without
            selecting one, or type a new segment name below.
          </Alert>
        )}
        <Form.Select
          value={showNewSegmentInput ? ADD_NEW_SEGMENT_OPTION : form.segment}
          onChange={handleSegmentChange}
          isInvalid={!!fieldErrors.segment}
        >
          <option value="">No segment</option>
          {segments.map((segment) => (
            <option value={segment.name} key={segment.id}>
              {segment.name}
            </option>
          ))}
          <option value={ADD_NEW_SEGMENT_OPTION}>+ Add new segment</option>
        </Form.Select>
        {showNewSegmentInput && (
          <Form.Control
            className="mt-2"
            type="text"
            placeholder="New segment name"
            aria-label="New segment name"
            value={newSegmentDraft}
            onChange={(event) => {
              setNewSegmentDraft(event.target.value);
              clearFieldError("segment");
            }}
            isInvalid={!!fieldErrors.segment}
          />
        )}
        <Form.Control.Feedback type="invalid">
          {fieldErrors.segment}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3" controlId="formDate">
        <Form.Label>Date</Form.Label>
        <Form.Control
          type="date"
          value={form.date}
          max={getTodayIsoDate()}
          onChange={updateField("date")}
          isInvalid={!!fieldErrors.date}
        />
        <Form.Control.Feedback type="invalid">
          {fieldErrors.date}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3" controlId="formMemo">
        <Form.Label>Memo</Form.Label>
        <Form.Control
          as="textarea"
          rows={2}
          value={form.memo}
          onChange={updateField("memo")}
        />
      </Form.Group>

      <Button variant="primary" type="button" onClick={onSubmit}>
        Add transaction
      </Button>
    </Form>
  );
}

export default AddTransactionForm;
