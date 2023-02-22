import React, {Component} from "react";
import '../App.css';
import TransactionContainer from "../component/TransactionContainer";
import {BACKEND_URL} from "../config";
import 'bootstrap/dist/css/bootstrap.min.css';

class TransactionsPage extends Component {
    state = {
        transactionData: [],
    }

    componentDidMount() {
        this.getTransactions();
    }

    getTransactions = () => {
        fetch(`${BACKEND_URL}/transactions/`,)
            .then((data) => data.json())
            .then((data) => this.setState({transactionData: data}));
    }

    render() {
        return (
            <div>
                <React.Fragment>
                    <main className="container-fluid m-2">
                        <div className="row">
                            <div className="itemList">
                                <TransactionContainer
                                    transactions={this.state.transactionData}
                                />
                            </div>
                        </div>
                    </main>
                </React.Fragment>
            </div>
        );
    }

}

export default TransactionsPage;