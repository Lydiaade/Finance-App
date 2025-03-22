import { Component } from "react";
import { BACKEND_URL } from "../config";
import { Button, Form } from "react-bootstrap";

class UploadFile extends Component {
  state = {
    fileName: null,
    bankAccount: "",
    selectedFile: null,
    isSuccess: false,
    message: "",
    errorMessage: "",
    bankAccounts: [],
  };

  componentDidMount() {
    this.getBankAccounts();
  }

  getBankAccounts = () => {
    fetch(`${BACKEND_URL}/accounts`)
      .then((data) => data.json())
      .then((data) => this.setState({ bankAccounts: data }));
  };

  onFileChange = (event) => {
    this.setState({ selectedFile: event.target.files[0] });
  };

  handleBankAccountChange = (event) => {
    this.setState({ bankAccount: event.target.value }, () => {
      console.log("Current value:" + this.state.bankAccount);
    });
  };

  onSubmit = () => {
    const formData = new FormData();
    if (!this.state.selectedFile && !this.state.bankAccount) {
      this.setState({
        errorMessage:
          "Please select a csv file to upload and the correspoinging bank account",
      });
      return;
    }

    formData.append("file", this.state.selectedFile);
    formData.append("bankAccount", this.state.bankAccount);

    fetch(`${BACKEND_URL}/uploads/upload`, {
      method: "POST",
      body: formData,
    })
      .then((response) => {
        if (response.status === 200) {
          this.setState({ isSuccess: response.status === 200 });
          return response.text();
        } else {
          console.log(response.status);
        }
      })
      .then((text) => this.setState({ message: text }))
      .finally(() => {
        this.setState({ fileName: null, bankAccount: "", selectedFile: null });
      });
  };

  render() {
    return (
      <div>
        {this.state.isSuccess ? (
          <div className="alert alert-success" role="alert">
            <h4 className="alert-heading">Successfully Uploaded File!</h4>
            <p>{this.state.message}</p>
          </div>
        ) : (
          <div />
        )}
        <Form>
          <Form.Group className="mb-3" controlId="formBankAccount">
            <Form.Label column sm={4}>
              Bank Account:
            </Form.Label>
            <Form.Select
              value={this.state.bankAccount}
              onChange={this.handleBankAccountChange}
              required
            >
              <option value="">Select Bank Account</option>
              {this.state.bankAccounts.map((account) => (
                <option value={account.id} key={account.id}>
                  {account.name}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
          <Form.Group controlId="formFile" className="mb-3">
            <Form.Label>Upload transactions csv file</Form.Label>
            <Form.Control
              type="file"
              accept=".csv"
              required
              onChange={this.onFileChange}
            />
          </Form.Group>
          <Button variant="primary" type="submit" onClick={this.onSubmit}>
            Submit
          </Button>
        </Form>
      </div>
    );
  }
}

export default UploadFile;
