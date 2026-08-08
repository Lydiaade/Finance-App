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

// Segment lookups fired by the nested TransactionTable aren't the concern of
// these tests - always resolve them to an empty list so they don't interfere.
function setupFetchMock(handleTransactions) {
  global.fetch = jest.fn((url) => {
    if (url.endsWith("/segments")) {
      return jsonResponse(200, []);
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
