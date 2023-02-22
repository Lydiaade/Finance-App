import React, {Component} from "react";
import '../App.css';
import UploadFile from "../component/UploadFile";
import 'bootstrap/dist/css/bootstrap.min.css';

class UploadTransactionPage extends Component {
    state = {}

    componentDidMount() {
    }

    render() {
        return (
            <div>
                <UploadFile/>
            </div>
        );
    }

}

export default UploadTransactionPage;
