import React, { Component } from "react";
import "../App.css";
import "bootstrap/dist/css/bootstrap.min.css";
import UploadData from "../components/UploadData";

class UploadHistory extends Component {
  state = {};

  componentDidMount() {}

  render() {
    return (
      <div className="container">
        <h1 className="pageTitle">Upload History</h1>
        <table className="table container-fluid">
          <thead>
            <tr className="transaction-header">
              <th scope="col" className="Name">
                File Name
              </th>
              <th scope="col" className="Upload Date">
                Upload Date
              </th>
              <th scope="col" className="Bank Account">
                Bank Account
              </th>
              <th scope="col" className="transactions">
                Number Of Transactions
              </th>
              <th scope="col" className="Actions">
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            <UploadData />
          </tbody>
        </table>
      </div>
    );
  }
}

export default UploadHistory;
