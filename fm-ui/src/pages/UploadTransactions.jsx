import React, {Component} from "react";
import '../App.css';
import UploadFile from "../components/UploadFile";
import 'bootstrap/dist/css/bootstrap.min.css';

class UploadTransactionPage extends Component {
    state = {}

    componentDidMount() {
    }

    render() {
        return (
            <div className="container">
                <h1 className="pageTitle">Upload Transaction</h1>
                <UploadFile/>
            </div>
        );
    }

}

export default UploadTransactionPage;
