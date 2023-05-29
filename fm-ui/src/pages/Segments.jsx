import React, {Component} from "react";
import '../App.css';
import {BACKEND_URL} from "../config";
import 'bootstrap/dist/css/bootstrap.min.css';
import SegmentContainer from "../components/SegmentContainer";

class TransactionsPage extends Component {
    state = {
        segmentData: [],
    }

    componentDidMount() {
        this.getSegments();
    }

    getSegments = () => {
        fetch(`${BACKEND_URL}/segments/`,)
            .then((data) => data.json())
            .then((data) => this.setState({segmentData: data}));
    }

    render() {
        return (
            <div>
                <React.Fragment>
                    <h1 className="pageTitle">All Segments</h1>
                    <main className="container-fluid m-2">
                        <div className="itemList">
                            <SegmentContainer
                                segments={this.state.segmentData}
                            />
                        </div>
                    </main>
                </React.Fragment>
            </div>
        );
    }

}

export default TransactionsPage;