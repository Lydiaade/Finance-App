import React, { useState, useEffect } from "react";
import { BACKEND_URL } from "../config";
import { Pagination, Form, Button, Row, Col, Spinner } from "react-bootstrap";
import TransactionTable from "./TransactionTable";
import { getTodayIsoDate } from "../helpers/utils";

const TransactionContainer = ({ id }) => {
  const [items, setItems] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const itemsPerPage = 10;

  // FM-52 AC-12: "input" state (what's currently typed/picked in the date
  // fields) is tracked separately from "applied" state (what was last
  // actually sent to the backend). Only Apply moves input -> applied, so
  // typing/picking a date alone never triggers a refetch.
  const [startDateInput, setStartDateInput] = useState("");
  const [endDateInput, setEndDateInput] = useState("");
  const [appliedStartDate, setAppliedStartDate] = useState(null);
  const [appliedEndDate, setAppliedEndDate] = useState(null);
  const [filterError, setFilterError] = useState("");
  const [filterLoading, setFilterLoading] = useState(false);

  // Both are always set/cleared together (see handleApply/handleClear), so
  // checking either is sufficient - kept as a single derived flag so the
  // "filtered vs unfiltered" question is asked in one place.
  const isFiltered = Boolean(appliedStartDate && appliedEndDate);

  const fetchItems = async (page, startDate, endDate) => {
    const filtering = Boolean(startDate && endDate);
    // AC-17: minimal loading indicator specifically for a filtered fetch in
    // flight - the pre-existing unfiltered load has no such indicator and
    // this ticket doesn't add general loading/error handling to it.
    if (filtering) {
      setFilterLoading(true);
    }
    try {
      const params = new URLSearchParams({ page, size: itemsPerPage });
      if (filtering) {
        params.set("startDate", startDate);
        params.set("endDate", endDate);
      }
      const response = await fetch(
        `${BACKEND_URL}/accounts/account/${id}/transactions?${params.toString()}`
      );
      const data = await response.json();
      setItems(data.content);
      setTotalPages(data.totalPages);
    } catch (error) {
      console.error("Error fetching items:", error);
    } finally {
      if (filtering) {
        setFilterLoading(false);
      }
    }
  };

  // AC-11/AC-19: every fresh mount starts from unfiltered state (nothing is
  // persisted), so the first fetch here always runs with no dates. AC-20:
  // paginating while filtered re-fetches with the same applied dates rather
  // than dropping them, since this effect re-runs on any of these changing.
  useEffect(() => {
    fetchItems(currentPage, appliedStartDate, appliedEndDate);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, currentPage, appliedStartDate, appliedEndDate]);

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  // AC-14: mirrors AccountService's backend validation (both required,
  // start-after-end, no future dates) so an invalid range never reaches the
  // network. The backend still enforces the same rules independently - this
  // is a fast-fail UX layer, not a replacement for it.
  const validationErrorFor = (startDate, endDate) => {
    const today = getTodayIsoDate();
    if (startDate > endDate) {
      return "Start date cannot be after end date";
    }
    if (startDate > today || endDate > today) {
      return "Date cannot be in the future";
    }
    return "";
  };

  const handleApply = () => {
    // AC-13: Apply is also disabled via the `disabled` prop below when
    // either field is empty - this is a defensive second check so a
    // request is never sent with only one date set.
    if (!startDateInput || !endDateInput) {
      setFilterError("Both start date and end date are required");
      return;
    }
    const message = validationErrorFor(startDateInput, endDateInput);
    if (message) {
      setFilterError(message);
      return;
    }
    setFilterError("");
    setCurrentPage(0);
    setAppliedStartDate(startDateInput);
    setAppliedEndDate(endDateInput);
  };

  const handleClear = () => {
    setStartDateInput("");
    setEndDateInput("");
    setFilterError("");
    setCurrentPage(0);
    setAppliedStartDate(null);
    setAppliedEndDate(null);
  };

  return (
    <div>
      <Form className="mb-3">
        <Row className="align-items-end g-2">
          <Col xs="auto">
            <Form.Group controlId="transactionFilterStartDate">
              <Form.Label>Start date</Form.Label>
              <Form.Control
                type="date"
                value={startDateInput}
                onChange={(e) => setStartDateInput(e.target.value)}
              />
            </Form.Group>
          </Col>
          <Col xs="auto">
            <Form.Group controlId="transactionFilterEndDate">
              <Form.Label>End date</Form.Label>
              <Form.Control
                type="date"
                value={endDateInput}
                onChange={(e) => setEndDateInput(e.target.value)}
              />
            </Form.Group>
          </Col>
          <Col xs="auto">
            <Button
              variant="primary"
              onClick={handleApply}
              disabled={!startDateInput || !endDateInput}
            >
              Apply
            </Button>
          </Col>
          <Col xs="auto">
            <Button variant="secondary" onClick={handleClear}>
              Clear
            </Button>
          </Col>
        </Row>
        {filterError && (
          <Row className="mt-2">
            <Col xs="auto">
              <div className="text-danger" role="alert">
                {filterError}
              </div>
            </Col>
          </Row>
        )}
      </Form>

      {filterLoading && (
        <div className="mb-2" role="status">
          <Spinner animation="border" size="sm" /> Loading filtered
          transactions...
        </div>
      )}

      {isFiltered && !filterLoading && items.length === 0 ? (
        <p>No transactions in this date range</p>
      ) : (
        <TransactionTable items={items} />
      )}

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
