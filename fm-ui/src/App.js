import React, {Component} from "react";
import './App.css';
import NavigationBar from "./component/Navbar";
import 'bootstrap/dist/css/bootstrap.min.css';
import {Route, Routes} from "react-router-dom";
import HomePage from "./pages/Home";
import TransactionsPage from "./pages/Transactions";
import UploadTransactionPage from "./pages/UploadTransactions";

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
                            <Route path="/transactions" element={<TransactionsPage/>}/>
                            <Route path="/uploadTransactions" element={<UploadTransactionPage/>}/>
                        </Routes>
                    </div>
                </React.Fragment>
            </div>
        );
    }

}

export default App;
