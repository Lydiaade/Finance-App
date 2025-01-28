import React, { useState, useEffect } from "react";
import { BACKEND_URL } from "../config";
import Transaction from "./Transaction";

// class TransactionContainer extends Component {

//     render() {
//         const {transactions} = this.props;
//         return (
//             <table className="table container-fluid">
//                 <thead>
//                     <tr className="transaction-header">
//                         <th scope="col" className="ID">ID</th>
//                         <th scope="col" className="Date">Date</th>
//                         <th scope="col" className="Amount">Amount</th>
//                         <th scope="col" className="Category">Category</th>
//                         <th scope="col" className="Segment">Segment</th>
//                         <th scope="col" className="PaidTo">Paid To</th>
//                         <th scope="col" className="Memo">Memo</th>
//                     </tr>
//                 </thead>
//                 <tbody>
//                 {transactions.map((transaction) => (
//                     <Transaction
//                         key={transactions.indexOf(transaction)}
//                         transaction={transaction}
//                     />
//                 ))}
//                 </tbody>
//             </table>
//         );
//     }
// }

const TransactionContainer = (data) => {
  const [items, setItems] = useState([]); // Data for the current page
  const [currentPage, setCurrentPage] = useState(1); // Current page
  const [totalPages, setTotalPages] = useState(0); // Total number of pages
  const itemsPerPage = 10; // Fixed limit (can make this dynamic)

  // Fetch items for the current page
  const fetchItems = async (page) => {
    try {
      const response = await fetch(
        `${BACKEND_URL}/accounts/account/${id}/transactions``/api/items?page=${page}&limit=${itemsPerPage}`
      );
      const data = await response.json();
      setItems(data.data); // Data for the current page
      setTotalPages(data.totalPages); // Total pages from API response
    } catch (error) {
      console.error("Error fetching items:", error);
    }
  };

  // Fetch data when the component mounts or the page changes
  useEffect(() => {
    fetchItems(currentPage);
  }, [currentPage]);

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  return (
    <div>
      <ul>
        {items.map((item) => (
          <li key={item.id}>{item.name}</li>
        ))}
      </ul>
      <Pagination
        totalPages={totalPages}
        currentPage={currentPage}
        onPageChange={handlePageChange}
      />
    </div>
  );
};

const Pagination = ({ totalPages, currentPage, onPageChange }) => {
  const pageNumbers = Array.from({ length: totalPages }, (_, i) => i + 1);

  return (
    <div>
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1}
      >
        Previous
      </button>
      {pageNumbers.map((number) => (
        <button
          key={number}
          onClick={() => onPageChange(number)}
          style={{
            margin: "0 5px",
            backgroundColor: number === currentPage ? "blue" : "white",
            color: number === currentPage ? "white" : "black",
          }}
        >
          {number}
        </button>
      ))}
      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages}
      >
        Next
      </button>
    </div>
  );
};

export default TransactionContainer;
