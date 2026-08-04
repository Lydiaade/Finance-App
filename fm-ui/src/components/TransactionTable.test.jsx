import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import TransactionTable from "./TransactionTable";

const items = [
  {
    id: 1,
    date: "2020-01-15",
    amount: 25.5,
    category: "Groceries",
    paid_to: "Tesco",
    memo: "",
    segment: "Undefined",
  },
  {
    id: 2,
    date: "2020-01-16",
    amount: 10,
    category: "Bills",
    paid_to: "Water Co",
    memo: "",
    segment: "Bills",
  },
];

const segmentList = [
  { id: 10, name: "Groceries" },
  { id: 11, name: "Bills" },
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

function setupFetchMock({ preview, update } = {}) {
  global.fetch = jest.fn((url, options) => {
    if (url.endsWith("/segments") && !options) {
      return jsonResponse(200, segmentList);
    }
    if (url.includes("/segment-preview")) {
      return preview ? preview(url) : jsonResponse(200, { matchingTransactionCount: 0 });
    }
    if (options?.method === "PATCH") {
      return update ? update(options) : jsonResponse(200, {});
    }
    return jsonResponse(200, []);
  });
}

function row1Select() {
  return screen.getByRole("combobox", { name: "Segment for transaction 1" });
}

function row2Select() {
  return screen.getByRole("combobox", { name: "Segment for transaction 2" });
}

test("renders each transaction row with an editable segment dropdown populated from GET /segments", async () => {
  setupFetchMock();
  render(<TransactionTable items={items} />);

  // Options only fully match GET /segments once that fetch resolves - wait
  // for "Groceries" to actually appear in row 1's own options, scoped with
  // `within` so this doesn't pass prematurely just because row 1's current
  // value ("Undefined") is always injected as a fallback option.
  await waitFor(() =>
    expect(within(row1Select()).getByText("Groceries")).toBeInTheDocument()
  );
  expect(row1Select()).toHaveValue("Undefined");
  expect(row2Select()).toHaveValue("Bills");
  expect(within(row1Select()).getByText("Bills")).toBeInTheDocument();
  expect(within(row2Select()).getByText("Groceries")).toBeInTheDocument();
  expect(within(row1Select()).getByText("+ Add new segment")).toBeInTheDocument();
});

test("after a successful inline segment update, the visible list reflects the change without a manual refresh, other rows unaffected (AC-18)", async () => {
  setupFetchMock({
    preview: () => jsonResponse(200, { matchingTransactionCount: 0 }),
    update: () =>
      jsonResponse(200, {
        transaction: {
          id: 1,
          date: "2020-01-15",
          account: { id: 1, name: "Current Account" },
          amount: 25.5,
          segment: "Groceries",
          paid_to: "Tesco",
          memo: "",
        },
        updatedTransactionCount: 0,
      }),
  });
  render(<TransactionTable items={items} />);

  await waitFor(() =>
    expect(within(row1Select()).getByText("Groceries")).toBeInTheDocument()
  );

  await userEvent.selectOptions(row1Select(), "Groceries");

  await waitFor(() => expect(row1Select()).toHaveValue("Groceries"));
  expect(row2Select()).toHaveValue("Bills");
});
