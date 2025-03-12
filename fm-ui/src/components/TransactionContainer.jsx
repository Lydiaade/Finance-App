import React, { useState, useEffect } from "react";
import { BACKEND_URL } from "../config";
import Transaction from "./Transaction";
import { Pagination } from "react-bootstrap";
import TransactionTable from "./TransactionTable";

const TransactionContainer = ({ id }) => {
  const [items, setItems] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const itemsPerPage = 10;

  const fetchItems = async (page) => {
    try {
      const response = await fetch(
        `${BACKEND_URL}/accounts/account/${id}/transactions?page=${page}&limit=${itemsPerPage}`
      );
      const data = await response.json();
      setItems(data.content);
      setTotalPages(data.totalPages);
    } catch (error) {
      console.error("Error fetching items:", error);
    }
  };

  useEffect(() => {
    fetchItems(currentPage);
  });

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  return (
    <div>
      <TransactionTable items={items} />
      <PaginationObject
        totalPages={totalPages}
        currentPage={currentPage}
        onPageChange={handlePageChange}
      />
    </div>
  );
};

const PaginationObject = ({ totalPages, currentPage, onPageChange }) => {
  const pageNumbers = Array.from({ length: totalPages }, (_, i) => i);

  return (
    <Pagination className="jusify-content-left">
      <Pagination.Prev
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
      />
      {pageNumbers.map((number) => (
        <Pagination.Item
          key={number}
          onClick={() => onPageChange(number)}
          active={number === currentPage}
        >
          {number + 1}
        </Pagination.Item>
      ))}
      <Pagination.Next
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage + 1 === totalPages}
      />
    </Pagination>
  );
};

export default TransactionContainer;
