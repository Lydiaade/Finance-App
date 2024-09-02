import React, {Component} from "react";
import {BACKEND_URL} from "../config";
import "./CreateNewBankAccount.css"

class CurrentAccountOverview extends Component {
    state = {
        account: {
            name: "",
            sortCode: "",
            accountNumber: "",
            currentBalance: "",
            accountType: "",
            bankName: "",
            currency: "",
            balanceDate: undefined
        },
        accountTypes: [],
        bankNames: [],
        currencies: [],
    }

    constructor(props) {
        super(props);
        this.state = {
            account: {
                name: "",
                sortCode: "",
                accountNumber: "",
                currentBalance: "",
                accountType: "",
                bankName: "",
                isMainAccount: true,
                currency: "",
            },
            accountTypes: [],
            bankNames: [],
            currencies: [],
        }
        this.handleAccountTypeChange = this.handleAccountTypeChange.bind(this);
        this.handleAccountBankChange = this.handleAccountBankChange.bind(this);
        this.handleAccountCurrencyChange = this.handleAccountCurrencyChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleReset = this.handleReset.bind(this);
    }

    componentDidMount() {
        this.getAccountTypes();
        this.getAccountBanks();
        this.getAccountCurrencies();
    }

    handleSubmit(event) {
        let payload = {
            "name": this.state.account.name,
            "accountType": this.state.account.accountType,
            "sortCode": this.state.account.sortCode,
            "accountNumber": this.state.account.accountNumber,
            "accountBank": this.state.account.bankName,
            "currentBalance": parseInt(this.state.account.currentBalance),
            "balanceDate": this.state.account.balanceDate,
            "currency": this.state.account.currency,
            "isMainAccount": this.state.account.isMainAccount,
        }
        console.log(payload);
        fetch(`${BACKEND_URL}/accounts/account`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        })
            .then((response) => console.log(response.status));
        this.handleReset()
    }

    handleReset(event) {
        this.setState({
            account: {
                name: "",
                sortCode: "",
                accountNumber: "",
                currentBalance: "",
                accountType: "",
                bankName: "",
                isMainAccount: true,
                currency: "",
            }
        })
    }

    getAccountTypes = () => {
        fetch(`${BACKEND_URL}/accounts/types`)
            .then((data) => data.json())
            .then((data) => this.setState({accountTypes: data}));
    }

    handleAccountTypeChange(event) {
        this.setState(prevState => ({
            account: {
                ...prevState.account,
                accountType: event.target.value
            }
        }));
    }

    getAccountBanks = () => {
        fetch(`${BACKEND_URL}/accounts/banks`)
            .then((data) => data.json())
            .then((data) => this.setState({bankNames: data}));
    }

    handleAccountBankChange(event) {
        this.setState(prevState => ({
            account: {
                ...prevState.account,
                bankName: event.target.value
            }
        }));
    }

    getAccountCurrencies() {
        fetch(`${BACKEND_URL}/accounts/currencies`)
            .then((data) => data.json())
            .then((data) => this.setState({currencies: data}));
    }

    handleAccountCurrencyChange(event) {
        this.setState(prevState => ({
            account: {
                ...prevState.account,
                currency: event.target.value
            }
        }))
    }

    setAccountName(event) {
        this.setState(prevState => ({
            account: {
                ...prevState.account,
                name: event.target.value
            }
        }))
    }

    setSortCode(event) {
        this.setState(prevState => ({
            account: {
                ...prevState.account,
                sortCode: event.target.value
            }
        }))
    }
    setAccountNumber(event) {
        this.setState(prevState => ({
            account: {
                ...prevState.account,
                accountNumber: event.target.value
            }
        }))
    }

    setCurrentBalance(event) {
        this.setState(prevState => ({
            account: {
                ...prevState.account,
                currentBalance: event.target.value
            }
        }))
    }

    setCurrentBalanceDate(event) {
        this.setState(prevState => ({
            account: {
                ...prevState.account,
                balanceDate: event.target.value
            }
        }))
    }

    setCurrency(event) {
        this.setState(prevState => ({
            account: {
                ...prevState.account,
                currency: event.target.value
            }
        }))
    }

    setIsMainAccount(event) {
        this.setState(prevState => ({
            account: {
                ...prevState.account,
                isMainAccount: !prevState.account.isMainAccount
            }
        }))
    }


    render() {
        return (
            <div className="new-account-form container">
                <h1>Account Details</h1>
                <form onSubmit={this.handleSubmit} onReset={this.handleReset}>
                    <div className="form-group">
                        <label>Account Name:</label>
                        <input type="text" className="form-control" name="accountName" value={this.state.account.name}
                               onChange={(e) => this.setAccountName(e)}/>

                    </div>
                    <div className="form-group">
                        <label>Account Type:</label>
                        <select className="form-control" value={this.state.account.accountType}
                                onChange={this.handleAccountTypeChange}>
                            <option value="">Please select account type</option>
                            {this.state.accountTypes.map((type) => (
                                <option value={type} key={type}>{type}</option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Sort Code:</label>
                        <input type="text" className="form-control" name="Sort Code" value={this.state.account.sortCode}
                               onChange={(e) => this.setSortCode(e)}/>
                    </div>
                    <div className="form-group">
                        <label>Account Number:</label>
                        <input type="text" className="form-control" name="Account Number"
                               value={this.state.account.accountNumber}
                               onChange={(e) => this.setAccountNumber(e)}/>
                    </div>
                    <div className="form-group">
                        <label>Bank Name:</label>
                        <select value={this.state.account.accountBank} onChange={this.handleAccountBankChange}
                                className="form-control">
                            <option value="">Please select account bank name</option>
                            {this.state.bankNames.map((type) => (
                                <option value={type} key={type}>{type}</option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Currency:</label>
                        <select value={this.state.account.currency} onChange={this.handleAccountCurrencyChange}
                                className="form-control">
                            <option value="">Please select account currency</option>
                            {this.state.currencies.map((type) => (
                                <option value={type} key={type}>{type}</option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label>Balance:</label>
                        <input type="number" className="form-control" name="Current Balance"
                               value={this.state.account.currentBalance}
                               onChange={(e) => this.setCurrentBalance(e)}/>
                    </div>
                    <div className="form-group">
                        <label>Balance Date:</label>
                        <input type="date" className="form-control" name="Balance Date:"
                               value={this.state.account.balanceDate}
                               onChange={(e) => this.setCurrentBalanceDate(e)}/>
                    </div>
                    <div className="form-group">
                        <label>Is this your main account?</label>
                        <input type="checkbox" defaultChecked name="Main Account"
                               value={this.state.account.isMainAccount}
                               onChange={(e) => this.setIsMainAccount(e)}/>
                    </div>
                    <div className="form-buttons">
                        <input type="reset" className="btn btn-danger" value="Cancel"/>
                        <input type="submit" className="btn btn-primary" value="Save"/>
                    </div>
                </form>
            </div>
        )
    }
}

export default CurrentAccountOverview;