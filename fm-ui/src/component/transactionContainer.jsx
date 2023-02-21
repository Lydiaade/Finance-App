import React, {Component} from "react";
import Transaction from "./Transaction";

class TransactionContainer extends Component {
    render() {
        const {transactions} = this.props;
        return (
            <div className="row container-fluid">
                {transactions.map((transaction) => (
                    <Transaction
                        key={transactions.indexOf(transaction)}
                        transaction={transaction}
                    />
                ))}
            </div>
        );
    }
}

export default TransactionContainer;
