import React, {Component} from "react";
import './App.css';
import TransactionContainer from "./component/transactionContainer";

const BACKEND_URL = "http://localhost:8080/"

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
        fetch(BACKEND_URL)
            .then((data) => data.text())
            .then((data) => this.setState({message: data}));
    }
    getTransactions = () => {
        fetch(BACKEND_URL + "transactions", {method: "GET"})
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
