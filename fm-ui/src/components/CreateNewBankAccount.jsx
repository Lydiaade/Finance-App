import React, {Component} from "react";
import {BACKEND_URL} from "../config";
import './CreateNewBankAccount.css'

class CurrentAccountOverview extends Component {
    state = {
        name: "",
        sortCode: "",
        accountNumber: "",
        currentBalance: "",
        accountType: "",
        accountTypes: [],
        bankNames: [],
    }

    constructor(props) {
        super(props);
        this.state = {
            name: "",
            sortCode: "",
            accountNumber: "",
            currentBalance: "",
            accountType: "",
            accountTypes: [],
            bankNames: [],
        }
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    componentDidMount() {
        this.getAccountTypes();
        this.getAccountBanks();
    }

    handleSubmit(event) {
        // event.preventDefault();
        let payload = {
            "name": this.state.name,
            "sortCode": this.state.sortCode,
            "accountNumber": this.state.accountNumber,
            "currentBalance": parseInt(this.state.currentBalance),
            "accountType": this.state.accountType,
        }
        fetch(`${BACKEND_URL}/accounts/account`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        })
            .then((response) => console.log(response.status));
    }

    getAccountTypes = () => {
        fetch(`${BACKEND_URL}/accounts/types`)
            .then((data) => data.json())
            .then((data) => this.setState({accountTypes: data}));
    }

    getAccountBanks = () => {
        fetch(`${BACKEND_URL}/accounts/banks`)
            .then((data) => data.json())
            .then((data) => this.setState({bankNames: data}));
    }


    render() {
        return (
            <div className="new-account-form container">
                <form onSubmit={this.handleSubmit}>
                    <div className="form-group">
                        <label>Account Name:</label>
                        <input type="text" className="form-control" name="accountName" value={this.state.name}
                               onChange={(e) => this.setState({name: e.target.value})}/>

                    </div>
                    <div className="form-group">
                        <label>Account Type:</label>
                        <select className="form-control" name="Account Type">
                            {this.state.accountTypes.map((type) => (
                                <option key={type}>{type}</option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Sort Code:</label>
                        <input type="text" className="form-control" name="Sort Code" value={this.state.sortCode}
                               onChange={(e) => this.setState({sortCode: e.target.value})}/>
                    </div>
                    <div className="form-group">
                        <label>Account Number:</label>
                        <input type="text" className="form-control" name="Account Number"
                               value={this.state.accountNumber}
                               onChange={(e) => this.setState({accountNumber: e.target.value})}/>
                    </div>
                    <div className="form-group">
                        <label>Bank Name:</label>
                        <select className="form-control" name="Account Bank">
                            {this.state.bankNames.map((type) => (
                                <option key={type}>{type}</option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Currency:</label>
                        <input type="number" className="form-control" name="Current Balance"
                               value={this.state.currentBalance}
                               onChange={(e) => this.setState({currentBalance: e.target.value})}/>
                    </div>
                    <div className="form-group">
                        <label>Balance:</label>
                        <input type="number" className="form-control" name="Current Balance"
                               value={this.state.currentBalance}
                               onChange={(e) => this.setState({currentBalance: e.target.value})}/>
                    </div>
                    <div className="form-group">
                        <label>Balance Date:</label>
                        <input type="number" className="form-control" name="Current Balance"
                               value={this.state.currentBalance}
                               onChange={(e) => this.setState({currentBalance: e.target.value})}/>
                    </div>
                    <div className="form-group">
                        <label>Is this your main account?</label>
                        <input type="checkbox" name="Main Account"
                               value={this.state.currentBalance}
                               onChange={(e) => this.setState({currentBalance: e.target.value})}/>
                    </div>
                    <input type="submit" className="btn btn-primary" value="Submit"/>
                </form>
            </div>
        )
    }
}

export default CurrentAccountOverview;