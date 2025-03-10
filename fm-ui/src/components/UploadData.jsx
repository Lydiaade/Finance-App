import React, { Component } from "react";

class UploadData extends Component {
  render() {
    const { id, fileName, successfulTransactions, uploadedAt } =
      this.props.upload;

    return (
      <tr className="uploadData" id={id}>
        <td className="uploadDataFileName">{fileName}</td>
        <td className="uploadDataDate">{this.convertDateTime(uploadedAt)}</td>
        <td className="uploadDataAccount"></td>
        <td className="uploadDataTransactions">{successfulTransactions}</td>
        <td className="uploadDataActions">
          <button className="btn btn-primary">View</button>
          <button className="btn btn-danger">Delete</button>
        </td>
      </tr>
    );
  }

  convertDateTime = (date) => {
    return `${new Date(date).toLocaleDateString()} ${new Date(
      date
    ).toLocaleTimeString()}`;
  };
}

export default UploadData;
