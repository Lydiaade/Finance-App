import React from "react";
import Transaction from "./Transaction";

const TransactionTable = ({ items }) => {
  return (
    <div>
      <table className="table container-fluid">
        <thead>
          <tr className="transaction-header">
            <th scope="col" className="Date">
              Date
            </th>
            <th scope="col" className="Amount">
              Amount
            </th>
            <th scope="col" className="Category">
              Category
            </th>
            <th scope="col" className="Segment">
              Segment
            </th>
            <th scope="col" className="PaidTo">
              Paid To
            </th>
            <th scope="col" className="Memo">
              Memo
            </th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <Transaction key={items.indexOf(item)} transaction={item} />
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default TransactionTable;
