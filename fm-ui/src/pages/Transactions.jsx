import React, { Component } from "react";
import "../App.css";
import TransactionContainer from "../components/TransactionContainer";
import { BACKEND_URL } from "../config";
import "bootstrap/dist/css/bootstrap.min.css";

class TransactionsPage extends Component {
  state = {
    transactionData: [],
  };

  componentDidMount() {
    this.getTransactions();
  }

  getTransactions = () => {
    fetch(`${BACKEND_URL}/transactions`)
      .then((data) => data.json())
      .then((data) => this.setState({ transactionData: data }));
  };

  render() {
    return (
      <div>
        <React.Fragment>
          <h1 className="pageTitle">All Transactions</h1>
          <main className="container-fluid m-2">
            <div className="itemList">
              <TransactionContainer transactions={this.state.transactionData} />
            </div>
          </main>
        </React.Fragment>
      </div>
    );
  }
}

export default TransactionsPage;
