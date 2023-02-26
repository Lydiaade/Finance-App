import {Component} from "react";

class AccountOverview extends Component {
    render() {
        const {id, name, sortCode, accountNumber, currentBalance} = this.props.account;
        console.log(name);
        return (
            <div className="account col">
                <h3 className="accountName">{name}</h3>
                <h6 className="accountCurrentBalance">Current Balance: £{currentBalance}</h6>
                <p className="accountDetails">Sort Code: {sortCode} | Account Number: {accountNumber}</p>
                <a href={`/${id}/transactions`}>
                    <button type="button" className="btn btn-primary">View Transactions</button>
                </a>
            </div>
        );
    }
}

export default AccountOverview;