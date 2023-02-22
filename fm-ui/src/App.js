import React, {Component} from "react";
import './App.css';
import TransactionContainer from "./component/TransactionContainer";
import {BACKEND_URL} from "./config";
import UploadFile from "./component/UploadFile";

class App extends Component {
    state = {
        transactionData: [],
        message: "Finance Manager"
    }

    componentDidMount() {
        this.getHome();
        this.getTransactions();
    }

    getHome = () => {
        fetch(`${BACKEND_URL}/home/`)
            .then((data) => data.text())
            .then((data) => this.setState({message: data}));
    }
    getTransactions = () => {
        fetch(`${BACKEND_URL}/transactions/`, )
            .then((data) => data.json())
            .then((data) => this.setState({transactionData: data}));
    }

    render() {
        return (
            <div>
                <React.Fragment>
                    <div className="Finance Manager App">
                        <header className="App-header">
                            <h1>Finance Manager</h1>
                            <p>{this.state.message}</p>
                        </header>
                    </div>
                    <UploadFile/>
                    <main className="container-fluid m-2">
                        <div className="row">
                            <div className="itemList col-9">
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

export default App;
