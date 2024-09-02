import {Component} from "react";
import {BACKEND_URL} from "../config";
import {Col} from "react-bootstrap";

class AccountOverview extends Component {
    deleteAccount = (id) => {
        fetch(`${BACKEND_URL}/accounts/account/${id}`, {method: "DELETE"})
            .then((data) => console.log(data))

        window.location.reload()
    }

    render() {
        console.log(this.props.account)
        const {id, name, accountType, currentBalance, currentBalanceDate} = this.props.account;
        console.log(name);
        return (
            <div className="account">
                <Col lg={8} className="col">
                    <h5 className="accountName">Account Name: {name.toString().toLocaleUpperCase()}</h5>
                    <h3 className="accountCurrentBalance">Current Balance: £{currentBalance}</h3>
                    <h6>As of: {currentBalanceDate}</h6>
                    <p className="accountType">Account Type: {accountType}</p>
                </Col>
                <Col lg={4}>
                    <a role="button" className="btn btn-primary" href={`/${id}/transactions`}>View Transactions</a>
                    <button type="button" className="btn btn-danger" onClick={() => this.deleteAccount(id)}>Delete
                        Account
                    </button>
                </Col>
            </div>
        );
    }
}

export default AccountOverview;