import React, {Component} from "react";
import '../App.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import AccountOverview from "../components/AccountOverview";
import {BACKEND_URL} from "../config";

class AccountsPage extends Component {
    state = {
        accounts: [
            {name: "Current Account", sortCode: "12-34-56", accountNumber: "12345678", currentBalance: "5000"},
        ]
    }

    componentDidMount() {
        this.getAccounts();
    }

    getAccounts = () => {
        fetch(`${BACKEND_URL}/accounts/`)
            .then((data) => data.json())
            .then((data) => this.setState({accounts: data}));
    }

    render() {
        return (
            <div>
                <React.Fragment>
                    <header className="App-header">
                        <h1 className="pageTitle">Existing Accounts</h1>
                        {/*TODO: Add account component*/}
                        <p>INSERT HERE: Add account</p>
                        <span/>
                        {this.state.accounts.length === 0 ? <h5>You currently have no existing accounts</h5> :
                        <div className="container-fluid accounts">
                            {this.state.accounts.map((account) => (
                                <AccountOverview
                                    key={this.state.accounts.indexOf(account)}
                                    account={account}
                                />
                            ))}
                        </div>}
                    </header>
                </React.Fragment>
            </div>
        );
    }

}

export default AccountsPage;
