import {Component} from "react";
import {BACKEND_URL} from "../config";
import {Col, Row} from "react-bootstrap";
import "./AccountOverview.css"

class AccountOverview extends Component {
    deleteAccount = (id) => {
        fetch(`${BACKEND_URL}/accounts/account/${id}`, {method: "DELETE"})
            .then((data) => console.log(data))

        window.location.reload()
    }

    render() {
        const {id, name, accountType, currentBalance, currentBalanceDate} = this.props.account;
        return (
            <Row className="account">
                <Col lg={10} className="col">
                    <h4 className="accountName">Account Name: {name.toString().toLocaleUpperCase()}</h4>
                    <div className="accountMain">
                        <h1 className="accountCurrentBalance">Current Balance: £{currentBalance}</h1>
                        <p>As of: {currentBalanceDate}</p>
                    </div>
                    <h6 className="accountType">Account Type: {accountType}</h6>
                </Col>
                <Col lg={2} className="account-buttons">
                    <button type="button" className="btn btn-warning">Edit
                        Account
                    </button>
                    <a role="button" className="btn btn-primary" href={`/${id}/transactions`}>View Account</a>
                </Col>
            </Row>
        );
    }
}

export default AccountOverview;