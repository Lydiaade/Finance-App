import React, { Component } from "react";
import { useParams } from "react-router-dom";
import "../App.css";
import "bootstrap/dist/css/bootstrap.min.css";
import { BACKEND_URL } from "../config";
import TransactionTable from "../components/TransactionTable";
import { convertDateTime } from "../helpers/utils";
import { Container } from "react-bootstrap";

class ViewUpload extends Component {
  constructor(props) {
    super(props);
    this.state = {
      data: null,
      isLoading: true,
    };
  }

  componentDidMount() {
    this.getUploadedData();
  }

  getUploadedData = () => {
    fetch(`${BACKEND_URL}/uploads/upload/${this.props.id}`)
      .then((data) => data.json())
      .then((data) => this.setState({ data: data, isLoading: false }));
  };

  render() {
    console.log(this.state.data);
    if (this.state.isLoading) {
      return <p>Loading...</p>;
    } else {
      return (
        <div className="container">
          <h1 className="pageTitle">Upload Details</h1>
          <Container>
            <p>File Name: {this.state.data.fileName}</p>
            <p>Upload Date: {convertDateTime(this.state.data.uploadedAt)}</p>
            <p>
              Successful Transactions: {this.state.data.successfulTransactions}
            </p>
            <p>Failed Transactions: {this.state.data.failedTransactions}</p>
            <p>Bank Account: {this.state.data.bankAccount.name}</p>
          </Container>
          <TransactionTable items={this.state.data.transactions} />
        </div>
      );
    }
  }
}

function withParams(Component) {
  return (props) => {
    const params = useParams(); // Get params from URL
    return <Component {...props} {...params} />;
  };
}

export default withParams(ViewUpload);
