import {Component} from "react";
import {BACKEND_URL} from "../config";

class CurrentAccountOverview extends Component {
    state = {
        accountInfo: {
            totalOutflow: 6000,
            totalInflow: 5000,
            startingBalance: 3000,
            currentBalance: 1000
        }

    }

    componentDidMount() {
        this.getAccountInfo();
    }

    getAccountInfo = () => {
        fetch(`${BACKEND_URL}/home/account/overview`,)
            .then((data) => data.json())
            .then((data) => this.setState({accountInfo: data}));
    }

    render() {
        return (
            <div className="container">
                <h5>Starting Account Balance: £{this.state.accountInfo.startingBalance}</h5>
                <h5>Total Inflow: £{this.state.accountInfo.totalInflow}</h5>
                <h5>Total Outflow: £{this.state.accountInfo.totalOutflow}</h5>
                <h5>Current Account Balance: £{this.state.accountInfo.currentBalance}</h5>
            </div>
        )
    }
}

export default CurrentAccountOverview;