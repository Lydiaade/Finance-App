import React, {Component} from "react";
import Transaction from "./Transaction";

class TransactionContainer extends Component {
    render() {
        const {transactions} = this.props;
        console.log(`Transactions ${transactions.length}`);
        console.log(transactions);
        return (
            <table className="table container-fluid">
                <thead>
                    <tr className="transaction-header">
                        <th scope="col" className="ID">ID</th>
                        <th scope="col" className="Date">Date</th>
                        <th scope="col" className="Amount">Amount</th>
                        <th scope="col" className="Category">Category</th>
                        <th scope="col" className="PaidTo">Paid To</th>
                        <th scope="col" className="Memo">Memo</th>
                    </tr>
                </thead>
                <tbody>
                {transactions.map((transaction) => (
                    <Transaction
                        key={transactions.indexOf(transaction)}
                        transaction={transaction}
                    />
                ))}
                </tbody>
            </table>
        );
    }
}

export default TransactionContainer;
