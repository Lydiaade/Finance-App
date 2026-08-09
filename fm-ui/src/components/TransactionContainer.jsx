import React, { useState, useEffect } from "react";
import { BACKEND_URL } from "../config";
import { Pagination, Form, Button, Row, Col, Spinner, Alert } from "react-bootstrap";
import TransactionTable from "./TransactionTable";
import { getTodayIsoDate } from "../helpers/utils";

const UNDEFINED_SEGMENT_VALUE = "Undefined";

const TransactionContainer = ({ id }) => {
  const [items, setItems] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const itemsPerPage = 10;

  // "Input" (typed/picked) state is separate from "applied" (last sent to the backend) -
  // only Apply moves input into applied, so typing/picking never triggers a refetch on its own.
  const [startDateInput, setStartDateInput] = useState("");
  const [endDateInput, setEndDateInput] = useState("");
  const [segmentInput, setSegmentInput] = useState("");
  const [appliedStartDate, setAppliedStartDate] = useState(null);
  const [appliedEndDate, setAppliedEndDate] = useState(null);
  const [appliedSegment, setAppliedSegment] = useState(null);
  const [filterError, setFilterError] = useState("");
  const [filterLoading, setFilterLoading] = useState(false);

  const [segments, setSegments] = useState([]);
  const [segmentsLoadFailed, setSegmentsLoadFailed] = useState(false);

  useEffect(() => {
    fetch(`${BACKEND_URL}/segments`)
      .then((response) => response.json())
      .then((data) => {
        setSegments(data);
        setSegmentsLoadFailed(false);
      })
      .catch(() => {
        setSegments([]);
        setSegmentsLoadFailed(true);
      });
  }, []);

  // Exact-match (not case-insensitive) on purpose, to mirror the backend's exact-match
  // filtering - segment names have no dedup on creation, so "undefined" and "Undefined" can
  // both exist as distinct, separately-filterable real segments.
  const hasRealUndefinedSegment = segments.some(
    (segment) => segment.name === UNDEFINED_SEGMENT_VALUE
  );

  const isFiltered = Boolean((appliedStartDate && appliedEndDate) || appliedSegment);

  const fetchItems = async (page, startDate, endDate, segment) => {
    const filtering = Boolean((startDate && endDate) || segment);
    if (filtering) {
      setFilterLoading(true);
    }
    try {
      const params = new URLSearchParams({ page, size: itemsPerPage });
      if (startDate && endDate) {
        params.set("startDate", startDate);
        params.set("endDate", endDate);
      }
      if (segment) {
        params.set("segment", segment);
      }
      const response = await fetch(
        `${BACKEND_URL}/accounts/account/${id}/transactions?${params.toString()}`
      );
      if (!response.ok) {
        // The backend returns a plain-text body on rejection, not JSON - response.json()
        // would throw here and leave stale items on screen looking like a valid result.
        if (filtering) {
          let message = "Failed to load filtered transactions. Please try again.";
          try {
            const text = await response.text();
            if (text) {
              message = text;
            }
          } catch (readError) {
            // ignore - fall back to the generic message above
          }
          setFilterError(message);
          setItems([]);
          setTotalPages(0);
        }
        return;
      }
      const data = await response.json();
      if (filtering) {
        setFilterError("");
      }
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

  useEffect(() => {
    fetchItems(currentPage, appliedStartDate, appliedEndDate, appliedSegment);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, currentPage, appliedStartDate, appliedEndDate, appliedSegment]);

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  // Mirrors the backend's own date validation as a fast-fail UX layer - the backend still
  // enforces these rules independently.
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

  const hasSegmentInput = Boolean(segmentInput);
  const hasBothDateInputs = Boolean(startDateInput && endDateInput);
  const hasExactlyOneDateInput = Boolean(startDateInput) !== Boolean(endDateInput);

  const applyDisabled = !hasSegmentInput && !hasBothDateInputs;

  const handleApply = () => {
    if (hasExactlyOneDateInput) {
      setFilterError("Both start date and end date are required");
      return;
    }
    if (!hasSegmentInput && !hasBothDateInputs) {
      return;
    }
    if (hasBothDateInputs) {
      const message = validationErrorFor(startDateInput, endDateInput);
      if (message) {
        setFilterError(message);
        return;
      }
    }
    setFilterError("");
    setCurrentPage(0);
    setAppliedStartDate(hasBothDateInputs ? startDateInput : null);
    setAppliedEndDate(hasBothDateInputs ? endDateInput : null);
    setAppliedSegment(hasSegmentInput ? segmentInput : null);
  };

  const handleClear = () => {
    setStartDateInput("");
    setEndDateInput("");
    setSegmentInput("");
    setFilterError("");
    setCurrentPage(0);
    setAppliedStartDate(null);
    setAppliedEndDate(null);
    setAppliedSegment(null);
  };

  const emptyResultsMessage = () => {
    const datesApplied = Boolean(appliedStartDate && appliedEndDate);
    const segmentApplied = Boolean(appliedSegment);
    if (datesApplied && segmentApplied) {
      return "No transactions match this segment and date range";
    }
    if (segmentApplied) {
      return "No transactions for this segment";
    }
    return "No transactions in this date range";
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
            <Form.Group controlId="transactionFilterSegment">
              <Form.Label>Segment</Form.Label>
              <Form.Select
                value={segmentInput}
                onChange={(e) => setSegmentInput(e.target.value)}
              >
                <option value="">All segments</option>
                {!hasRealUndefinedSegment && (
                  <option value={UNDEFINED_SEGMENT_VALUE}>Undefined</option>
                )}
                {segments.map((segment) => (
                  <option value={segment.name} key={segment.id}>
                    {segment.name}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
          <Col xs="auto">
            <Button variant="primary" onClick={handleApply} disabled={applyDisabled}>
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
        {segmentsLoadFailed && (
          <Row className="mt-2">
            <Col xs="auto">
              <Alert variant="warning" className="py-1 px-2 mb-0">
                Couldn't load segments. You can still filter by date, or by
                the "Undefined" segment.
              </Alert>
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

      {isFiltered && !filterLoading && !filterError && items.length === 0 ? (
        <p>{emptyResultsMessage()}</p>
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
