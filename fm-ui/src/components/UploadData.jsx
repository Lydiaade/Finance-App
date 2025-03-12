import React, { Component } from "react";
import { BACKEND_URL } from "../config";
import { convertDateTime } from "../helpers/utils";

class UploadData extends Component {
  render() {
    const { id, fileName, successfulTransactions, uploadedAt } =
      this.props.upload;

    return (
      <tr className="uploadData">
        <td className="uploadDataFileName">{fileName}</td>
        <td className="uploadDataDate">{convertDateTime(uploadedAt)}</td>
        <td className="uploadDataAccount"></td>
        <td className="uploadDataTransactions">{successfulTransactions}</td>
        <td className="uploadDataActions">
          <a
            role="button"
            className="btn btn-primary btn-sm"
            href={`/uploadHistory/${id}`}
          >
            View
          </a>
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
}

export default UploadData;
