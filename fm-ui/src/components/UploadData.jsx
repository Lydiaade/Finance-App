import React, { Component } from "react";
import { BACKEND_URL } from "../config";

class UploadData extends Component {
  render() {
    const { id, fileName, successfulTransactions, uploadedAt } =
      this.props.upload;

    return (
      <tr className="uploadData">
        <td className="uploadDataFileName">
          {fileName}
        </td>
        <td className="uploadDataDate">{this.convertDateTime(uploadedAt)}</td>
        <td className="uploadDataAccount"></td>
        <td className="uploadDataTransactions">{successfulTransactions}</td>
        <td className="uploadDataActions">
          <button className="btn btn-primary btn-sm">View</button>
          <button
            className="btn btn-danger btn-sm"
            onClick={this.deleteUpload.bind(this, id)}
          >
            Delete
          </button>
        </td>
      </tr>
    );
  }

  deleteUpload = (id) => {
    fetch(`${BACKEND_URL}/uploads/upload/${id}`, {
      method: "DELETE",
    }).then((data) => console.log(data));
    window.location.reload();
  };

  convertDateTime = (date) => {
    return `${new Date(date).toLocaleDateString()} ${new Date(
      date
    ).toLocaleTimeString()}`;
  };
}

export default UploadData;
