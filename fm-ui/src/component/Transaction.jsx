import {Component} from "react";

class Transaction extends Component {
    render() {
        const {id, date, amount, category, paid_to, memo} = this.props.transaction;
        console.log(this.props.transaction);
        return (
            <div className="transaction col-7 m-2">
                <p className="transactionID" id={id}>
                    {id}
                </p>
                <p className="transactionDate">{date}</p>
                <p className="transactionAmount">{amount < 0 ? `- £${amount*-1}` : `£${amount}`}</p>
                <p className="transactionCategory">{category}</p>
                <p className="transactionPaidTo">{paid_to}</p>
                <p className="transactionMemo">{memo}</p>
            </div>
        );
    }
}

export default Transaction;
