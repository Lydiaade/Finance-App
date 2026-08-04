import React, { useEffect, useState } from "react";
import { BACKEND_URL } from "../config";
import Transaction from "./Transaction";
import { mergeSegmentByName } from "../helpers/segments";

const TransactionTable = ({ items }) => {
  // FM-19: local mirror of `items` so a successful inline segment edit can
  // update the visible list immediately (AC-18) without requiring the
  // parent (which owns pagination/fetching) to know anything about segment
  // editing. Stays in sync whenever the parent re-fetches/paginates.
  const [rows, setRows] = useState(items);
  const [segments, setSegments] = useState([]);

  useEffect(() => {
    setRows(items);
  }, [items]);

  useEffect(() => {
    let ignore = false;
    fetch(`${BACKEND_URL}/segments`)
      .then((response) => response.json())
      .then((data) => {
        if (!ignore) setSegments(data);
      })
      .catch(() => {
        if (!ignore) setSegments([]);
      });
    return () => {
      ignore = true;
    };
  }, []);

  // AC-11: a segment created inline (from any row) becomes immediately
  // selectable everywhere in this table, without a page reload. `name` is
  // the canonical name the backend returned (it may already be known - a
  // no-op merge in that case).
  const handleSegmentAdded = (name) => {
    setSegments((previous) => mergeSegmentByName(previous, name));
  };

  const handleSegmentUpdated = (transactionId, newSegment) => {
    setRows((previous) =>
      previous.map((row) =>
        row.id === transactionId ? { ...row, segment: newSegment } : row
      )
    );
  };

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
          {rows.map((item) => (
            <Transaction
              key={item.id}
              transaction={item}
              segments={segments}
              onSegmentAdded={handleSegmentAdded}
              onSegmentUpdated={handleSegmentUpdated}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default TransactionTable;
