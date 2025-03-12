import { BACKEND_URL } from "../config";
import React, { useEffect, useRef, useState } from "react";
import Container from "react-bootstrap/Container";
import { useNavigate } from "react-router-dom";
import { Button, Col, Form, FormGroup, Row } from "react-bootstrap";

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
  function deleteAccount() {
    fetch(`${BACKEND_URL}/accounts/account/${account.id}`, {
      method: "DELETE",
    }).then((data) => console.log(data));
    let path = `/accounts`;
    navigate(path);
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
          If you delete your account all the transactions uploaded will also be
          removed and you will not be able to retrieve it so please take heed!
        </h6>
        <Button className="btn btn-danger" onClick={deleteAccount}>
          Delete Account
        </Button>
      </Container>
    </Container>
  );
}

export default EditAccount;
