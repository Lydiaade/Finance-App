import React, {Component} from "react";
import '../App.css';
import {BACKEND_URL} from "../config";
import 'bootstrap/dist/css/bootstrap.min.css';
import CurrentAccountOverview from "../components/CurrentAccountOverview";

class HomePage extends Component {
    state = {
        message: "Finance Manager"
    }

    componentDidMount() {
        this.getHome();
    }

    getHome = () => {
        fetch(`${BACKEND_URL}/home/`)
            .then((data) => data.text())
            .then((data) => this.setState({message: data}));
    }

    render() {
        return (
            <div>
                <React.Fragment>
                    <div className="Finance Manager App">
                        <header className="App-header">
                            <div className="pageTitle">
                                <h1>Finance Manager</h1>
                                <p>{this.state.message}</p>
                            </div>
                            <CurrentAccountOverview />
                        </header>
                    </div>
                </React.Fragment>
            </div>
        );
    }

}

export default HomePage;
