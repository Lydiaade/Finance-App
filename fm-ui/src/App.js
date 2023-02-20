import React, {Component} from "react";
import './App.css';

const BACKEND_URL = "http://localhost:8080/"

class App extends Component {
    state = {
        message: "Finance Manager"
    }

    componentDidMount() {
        this.getHome();
    }

    getHome = () => {
        fetch(BACKEND_URL)
            .then((data) => data.json())
            .then((data) => console.log(data))
            .then((data) => this.setState({message: data}));
    };

    render() {
        return (
            <div>
                <React.Fragment>
                    <div className="App">
                        <header className="App-header">
                            <p>
                                Edit <code>src/App.js</code> and save to reload.
                            </p>
                            <a
                                className="App-link"
                                href="https://reactjs.org"
                                target="_blank"
                                rel="noopener noreferrer"
                            >
                                Finance Manager
                            </a>
                            <h1>{this.state.message}</h1>
                        </header>
                    </div>
                </React.Fragment>
            </div>
        );
    }

}

export default App;
