import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Alert, Button, Form } from "react-bootstrap";
import { BACKEND_URL } from "../config";
import { formatIsoDateForDisplay, getTodayIsoDate } from "../helpers/utils";

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

  useEffect(() => {
    fetch(`${BACKEND_URL}/segments`)
      .then((response) => response.json())
      .then((data) => setSegments(data))
      .catch(() => setSegments([]));
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

    if (!form.date) {
      errors.date = "Enter a date.";
    } else if (form.date > getTodayIsoDate()) {
      errors.date = "Date cannot be in the future.";
    }

    return errors;
  };

  const buildPayload = () => {
    const magnitude = Math.abs(parseFloat(form.amount));
    const signedAmount = form.direction === "out" ? -magnitude : magnitude;

    return {
      date: form.date,
      accountId: parseInt(form.accountId, 10),
      amount: signedAmount,
      category: form.segment || null,
      paid_to: form.paidTo.trim(),
      memo: form.memo.trim() || null,
    };
  };

  const resetForm = () => {
    setForm(BLANK_FORM);
    setFieldErrors({});
    setSavedTransaction(null);
    setErrorMessage("");
    setView("form");
  };

  const onSubmit = async () => {
    const errors = validate();
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    try {
      const response = await fetch(`${BACKEND_URL}/transactions/transaction`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(buildPayload()),
      });

      if (response.status === 201) {
        const data = await response.json();
        setSavedTransaction(data);
        setView("success");
        return;
      }

      setErrorMessage(
        "We couldn't save this transaction. Please check the details and try again."
      );
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
            Segment: {savedTransaction.category || "None"}
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

      <Form.Group className="mb-3" controlId="formDirection">
        <Form.Label>Money in or out</Form.Label>
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
          <div className="invalid-feedback d-block">
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
        <Form.Select value={form.segment} onChange={updateField("segment")}>
          <option value="">No segment</option>
          {segments.map((segment) => (
            <option value={segment.name} key={segment.id}>
              {segment.name}
            </option>
          ))}
        </Form.Select>
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
