import React, {Component} from "react";
import '../App.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import AccountOverview from "../components/AccountOverview";

class AccountsPage extends Component {
    state = {
        accounts: [
            {name: "Current Account", balance: 5000},
            {name: "Saving Account", balance: 10000}
        ]
    }

    componentDidMount() {
        // this.getHome();
    }

    // getHome = () => {
    //     fetch(`${BACKEND_URL}/home/`)
    //         .then((data) => data.text())
    //         .then((data) => this.setState({message: data}));
    // }

    render() {
        return (
            <div>
                <React.Fragment>
                    <header className="App-header">
                        <h1 className="pageTitle">Existing Accounts</h1>
                        <span/>
                        <div className="container-fluid accounts">
                            {this.state.accounts.map((account) => (
                                <AccountOverview
                                    key={this.state.accounts.indexOf(account)}
                                    account={account}
                                />
                            ))}
                        </div>
                    </header>
                </React.Fragment>
            </div>
        );
    }

}

export default AccountsPage;
