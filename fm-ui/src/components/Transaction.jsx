import React, {Component} from "react";

class Transaction extends Component {
    render() {
        const {date, amount, category, paid_to, memo, segment} = this.props.transaction;
        return (
            <tr className="transaction">
                <td className="transactionDate">{date}</td>
                <td className="transactionAmount">{amount < 0 ? `- £${amount*-1}` : `£${amount}`}</td>
                <td className="transactionCategory">{category}</td>
                <td className="transactionSegment">{segment}</td>
                <td className="transactionPaidTo">{paid_to}</td>
                <td className="transactionMemo">{memo}</td>
            </tr>
        );
    }
}

export default Transaction;
