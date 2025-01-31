import React, { Component } from "react";
import "../App.css";
import "bootstrap/dist/css/bootstrap.min.css";
import AccountOverview from "../components/AccountOverview";
import { BACKEND_URL } from "../config";
import { Col } from "react-bootstrap";

class AccountsPage extends Component {
  state = {
    accounts: [
      {
        name: "Current Account",
        sortCode: "12-34-56",
        accountNumber: "12345678",
        currentBalance: "5000",
      },
    ],
  };

  componentDidMount() {
    this.getAccounts();
  }

  getAccounts = () => {
    fetch(`${BACKEND_URL}/accounts`)
      .then((data) => data.json())
      .then((data) => this.setState({ accounts: data }));
  };

  render() {
    return (
      <div>
        <React.Fragment>
          <header className="App-header">
            <h1 className="pageTitle">Bank Accounts</h1>
            <div className="container accounts">
              {this.state.accounts.length === 0 ? (
                <h5>You currently have no existing accounts</h5>
              ) : (
                <div className="container">
                  {this.state.accounts.map((account) => (
                    <AccountOverview
                      key={this.state.accounts.indexOf(account)}
                      account={account}
                    />
                  ))}
                </div>
              )}
              <a className="hide-link" href={`/account`}>
                <div className="container add-account">
                  <Col>
                    <h3>+</h3>
                  </Col>
                </div>
              </a>
            </div>
          </header>
        </React.Fragment>
      </div>
    );
  }
}

export default AccountsPage;
