import React, { Component } from "react";
import "../App.css";
import "bootstrap/dist/css/bootstrap.min.css";

class UploadHistory extends Component {
  state = {};

  componentDidMount() {}

  render() {
    return (
      <div className="container">
        <h1 className="pageTitle">Upload History</h1>
        <p>File Name, Upload Date, Bank Account, Number Of Transactions</p>
      </div>
    );
  }
}

export default UploadHistory;
