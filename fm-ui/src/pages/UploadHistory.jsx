import React, { Component } from "react";
import "../App.css";
import "bootstrap/dist/css/bootstrap.min.css";
import { BACKEND_URL } from "../config";
import UploadData from "../components/UploadData";

class UploadHistory extends Component {
  state = {};

  componentDidMount() {
    this.getUploads();
  }

  getUploads = () => {
    fetch(`${BACKEND_URL}/uploads`)
      .then((data) => data.json())
      .then((data) => this.setState({ uploads: data }));
  };

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
            {this.state.uploads &&
              this.state.uploads.map((data) => (
                <UploadData key={data.id} upload={data} />
              ))}
          </tbody>
        </table>
      </div>
    );
  }
}

export default UploadHistory;
