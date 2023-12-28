import {Component} from "react";
import {BACKEND_URL} from "../config";

class AccountOverview extends Component {
    deleteAccount = (id) => {
        fetch(`${BACKEND_URL}/accounts/account/${id}`, {method: 'DELETE'})
            .then((data) => console.log(data))

        window.location.reload()
    }

    render() {
        const {id, name, sortCode, accountNumber, currentBalance, currentBalanceDate} = this.props.account;
        console.log(name);
        return (
            <div className="account col">
                <h3 className="accountName">{name}</h3>
                <h5 className="accountCurrentBalance">Current Balance: £{currentBalance}</h5>
                <h6>As of date: {currentBalanceDate}</h6>
                <p className="accountDetails">Sort Code: {sortCode} | Account Number: {accountNumber}</p>
                <a role="button" className="btn btn-primary" href={`/${id}/transactions`}>View Transactions</a>
                <button type="button" className="btn btn-danger" onClick={() => this.deleteAccount(id)}>Delete Account
                </button>
            </div>
        );
    }
}

export default AccountOverview;