import React, {Component} from "react";
import {BACKEND_URL} from "../config";
import './CreateNewBankAccount.css'
import Transaction from "./Transaction";

class CurrentAccountOverview extends Component {
    state = {
        name: "",
        sortCode: "",
        accountNumber: "",
        currentBalance: "",
        accountTypes: []
    }

    constructor(props) {
        super(props);
        this.state = {
            name: "",
            sortCode: "",
            accountNumber: "",
            currentBalance: "",
            accountTypes: []
        }
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    componentDidMount() {
        this.getAccountTypes();
    }


    getAccountTypes() {
        fetch(`${BACKEND_URL}/accounts/types`)
            .then((data) => data.json())
            .then((data) => this.setState({accountTypes: data}));
    }


    handleSubmit(event) {
        // event.preventDefault();
        let payload = {
            "name": this.state.name,
            "sortCode": this.state.sortCode,
            "accountNumber": this.state.accountNumber,
            "currentBalance": parseInt(this.state.currentBalance)
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
    render() {
        console.log(this.state.accountTypes)
        // this.getAccountTypes();
        return (
            <div className="new-account-form container">
                <form onSubmit={this.handleSubmit}>
                    <div className="form-group">
                        <label>Account Name:</label>
                        <input type="text" className="form-control" name="accountName" value={this.state.name}
                               onChange={(e) => this.setState({name: e.target.value})}/>

                    </div>
                    <div className="form-group">
                        <label htmlFor="exampleFormControlSelect1">Account Type:</label>
                        <select className="form-control" id="exampleFormControlSelect1">
                            {this.state.accountTypes.map((accountType) => (
                                <option>{accountType}</option>
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
                        <input type="text" className="form-control" name="Account Number"
                               value={this.state.accountNumber}
                               onChange={(e) => this.setState({accountNumber: e.target.value})}/>
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
                        <input type="checkbox" className="form-control" name="Main Account"
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