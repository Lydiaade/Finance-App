import React, { useState, useEffect } from "react";
import { BACKEND_URL } from "../config";
import { Pagination, Form, Button, Row, Col, Spinner, Alert } from "react-bootstrap";
import TransactionTable from "./TransactionTable";
import { getTodayIsoDate } from "../helpers/utils";

// Synthetic <option> value for the "Undefined" segment when the real segment
// list (GET /segments) doesn't already contain one - "Undefined" is the
// default value Transaction.segment is created with, so it must always be
// selectable here even if no transaction has ever been explicitly
// classified into a real "Undefined" segment row yet (FM-53 AC-16/AC-22).
const UNDEFINED_SEGMENT_VALUE = "Undefined";

const TransactionContainer = ({ id }) => {
  const [items, setItems] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const itemsPerPage = 10;

  // FM-52 AC-12: "input" state (what's currently typed/picked in the date
  // fields) is tracked separately from "applied" state (what was last
  // actually sent to the backend). Only Apply moves input -> applied, so
  // typing/picking a date alone never triggers a refetch. FM-53 extends the
  // same input-vs-applied split to the segment dropdown.
  const [startDateInput, setStartDateInput] = useState("");
  const [endDateInput, setEndDateInput] = useState("");
  const [segmentInput, setSegmentInput] = useState("");
  const [appliedStartDate, setAppliedStartDate] = useState(null);
  const [appliedEndDate, setAppliedEndDate] = useState(null);
  const [appliedSegment, setAppliedSegment] = useState(null);
  const [filterError, setFilterError] = useState("");
  const [filterLoading, setFilterLoading] = useState(false);

  // FM-53: segment options for the filter dropdown, fetched the same way
  // AddTransactionForm fetches GET /segments. segmentsLoadFailed mirrors
  // AddTransactionForm's pattern - AC-27: a failed load must not block the
  // date-only filter path, so it just leaves the dropdown at
  // placeholder/Undefined-only rather than throwing.
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

  // AC-16: don't render a synthetic "Undefined" option if the real segment
  // list already has one - matched case-sensitively (exact string), not
  // case-insensitively, to mirror the backend's exact-match semantics
  // (TransactionSpecifications.hasSegment/AC-6/AC-12). QA/FM-53 gap: the
  // Segments page's plain "add new segment" flow (SegmentController.addSegment
  // -> SegmentService.addSegment) has zero name-dedup, unlike
  // getOrCreateSegment's case-insensitive reuse used elsewhere - so a real
  // Segment row named e.g. "undefined" (different casing) is genuinely
  // reachable today. A case-insensitive check here would then suppress the
  // synthetic option and make the literal "Undefined" default segment
  // permanently unfilterable via this dropdown, even though the backend
  // would happily match it as a distinct value from "undefined". Comparing
  // exactly means only a real segment literally named "Undefined" (which
  // really would render as a visually-identical duplicate <option>)
  // suppresses the synthetic one - any other casing renders as its own,
  // separately selectable and separately filterable option.
  const hasRealUndefinedSegment = segments.some(
    (segment) => segment.name === UNDEFINED_SEGMENT_VALUE
  );

  // Applied segment/dates are always set/cleared together per filter type
  // (see handleApply/handleClear), so checking either date is sufficient -
  // kept as a single derived flag so the "filtered vs unfiltered" question
  // is asked in one place. FM-53: a segment-only filter also counts.
  const isFiltered = Boolean((appliedStartDate && appliedEndDate) || appliedSegment);

  const fetchItems = async (page, startDate, endDate, segment) => {
    // AC-23: a segment-only request must also count as "filtering" so it
    // gets the same loading indicator and plain-text-error-body handling a
    // date-only request already gets.
    const filtering = Boolean((startDate && endDate) || segment);
    // AC-17: minimal loading indicator specifically for a filtered fetch in
    // flight - the pre-existing unfiltered load has no such indicator and
    // this ticket doesn't add general loading/error handling to it.
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
        // Bug fix: AccountController/AccountService return a plain-text body
        // on rejection (e.g. "Date cannot be in the future"), not JSON -
        // calling response.json() on that would throw and get swallowed by
        // the generic catch below, leaving stale pre-filter items on screen
        // looking like a valid filtered result. Only surface this for a
        // filtered request - the unfiltered load has no error UI in scope
        // here, matching the filterLoading gating above.
        if (filtering) {
          let message = "Failed to load filtered transactions. Please try again.";
          try {
            const text = await response.text();
            if (text) {
              message = text;
            }
          } catch (readError) {
            // Body couldn't be read - fall back to the generic message above.
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

  // AC-11/AC-19/AC-26: every fresh mount starts from unfiltered state
  // (nothing is persisted, including the segment selection), so the first
  // fetch here always runs with no dates and no segment. AC-25: paginating
  // while filtered re-fetches with the same applied segment/dates rather
  // than dropping them, since this effect re-runs on any of these changing.
  useEffect(() => {
    fetchItems(currentPage, appliedStartDate, appliedEndDate, appliedSegment);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, currentPage, appliedStartDate, appliedEndDate, appliedSegment]);

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

  const hasSegmentInput = Boolean(segmentInput);
  const hasBothDateInputs = Boolean(startDateInput && endDateInput);
  const hasExactlyOneDateInput = Boolean(startDateInput) !== Boolean(endDateInput);

  // AC-20: Apply used to require both dates before it was even clickable.
  // Now a segment alone is also a valid, completable filter, so Apply is
  // enabled whenever there's a segment selected OR a complete date range -
  // an isolated single date with no segment stays disabled exactly as
  // before.
  const applyDisabled = !hasSegmentInput && !hasBothDateInputs;

  const handleApply = () => {
    // AC-20: a single date filled in without its pair is never a valid
    // request on its own, regardless of whether a segment is also selected -
    // block it with the same inline message FM-52 already used. Both the
    // partial date and the segment selection are left exactly as the user
    // left them (not cleared), so nothing is silently dropped.
    if (hasExactlyOneDateInput) {
      setFilterError("Both start date and end date are required");
      return;
    }
    // Defensive second check mirroring the `disabled` prop below, so a
    // request is never sent with neither a segment nor a complete date
    // range set.
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

  // AC-24: the zero-results message needs to describe whichever combination
  // of filters actually produced it, rather than hardcoding date wording for
  // what might be a segment-only (or combined) empty result.
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
