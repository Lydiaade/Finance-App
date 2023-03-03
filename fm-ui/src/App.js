import React, {Component} from "react";
import './App.css';
import NavigationBar from "./components/general/Navbar";
import 'bootstrap/dist/css/bootstrap.min.css';
import {Route, Routes} from "react-router-dom";
import HomePage from "./pages/Home";
import TransactionsPage from "./pages/Transactions";
import UploadTransactionPage from "./pages/UploadTransactions";
import AccountsPage from "./pages/Accounts";
import AccountTransactions from "./pages/AccountTransactions";
import CreateNewAccount from "./components/CreateNewAccount";

class App extends Component {
    state = {}

    componentDidMount() {
    }

    render() {
        return (
            <div>
                <React.Fragment>
                    <NavigationBar/>
                    <div className="container">
                        <Routes>
                            <Route path="/" element={<HomePage/>}/>
                            <Route path="/:id/transactions" element={<AccountTransactions/>}/>
                            <Route path="/transactions" element={<TransactionsPage/>}/>
                            <Route path="/uploadTransactions" element={<UploadTransactionPage/>}/>
                            <Route path="/accounts" element={<AccountsPage/>}/>
                            <Route path="/account" element={<CreateNewAccount/>}/>
                        </Routes>
                    </div>
                </React.Fragment>
            </div>
        );
    }

}

export default App;
