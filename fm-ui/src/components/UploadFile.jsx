import { Component } from "react";
import { BACKEND_URL } from "../config";
import { Button, Form } from "react-bootstrap";
import { convertDateTime } from "../helpers/utils";

class UploadFile extends Component {
  state = {
    bankAccount: "",
    selectedFile: null,
    bankAccounts: [],
    alert: null,
    fieldErrors: {},
  };

  componentDidMount() {
    this.getBankAccounts();
  }

  getBankAccounts = () => {
    fetch(`${BACKEND_URL}/accounts`)
      .then((data) => data.json())
      .then((data) => this.setState({ bankAccounts: data }));
  };

  clearFieldError = (field) => {
    const { [field]: _removed, ...rest } = this.state.fieldErrors;
    this.setState({ fieldErrors: rest, alert: null });
  };

  onFileChange = (event) => {
    this.setState({ selectedFile: event.target.files[0] });
    this.clearFieldError("selectedFile");
  };

  handleBankAccountChange = (event) => {
    this.setState({ bankAccount: event.target.value });
    this.clearFieldError("bankAccount");
  };

  buildUploadSummary = (info) => {
    return [
      `${info.fileName} — ${info.bankAccount?.name ?? "Unknown account"}`,
      `Uploaded: ${convertDateTime(info.uploadedAt)}`,
      `Imported: ${info.successfulTransactions}`,
      `Skipped: ${info.failedTransactions}`,
    ].join("\n");
  };

  errorMessageForStatus = (status) => {
    if (status === 422) {
      return "Ensure all transactions match the selected bank account.";
    }
    if (status === 400) {
      return "Ensure all transactions match the required format.";
    }
    return "Upload failed. Please try again.";
  };

  onSubmit = async () => {
    const { selectedFile, bankAccount } = this.state;

    const fieldErrors = {};
    if (!bankAccount) fieldErrors.bankAccount = "Please select a bank account.";
    if (!selectedFile) fieldErrors.selectedFile = "Please select a CSV file.";

    if (Object.keys(fieldErrors).length > 0) {
      this.setState({
        fieldErrors,
        alert: {
          type: "danger",
          title: "Missing details",
          body: Object.values(fieldErrors).join(" "),
        },
      });
      return;
    }

    const formData = new FormData();
    formData.append("file", selectedFile);
    formData.append("bankAccount", bankAccount);

    try {
      const response = await fetch(`${BACKEND_URL}/uploads/upload`, {
        method: "POST",
        body: formData,
      });

      if (response.status === 202) {
        const info = await response.json();
        const partial = info.failedTransactions > 0;
        this.setState({
          alert: {
            type: partial ? "warning" : "success",
            title: partial
              ? "Upload Partially Successful"
              : "Successfully Uploaded File!",
            body: this.buildUploadSummary(info),
          },
          bankAccount: "",
          selectedFile: null,
          fieldErrors: {},
        });
        return;
      }

      this.setState({
        alert: {
          type: "danger",
          title: "Failed to Upload File",
          body: this.errorMessageForStatus(response.status),
        },
      });
    } catch (error) {
      this.setState({
        alert: {
          type: "danger",
          title: "Failed to Upload File",
          body: "An error occurred while uploading. Please try again.",
        },
      });
    }
  };

  render() {
    const { alert, bankAccount, bankAccounts, fieldErrors } = this.state;

    return (
      <div>
        {alert && (
          <div className={`alert alert-${alert.type}`} role="alert">
            <h4 className="alert-heading">{alert.title}</h4>
            <p style={{ whiteSpace: "pre-line", marginBottom: 0 }}>
              {alert.body}
            </p>
          </div>
        )}
        <Form>
          <Form.Group className="mb-3" controlId="formBankAccount">
            <Form.Label column sm={4}>
              Bank Account:
            </Form.Label>
            <Form.Select
              value={bankAccount}
              onChange={this.handleBankAccountChange}
              isInvalid={!!fieldErrors.bankAccount}
              required
            >
              <option value="">Select Bank Account</option>
              {bankAccounts.map((account) => (
                <option value={account.id} key={account.id}>
                  {account.name}
                </option>
              ))}
            </Form.Select>
            <Form.Control.Feedback type="invalid">
              {fieldErrors.bankAccount}
            </Form.Control.Feedback>
          </Form.Group>
          <Form.Group controlId="formFile" className="mb-3">
            <Form.Label>Upload transactions csv file</Form.Label>
            <Form.Control
              type="file"
              accept=".csv"
              required
              onChange={this.onFileChange}
              isInvalid={!!fieldErrors.selectedFile}
            />
            <Form.Control.Feedback type="invalid">
              {fieldErrors.selectedFile}
            </Form.Control.Feedback>
          </Form.Group>
          <Button variant="primary" type="button" onClick={this.onSubmit}>
            Submit
          </Button>
        </Form>
      </div>
    );
  }
}

export default UploadFile;