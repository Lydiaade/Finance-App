import React, {Component} from "react";
import {BACKEND_URL} from "../config";
import "./CreateNewBankAccount.css"
import {Button, Col, Form, FormGroup, Row} from "react-bootstrap";

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
        event.preventDefault();
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
                <Form onSubmit={this.handleSubmit} onReset={this.handleReset}>
                    <Form.Group as={Row} className="mb-3" controlId="formHorizontalAccountName">
                        <Form.Label column sm={4}>Account Name:</Form.Label>
                        <Col sm={8}>
                            <input type="text" className="form-control" name="accountName"
                                   value={this.state.account.name}
                                   onChange={(e) => this.setAccountName(e)}/>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} className="mb-3" controlId="formHorizontalAccountType">
                        <Form.Label column sm={4}>Account Type:</Form.Label>
                        <Col sm={8}>
                            <select className="form-control" value={this.state.account.accountType}
                                    onChange={this.handleAccountTypeChange}>
                                <option value="">Please select account type</option>
                                {this.state.accountTypes.map((type) => (
                                    <option value={type} key={type}>{type}</option>
                                ))}
                            </select>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} className="mb-3" controlId="formHorizontalSortCode">
                        <Form.Label column sm={4}>Sort Code:</Form.Label>
                        <Col sm={8}>
                            <input type="text" className="form-control" name="Sort Code"
                                   value={this.state.account.sortCode}
                                   onChange={(e) => this.setSortCode(e)}/>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} className="mb-3" controlId="formHorizontalAccountNumber">
                        <Form.Label column sm={4}>Account Number:</Form.Label>
                        <Col sm={8}>
                            <input type="text" className="form-control" name="Account Number"
                                   value={this.state.account.accountNumber}
                                   onChange={(e) => this.setAccountNumber(e)}/>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} className="mb-3" controlId="formHorizontalBankName">
                        <Form.Label column sm={4}>Bank Name:</Form.Label>
                        <Col sm={8}>
                            <select value={this.state.account.accountBank} onChange={this.handleAccountBankChange}
                                    className="form-control">
                                <option value="">Please select account bank name</option>
                                {this.state.bankNames.map((type) => (
                                    <option value={type} key={type}>{type}</option>
                                ))}
                            </select>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} className="mb-3" controlId="formHorizontalCurrency">
                        <Form.Label column sm={4}>Currency:</Form.Label>
                        <Col sm={8}>
                            <select value={this.state.account.currency} onChange={this.handleAccountCurrencyChange}
                                    className="form-control">
                                <option value="">Please select account currency</option>
                                {this.state.currencies.map((type) => (
                                    <option value={type} key={type}>{type}</option>
                                ))}
                            </select>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} className="mb-3" controlId="formHorizontalBalance">
                        <Form.Label column sm={4}>Balance:</Form.Label>
                        <Col sm={8}>
                            <input type="number" className="form-control" name="Current Balance"
                                   value={this.state.account.currentBalance}
                                   onChange={(e) => this.setCurrentBalance(e)}/>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} className="mb-3" controlId="formHorizontalBalanceDate">
                        <Form.Label column sm={4}>Balance Date:</Form.Label>
                        <Col sm={8}>
                            <input type="date" className="form-control" name="Balance Date:"
                                   value={this.state.account.balanceDate}
                                   onChange={(e) => this.setCurrentBalanceDate(e)}/>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} className="mb-3" controlId="formHorizontalMainAccount">
                        <Form.Label column sm={4}>Is this your main account?</Form.Label>
                        <Col sm={8}>
                            <input type="checkbox" defaultChecked name="Main Account"
                                   value={this.state.account.isMainAccount}
                                   onChange={(e) => this.setIsMainAccount(e)}/>
                        </Col>
                    </Form.Group>
                    <FormGroup className="form-buttons">
                        <Button type="reset" className="btn btn-danger">Cancel</Button>
                        <Button type="submit" className="btn btn-primary">Save</Button>
                    </FormGroup>
                </Form>
            </div>
        )
    }
}

export default CurrentAccountOverview;