import {Component} from "react";
import {Col, Row} from "react-bootstrap";
import "./AccountOverview.css"

class AccountOverview extends Component {
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
                    <a role="button" className="btn btn-warning" href={`/${id}/edit`}>Edit Account</a>
                    <a role="button" className="btn btn-primary" href={`/${id}/transactions`}>View Account</a>
                </Col>
            </Row>
        );
    }
}

export default AccountOverview;