import {Component} from "react";

class AccountOverview extends Component {
    render() {
        const {name, balance} = this.props.account;
        console.log(name);
        return (
            <div className="account col-6">
                <p className="accountID" id={this.props.key}>
                    {this.props.key}
                </p>
                <h3 className="accountName">{name}</h3>
                <p className="accountBalance">Account Balance: £{balance}</p>
            </div>
        );
    }
}

export default AccountOverview;