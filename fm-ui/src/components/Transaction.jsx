import {Component} from "react";

class Transaction extends Component {
    render() {
        const {id, date, amount, category, paid_to, memo} = this.props.transaction;
        return (
            <tr className="transaction">
                <th scope="row" className="transactionID" id={id}>{id}</th>
                <td className="transactionDate">{date}</td>
                <td className="transactionAmount">{amount < 0 ? `- £${amount*-1}` : `£${amount}`}</td>
                <td className="transactionCategory">{category}</td>
                <td className="transactionPaidTo">{paid_to}</td>
                <td className="transactionMemo">{memo}</td>
            </tr>
        );
    }
}

export default Transaction;
