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
  useEffect(() => {
    async function fetchData() {
      // You can await here
      await fetch(`${BACKEND_URL}/accounts/account/${id}`)
        .then((data) => data.json())
        .then((data) => {
          setAccount(data);
        });
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

  function cancelDelete() {
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
        <Button className="btn btn-danger" onClick={openDeleteConfirm}>
          Delete Account
        </Button>
      </Container>

      <Modal show={showDeleteConfirm} onHide={cancelDelete} animation={false}>
        <Modal.Header closeButton>
          <Modal.Title>Delete this account?</Modal.Title>
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
