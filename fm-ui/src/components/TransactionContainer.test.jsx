import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import TransactionContainer from "./TransactionContainer";
import { getTodayIsoDate } from "../helpers/utils";

function page(content, { totalPages = 1 } = {}) {
  return { content, totalPages };
}

const unfilteredItems = [
  {
    id: 1,
    date: "2020-01-15",
    amount: 25.5,
    category: "Groceries",
    paid_to: "Tesco",
    memo: "",
    segment: "Undefined",
  },
];

function jsonResponse(status, body) {
  return Promise.resolve({
    status,
    ok: status >= 200 && status < 300,
    json: () => Promise.resolve(body),
    text: () =>
      Promise.resolve(typeof body === "string" ? body : JSON.stringify(body ?? {})),
  });
}

// Segment lookups fired by the nested TransactionTable, and now also by
// TransactionContainer's own filter dropdown (FM-53), aren't the concern of
// most of these tests - default to an empty list so they don't interfere.
// FM-53 tests that care about the dropdown's actual contents pass
// `segmentsList`, and AC-27 passes `segmentsError` to simulate a failed load.
function setupFetchMock(handleTransactions, { segmentsList = [], segmentsError = false } = {}) {
  global.fetch = jest.fn((url) => {
    if (url.endsWith("/segments")) {
      if (segmentsError) {
        return Promise.reject(new Error("Network error"));
      }
      return jsonResponse(200, segmentsList);
    }
    return handleTransactions(url);
  });
}

function transactionCalls() {
  return global.fetch.mock.calls.filter(([url]) => url.includes("/transactions?"));
}

function lastTransactionCallUrl() {
  const calls = transactionCalls();
  return new URL(calls[calls.length - 1][0]);
}

// GET /segments resolves independently of the transactions fetch, so reading
// the dropdown's options must wait for it to actually land in state rather
// than assuming it's there as soon as the initial transaction list is.
function segmentOptionLabels(select) {
  return Array.from(select.querySelectorAll("option")).map(
    (option) => option.textContent
  );
}

// GET /segments resolves asynchronously (separately from the transactions
// fetch), so a test-authored segment name isn't guaranteed to exist as an
// <option> the instant the component mounts - wait for it before selecting,
// rather than racing userEvent.selectOptions against the fetch.
async function selectSegment(value) {
  const select = screen.getByLabelText("Segment");
  await waitFor(() =>
    expect(Array.from(select.options).map((option) => option.value)).toContain(value)
  );
  await userEvent.selectOptions(select, value);
}

beforeEach(() => {
  jest.clearAllMocks();
});

test("AC-11: initial load fetches the unfiltered list at page 0, filter not auto-applied", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)));
  render(<TransactionContainer id={1} />);

  await waitFor(() => expect(transactionCalls()).toHaveLength(1));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("page")).toBe("0");
  expect(url.searchParams.get("startDate")).toBeNull();
  expect(url.searchParams.get("endDate")).toBeNull();
});

test("fixes the pagination param bug: sends `size`, not `limit`", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)));
  render(<TransactionContainer id={1} />);

  await waitFor(() => expect(transactionCalls()).toHaveLength(1));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("size")).toBe("10");
  expect(url.searchParams.has("limit")).toBe(false);
});

test("AC-12: typing into the date inputs does not by itself trigger a refetch", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)));
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");

  // Still just the one initial fetch - no request fired from typing alone.
  expect(transactionCalls()).toHaveLength(1);
});

test("AC-13: Apply is disabled unless both start and end date are filled in", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)));
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  const applyButton = screen.getByRole("button", { name: "Apply" });
  expect(applyButton).toBeDisabled();

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  expect(applyButton).toBeDisabled();

  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  expect(applyButton).toBeEnabled();
});

test("AC-14: start-after-end is blocked client-side with an inline error and no network call", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)));
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await userEvent.type(screen.getByLabelText("Start date"), "2020-02-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-01");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  expect(screen.getByRole("alert")).toHaveTextContent(
    "Start date cannot be after end date"
  );
  expect(transactionCalls()).toHaveLength(1);
});

test("AC-5/AC-14: an end date of exactly today is inclusive and valid client-side (not treated as future)", async () => {
  setupFetchMock((url) => jsonResponse(200, page(unfilteredItems)));
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  const today = getTodayIsoDate();
  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), today);
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  // No client-side validation error, and the filtered request actually fires.
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("endDate")).toBe(today);
});

test("AC-14: a future date is blocked client-side with an inline error and no network call", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)));
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  const futureYear = new Date().getFullYear() + 1;
  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), `${futureYear}-01-01`);
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  expect(screen.getByRole("alert")).toHaveTextContent("Date cannot be in the future");
  expect(transactionCalls()).toHaveLength(1);
});

test("AC-15: a successful Apply refetches with startDate/endDate, resets to page 1, and reflects filtered pagination", async () => {
  setupFetchMock((url) => {
    if (url.includes("startDate=")) {
      return jsonResponse(200, page([unfilteredItems[0]], { totalPages: 3 }));
    }
    return jsonResponse(200, page(unfilteredItems, { totalPages: 1 }));
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("startDate")).toBe("2020-01-01");
  expect(url.searchParams.get("endDate")).toBe("2020-01-31");
  expect(url.searchParams.get("page")).toBe("0");

  // Filtered result set reports 3 pages - pagination controls reflect that.
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "3" })).toBeInTheDocument()
  );
});

test("AC-16: Clear resets both inputs, clears the applied filter, and refetches the full unfiltered list at page 1", async () => {
  setupFetchMock((url) => {
    if (url.includes("startDate=")) {
      return jsonResponse(200, page([], { totalPages: 0 }));
    }
    return jsonResponse(200, page(unfilteredItems, { totalPages: 1 }));
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));
  await waitFor(() => expect(transactionCalls()).toHaveLength(2));

  await userEvent.click(screen.getByRole("button", { name: "Clear" }));

  expect(screen.getByLabelText("Start date")).toHaveValue("");
  expect(screen.getByLabelText("End date")).toHaveValue("");
  await waitFor(() => expect(transactionCalls()).toHaveLength(3));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("startDate")).toBeNull();
  expect(url.searchParams.get("endDate")).toBeNull();
  expect(url.searchParams.get("page")).toBe("0");
});

test("AC-17: shows a minimal loading indicator while a filtered fetch is in flight", async () => {
  let resolveFilteredFetch;
  setupFetchMock((url) => {
    if (url.includes("startDate=")) {
      return new Promise((resolve) => {
        resolveFilteredFetch = () => resolve(jsonResponse(200, page(unfilteredItems)));
      });
    }
    return jsonResponse(200, page(unfilteredItems));
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  expect(await screen.findByRole("status")).toHaveTextContent(
    "Loading filtered transactions"
  );

  resolveFilteredFetch();
  await waitFor(() => expect(screen.queryByRole("status")).not.toBeInTheDocument());
});

test("AC-18: a valid range with zero results shows a distinct message, not the loading or pre-filter empty state", async () => {
  setupFetchMock((url) => {
    if (url.includes("startDate=")) {
      return jsonResponse(200, page([], { totalPages: 0 }));
    }
    return jsonResponse(200, page(unfilteredItems));
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() =>
    expect(screen.getByText("No transactions in this date range")).toBeInTheDocument()
  );
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
});

test("bug fix: a server-side rejection of a filtered request (plain-text 400 body) surfaces an error instead of showing stale pre-filter items as the filtered result", async () => {
  setupFetchMock((url) => {
    if (url.includes("startDate=")) {
      // AccountController/AccountService return a plain-text body on
      // rejection, not JSON - this reproduces that, e.g. from clock skew
      // making a client-valid request fail server-side validation.
      return jsonResponse(400, "Date cannot be in the future");
    }
    return jsonResponse(200, page(unfilteredItems));
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));
  expect(await screen.findByText("Tesco")).toBeInTheDocument();

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Date cannot be in the future"
  );
  // The pre-filter row must not remain on screen looking like a valid
  // filtered result, and the loading indicator must have cleared.
  expect(screen.queryByText("Tesco")).not.toBeInTheDocument();
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
});

test("AC-20: paginating while a filter is applied keeps the filter active on subsequent requests", async () => {
  setupFetchMock((url) => {
    if (url.includes("startDate=")) {
      return jsonResponse(200, page(unfilteredItems, { totalPages: 3 }));
    }
    return jsonResponse(200, page(unfilteredItems, { totalPages: 1 }));
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));
  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  // Wait for the filtered response to actually land in state (3 pages)
  // before interacting further, rather than racing the still-pending fetch.
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "3" })).toBeInTheDocument()
  );

  await userEvent.click(screen.getByRole("button", { name: "2" }));

  await waitFor(() => expect(transactionCalls()).toHaveLength(3));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("page")).toBe("1");
  expect(url.searchParams.get("startDate")).toBe("2020-01-01");
  expect(url.searchParams.get("endDate")).toBe("2020-01-31");
});

// FM-53: segment filter -----------------------------------------------------

test("AC-16: segment dropdown offers a placeholder, then real segments, without a duplicate Undefined when one already exists", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)), {
    segmentsList: [
      { id: 1, name: "Groceries" },
      { id: 2, name: "Undefined" },
    ],
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  const select = screen.getByLabelText("Segment");
  await waitFor(() =>
    expect(segmentOptionLabels(select)).toEqual(["All segments", "Groceries", "Undefined"])
  );
});

test("AC-16: adds a synthetic Undefined option when the real segment list doesn't already have one", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)), {
    segmentsList: [{ id: 1, name: "Groceries" }],
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  const select = screen.getByLabelText("Segment");
  await waitFor(() =>
    expect(segmentOptionLabels(select)).toEqual(["All segments", "Undefined", "Groceries"])
  );
});

// QA/FM-53 gap: the Segments page's plain add-segment flow has zero name
// dedup (unlike getOrCreateSegment's case-insensitive reuse elsewhere), so a
// real segment literally named "undefined" (different casing than the
// backend's literal default "Undefined") is a genuinely reachable state, not
// just theoretical. The dedup check must be case-sensitive so this doesn't
// silently make the literal "Undefined" default segment unreachable via the
// dropdown - both must appear as independently selectable, independently
// filterable options.
test("AC-16/AC-6: a real segment differing only in case from 'Undefined' does not suppress the synthetic option - both remain independently selectable", async () => {
  setupFetchMock((url) => jsonResponse(200, page(unfilteredItems)), {
    segmentsList: [{ id: 1, name: "undefined" }],
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  const select = screen.getByLabelText("Segment");
  await waitFor(() =>
    expect(segmentOptionLabels(select)).toEqual(["All segments", "Undefined", "undefined"])
  );

  await selectSegment("Undefined");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));
  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  expect(lastTransactionCallUrl().searchParams.get("segment")).toBe("Undefined");

  await userEvent.click(screen.getByRole("button", { name: "Clear" }));
  await waitFor(() => expect(transactionCalls()).toHaveLength(3));

  await selectSegment("undefined");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));
  await waitFor(() => expect(transactionCalls()).toHaveLength(4));
  expect(lastTransactionCallUrl().searchParams.get("segment")).toBe("undefined");
});

test("AC-17: selecting a segment with no dates entered and applying sends segment only, resets to page 1", async () => {
  setupFetchMock(
    (url) => {
      if (url.includes("segment=")) {
        return jsonResponse(200, page([unfilteredItems[0]], { totalPages: 2 }));
      }
      return jsonResponse(200, page(unfilteredItems, { totalPages: 1 }));
    },
    { segmentsList: [{ id: 1, name: "Groceries" }] }
  );
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await selectSegment("Groceries");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("segment")).toBe("Groceries");
  expect(url.searchParams.get("startDate")).toBeNull();
  expect(url.searchParams.get("endDate")).toBeNull();
  expect(url.searchParams.get("page")).toBe("0");

  await waitFor(() =>
    expect(screen.getByRole("button", { name: "2" })).toBeInTheDocument()
  );
});

test("AC-18: a valid date range with no segment selected does not send a segment param (no regression)", async () => {
  setupFetchMock((url) => jsonResponse(200, page(unfilteredItems)));
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("startDate")).toBe("2020-01-01");
  expect(url.searchParams.get("endDate")).toBe("2020-01-31");
  expect(url.searchParams.has("segment")).toBe(false);
});

test("AC-19: selecting a segment and a valid date range sends all three params together, resets to page 1", async () => {
  setupFetchMock((url) => jsonResponse(200, page(unfilteredItems)), {
    segmentsList: [{ id: 1, name: "Groceries" }],
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await selectSegment("Groceries");
  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("segment")).toBe("Groceries");
  expect(url.searchParams.get("startDate")).toBe("2020-01-01");
  expect(url.searchParams.get("endDate")).toBe("2020-01-31");
  expect(url.searchParams.get("page")).toBe("0");
});

test("AC-20: Apply is enabled when a segment is selected even with both date fields empty", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)), {
    segmentsList: [{ id: 1, name: "Groceries" }],
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  expect(screen.getByRole("button", { name: "Apply" })).toBeDisabled();
  await selectSegment("Groceries");
  expect(screen.getByRole("button", { name: "Apply" })).toBeEnabled();
});

test("AC-20: a single date filled in with no segment selected keeps Apply disabled", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)));
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  expect(screen.getByRole("button", { name: "Apply" })).toBeDisabled();
});

test("AC-20: a single date plus a selected segment is blocked with an inline error, without silently dropping the segment or the partial date", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)), {
    segmentsList: [{ id: 1, name: "Groceries" }],
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await selectSegment("Groceries");
  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  const applyButton = screen.getByRole("button", { name: "Apply" });
  // Enabled because the segment alone would be a valid, completable filter -
  // the partial date is what actually blocks the request on click.
  expect(applyButton).toBeEnabled();
  await userEvent.click(applyButton);

  expect(screen.getByRole("alert")).toHaveTextContent(
    "Both start date and end date are required"
  );
  // No new request fired beyond the initial unfiltered load.
  expect(transactionCalls()).toHaveLength(1);
  // Neither the segment selection nor the partial date was cleared.
  expect(screen.getByLabelText("Segment")).toHaveValue("Groceries");
  expect(screen.getByLabelText("Start date")).toHaveValue("2020-01-01");
});

test("AC-21: Clear resets the segment to the placeholder and clears both dates in one action, refetching the unfiltered list", async () => {
  setupFetchMock(
    (url) => {
      if (url.includes("segment=") || url.includes("startDate=")) {
        return jsonResponse(200, page([], { totalPages: 0 }));
      }
      return jsonResponse(200, page(unfilteredItems, { totalPages: 1 }));
    },
    { segmentsList: [{ id: 1, name: "Groceries" }] }
  );
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await selectSegment("Groceries");
  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));
  await waitFor(() => expect(transactionCalls()).toHaveLength(2));

  await userEvent.click(screen.getByRole("button", { name: "Clear" }));

  expect(screen.getByLabelText("Segment")).toHaveValue("");
  expect(screen.getByLabelText("Start date")).toHaveValue("");
  expect(screen.getByLabelText("End date")).toHaveValue("");
  await waitFor(() => expect(transactionCalls()).toHaveLength(3));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.has("segment")).toBe(false);
  expect(url.searchParams.get("startDate")).toBeNull();
  expect(url.searchParams.get("endDate")).toBeNull();
  expect(url.searchParams.get("page")).toBe("0");
});

test("AC-22: selecting Undefined and applying sends segment=Undefined", async () => {
  setupFetchMock((url) => jsonResponse(200, page(unfilteredItems)));
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await selectSegment("Undefined");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("segment")).toBe("Undefined");
});

test("AC-23: a segment-only request shows the same loading indicator a date-only request gets", async () => {
  let resolveFilteredFetch;
  setupFetchMock(
    (url) => {
      if (url.includes("segment=")) {
        return new Promise((resolve) => {
          resolveFilteredFetch = () => resolve(jsonResponse(200, page(unfilteredItems)));
        });
      }
      return jsonResponse(200, page(unfilteredItems));
    },
    { segmentsList: [{ id: 1, name: "Groceries" }] }
  );
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await selectSegment("Groceries");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  expect(await screen.findByRole("status")).toHaveTextContent(
    "Loading filtered transactions"
  );

  resolveFilteredFetch();
  await waitFor(() => expect(screen.queryByRole("status")).not.toBeInTheDocument());
});

test("AC-23: a segment-only request surfaces a plain-text error body instead of showing stale pre-filter items", async () => {
  setupFetchMock(
    (url) => {
      if (url.includes("segment=")) {
        return jsonResponse(400, "Unexpected error filtering by segment");
      }
      return jsonResponse(200, page(unfilteredItems));
    },
    { segmentsList: [{ id: 1, name: "Groceries" }] }
  );
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));
  expect(await screen.findByText("Tesco")).toBeInTheDocument();

  await selectSegment("Groceries");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Unexpected error filtering by segment"
  );
  expect(screen.queryByText("Tesco")).not.toBeInTheDocument();
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
});

test("AC-24: a segment-only zero-result shows segment-specific wording, not date-specific wording", async () => {
  setupFetchMock(
    (url) => {
      if (url.includes("segment=")) {
        return jsonResponse(200, page([], { totalPages: 0 }));
      }
      return jsonResponse(200, page(unfilteredItems));
    },
    { segmentsList: [{ id: 1, name: "Groceries" }] }
  );
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await selectSegment("Groceries");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() =>
    expect(screen.getByText("No transactions for this segment")).toBeInTheDocument()
  );
  expect(
    screen.queryByText("No transactions in this date range")
  ).not.toBeInTheDocument();
});

test("AC-24: a combined segment + date zero-result shows wording describing both filters", async () => {
  setupFetchMock(
    (url) => {
      if (url.includes("segment=") && url.includes("startDate=")) {
        return jsonResponse(200, page([], { totalPages: 0 }));
      }
      return jsonResponse(200, page(unfilteredItems));
    },
    { segmentsList: [{ id: 1, name: "Groceries" }] }
  );
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await selectSegment("Groceries");
  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() =>
    expect(
      screen.getByText("No transactions match this segment and date range")
    ).toBeInTheDocument()
  );
});

test("AC-25: paginating while a segment filter (with dates) is applied keeps re-fetching with the same segment and dates", async () => {
  setupFetchMock(
    (url) => {
      if (url.includes("segment=") && url.includes("startDate=")) {
        return jsonResponse(200, page(unfilteredItems, { totalPages: 3 }));
      }
      return jsonResponse(200, page(unfilteredItems, { totalPages: 1 }));
    },
    { segmentsList: [{ id: 1, name: "Groceries" }] }
  );
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  await selectSegment("Groceries");
  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));
  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "3" })).toBeInTheDocument()
  );

  await userEvent.click(screen.getByRole("button", { name: "2" }));

  await waitFor(() => expect(transactionCalls()).toHaveLength(3));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("page")).toBe("1");
  expect(url.searchParams.get("segment")).toBe("Groceries");
  expect(url.searchParams.get("startDate")).toBe("2020-01-01");
  expect(url.searchParams.get("endDate")).toBe("2020-01-31");
});

test("AC-26: a fresh mount doesn't persist a previously selected segment", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)), {
    segmentsList: [{ id: 1, name: "Groceries" }],
  });
  const { unmount } = render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));
  await selectSegment("Groceries");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));
  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  unmount();

  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(3));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.has("segment")).toBe(false);
  expect(screen.getByLabelText("Segment")).toHaveValue("");
});

test("AC-27: if GET /segments fails, the dropdown still renders (placeholder + Undefined) and date-only filtering is unaffected", async () => {
  setupFetchMock(() => jsonResponse(200, page(unfilteredItems)), {
    segmentsError: true,
  });
  render(<TransactionContainer id={1} />);
  await waitFor(() => expect(transactionCalls()).toHaveLength(1));

  const select = screen.getByLabelText("Segment");
  expect(segmentOptionLabels(select)).toEqual(["All segments", "Undefined"]);
  // The nested TransactionTable has its own, differently-worded "couldn't
  // load segments" warning for its own failed GET /segments call - scope
  // this to the filter bar's own wording so the two don't collide.
  await waitFor(() =>
    expect(
      screen.getByText(/Couldn't load segments\. You can still filter by date/)
    ).toBeInTheDocument()
  );

  await userEvent.type(screen.getByLabelText("Start date"), "2020-01-01");
  await userEvent.type(screen.getByLabelText("End date"), "2020-01-31");
  await userEvent.click(screen.getByRole("button", { name: "Apply" }));

  await waitFor(() => expect(transactionCalls()).toHaveLength(2));
  const url = lastTransactionCallUrl();
  expect(url.searchParams.get("startDate")).toBe("2020-01-01");
});
