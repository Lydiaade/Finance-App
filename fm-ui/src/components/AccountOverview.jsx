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
                    <h6 className="accountName">Account Name: {name.toString().toLocaleUpperCase()}</h6>
                    <h3 className="accountCurrentBalance">Current Balance: £{currentBalance}</h3>
                    <p>As of: {currentBalanceDate}</p>
                    <p className="accountType">Account Type: {accountType}</p>
                </Col>
                <Col lg={2} className="account-buttons">
                    <Row>
                        <button type="button" className="btn btn-warning">Edit
                            Account
                        </button>
                    </Row>
                    <Row>
                        <a role="button" className="btn btn-primary" href={`/${id}/transactions`}>View Account</a>
                    </Row>
                </Col>
            </Row>
        );
    }
}

export default AccountOverview;