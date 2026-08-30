import { BACKEND_URL } from "../config";
import React, { useEffect, useRef, useState } from "react";
import Container from "react-bootstrap/Container";
import { useNavigate } from "react-router-dom";
import { Alert, Button, Col, Form, FormGroup, Modal, Row } from "react-bootstrap";

const { useParams } = require("react-router-dom");

function EditAccount() {
  const [account, setAccount] = useState({
    id: "",
    name: "",
    sortCode: "",
    accountNumber: "",
    currentBalance: "",
  });
  const initialised = useRef(false);
  const { id } = useParams();
  let navigate = useNavigate();
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const [accountLoaded, setAccountLoaded] = useState(false);
  const [balance, setBalance] = useState("");
  const [balanceDate, setBalanceDate] = useState("");
  const [savingBalance, setSavingBalance] = useState(false);
  const [balanceError, setBalanceError] = useState("");
  const [balanceSuccess, setBalanceSuccess] = useState(false);
  useEffect(() => {
    async function fetchData() {
      // You can await here
      await fetch(`${BACKEND_URL}/accounts/account/${id}`)
        .then((data) => data.json())
        .then((data) => {
          setAccount(data);
          setBalance(
            data.currentBalance !== undefined && data.currentBalance !== null
              ? String(data.currentBalance)
              : ""
          );
          setBalanceDate(data.currentBalanceDate || "");
        })
        .finally(() => setAccountLoaded(true));
    }
    if (!initialised.current) {
      initialised.current = true;
      fetchData();
    }
  });

  function openDeleteConfirm() {
    setDeleteError("");
    setShowDeleteConfirm(true);
  }

  // Closing via Cancel, the header X, a backdrop click, or Escape must all be
  // equivalent no-ops - but not while a delete is in flight, otherwise the
  // dialog can be dismissed mid-request with no way to see the outcome.
  function cancelDelete() {
    if (deleting) return;
    setShowDeleteConfirm(false);
  }

  async function confirmDelete() {
    setDeleting(true);
    setDeleteError("");
    try {
      const response = await fetch(
        `${BACKEND_URL}/accounts/account/${account.id}`,
        { method: "DELETE" }
      );
      if (!response.ok) {
        throw new Error("Couldn't delete this account. Please try again.");
      }
      navigate("/accounts");
    } catch (error) {
      setDeleteError(
        error.message || "Couldn't delete this account. Please try again."
      );
      setDeleting(false);
      setShowDeleteConfirm(false);
    }
  }

  async function saveBalance(event) {
    event.preventDefault();
    if (!balance || !balance.toString().trim()) {
      setBalanceSuccess(false);
      setBalanceError("Balance is required.");
      return;
    }
    setSavingBalance(true);
    setBalanceError("");
    setBalanceSuccess(false);
    try {
      const response = await fetch(
        `${BACKEND_URL}/accounts/account/${account.id}/balance`,
        {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            currentBalance: Number(balance),
            currentBalanceDate: balanceDate,
          }),
        }
      );
      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Couldn't update the balance. Please try again.");
      }
      const updated = await response.json();
      setAccount(updated);
      setBalance(
        updated.currentBalance !== undefined && updated.currentBalance !== null
          ? String(updated.currentBalance)
          : balance
      );
      setBalanceDate(updated.currentBalanceDate || balanceDate);
      setBalanceSuccess(true);
    } catch (error) {
      setBalanceError(
        error.message || "Couldn't update the balance. Please try again."
      );
    } finally {
      setSavingBalance(false);
    }
  }

  return (
    <Container className="edit-account">
      <Container>
        <h1 className="pageTitle">Account Details</h1>
        <Form
        // onSubmit={this.handleSubmit} onReset={this.handleReset}
        >
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formHorizontalAccountName"
          >
            <Form.Label column sm={4}>
              Account Name:
            </Form.Label>
            <Col sm={8}>
              <input
                type="text"
                className="form-control"
                name="accountName"
                value={account.name}
                // onChange={(e) => this.setAccountName(e)}
              />
            </Col>
          </Form.Group>
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formHorizontalMainAccount"
          >
            <Form.Label column sm={4}>
              Is this your main account?
            </Form.Label>
            <Col sm={8}>
              <input
                type="checkbox"
                defaultChecked
                name="Main Account"
                value={account.isMainAccount}
                // onChange={(e) => this.setIsMainAccount(e)}
              />
            </Col>
          </Form.Group>
          <FormGroup className="form-buttons">
            <Button type="submit" className="btn btn-primary">
              Save Changes
            </Button>
          </FormGroup>
        </Form>
      </Container>
      <Container>
        <h2 className="h5">Balance</h2>
        {balanceError && (
          <Alert variant="danger" dismissible onClose={() => setBalanceError("")}>
            {balanceError}
          </Alert>
        )}
        {balanceSuccess && (
          <Alert
            variant="success"
            dismissible
            onClose={() => setBalanceSuccess(false)}
          >
            Balance updated successfully.
          </Alert>
        )}
        <Form onSubmit={saveBalance}>
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formHorizontalBalance"
          >
            <Form.Label column sm={4}>
              Balance:
            </Form.Label>
            <Col sm={8}>
              <Form.Control
                type="number"
                step="0.01"
                name="currentBalance"
                value={balance}
                onChange={(e) => setBalance(e.target.value)}
                required
              />
            </Col>
          </Form.Group>
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formHorizontalBalanceDate"
          >
            <Form.Label column sm={4}>
              Balance Date:
            </Form.Label>
            <Col sm={8}>
              <Form.Control
                type="date"
                name="currentBalanceDate"
                value={balanceDate}
                onChange={(e) => setBalanceDate(e.target.value)}
              />
            </Col>
          </Form.Group>
          <FormGroup className="form-buttons">
            <Button
              type="submit"
              className="btn btn-primary"
              disabled={savingBalance || !accountLoaded}
            >
              {savingBalance ? "Saving..." : "Save Balance"}
            </Button>
          </FormGroup>
        </Form>
      </Container>
      <Container>
        <h6>
          If you delete your account all the transactions and CSV upload
          history associated with it will also be removed and you will not be
          able to retrieve them so please take heed!
        </h6>
        {deleteError && (
          <Alert variant="danger" dismissible onClose={() => setDeleteError("")}>
            {deleteError}
          </Alert>
        )}
        <Button
          className="btn btn-danger"
          onClick={openDeleteConfirm}
          disabled={!accountLoaded}
        >
          Delete Account
        </Button>
      </Container>

      <Modal
        show={showDeleteConfirm}
        onHide={cancelDelete}
        animation={false}
        aria-labelledby="delete-account-modal-title"
      >
        <Modal.Header closeButton>
          <Modal.Title id="delete-account-modal-title">
            Delete this account?
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          This will permanently delete this account, along with all of its
          transactions and CSV upload history. This cannot be undone.
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={cancelDelete} disabled={deleting}>
            Cancel
          </Button>
          <Button variant="danger" onClick={confirmDelete} disabled={deleting}>
            {deleting ? "Deleting..." : "Yes, delete account"}
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
}

export default EditAccount;
