import { Component, useRef } from "react";
import { BACKEND_URL } from "../config";

class UploadFile extends Component {
  state = { fileName: null };

  onFileChange = (event) => {
    this.setState({ selectedFile: event.target.files[0] });
  };

  onFileUpload = () => {
    const formData = new FormData();
    if (!this.state.selectedFile) {
      this.setState({ errorMessage: "Please select a csv file to upload" });
      return;
    }

    formData.append("file", this.state.selectedFile);

    fetch(`${BACKEND_URL}/uploads/upload/csv`, {
      method: "POST",
      headers: {
        Accept: "application/json",
      },
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
      .then((text) => this.setState({ message: text }));

    this.setState({ fileName: null });
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
        <fieldset>
          <div className="mb-3">
            <label htmlFor="formFile" className="form-label">
              Upload transactions csv file
            </label>
            <input
              className="form-control"
              type="file"
              accept=".csv"
              id="formFile"
              onChange={this.onFileChange}
              ref={this.state.fileName}
              required
            />
          </div>
          <button onClick={this.onFileUpload} className="btn btn-primary">
            Upload
          </button>
        </fieldset>
      </div>
    );
  }
}

export default UploadFile;
