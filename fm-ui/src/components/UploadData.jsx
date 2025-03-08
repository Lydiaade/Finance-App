import React, { Component } from "react";

class UploadData extends Component {
  render() {
    // const { id, date, amount, category, paid_to, memo, segment } =
    //   this.props.transaction;
    return (
      <tr className="uploadData">
        {/* <th scope="row" className="uploadDataID" id={id}>
          {id}
        </th> */}
        <td className="uploadDataFileName">name</td>
        <td className="uploadDataDate">date</td>
        <td className="uploadDataAccount">account</td>
        <td className="uploadDataTransactions">transactions</td>
        <td className="uploadDataActions">
          <button className="btn btn-primary">View</button>
          <button className="btn btn-danger">Delete</button>
        </td>
      </tr>
    );
  }
}

export default UploadData;
