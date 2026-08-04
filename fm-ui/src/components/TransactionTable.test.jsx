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

test("a failed GET /segments surfaces a visible warning, distinct from a genuine empty-segments state", async () => {
  global.fetch = jest.fn((url, options) => {
    if (url.endsWith("/segments") && !options) {
      return Promise.reject(new Error("network down"));
    }
    return jsonResponse(200, []);
  });
  render(<TransactionTable items={items} />);

  await waitFor(() =>
    expect(screen.getByText(/Couldn't load segments/)).toBeInTheDocument()
  );
  // The row's own current value is still shown - the failure doesn't blank
  // out the dropdown, it just means no other options loaded.
  expect(row1Select()).toHaveValue("Undefined");
});

test("creating a new segment inline in one row makes it immediately selectable in a different row (AC-11 cross-row propagation)", async () => {
  setupFetchMock({
    preview: () => jsonResponse(200, { matchingTransactionCount: 0 }),
    update: () =>
      jsonResponse(200, {
        transaction: {
          id: 1,
          date: "2020-01-15",
          account: { id: 1, name: "Current Account" },
          amount: 25.5,
          segment: "Entertainment",
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

  // Row 2 doesn't have "Entertainment" as an option yet.
  expect(within(row2Select()).queryByText("Entertainment")).not.toBeInTheDocument();

  await userEvent.selectOptions(row1Select(), "+ Add new segment");
  await userEvent.type(
    screen.getByLabelText("New segment name for transaction 1"),
    "Entertainment"
  );
  await userEvent.click(screen.getByRole("button", { name: "Add" }));

  await waitFor(() => expect(row1Select()).toHaveValue("Entertainment"));

  // Row 2's own dropdown now lists the segment created via row 1's inline
  // edit, without a page reload - proving the cross-row propagation the
  // table-level `segments` state is built for.
  expect(within(row2Select()).getByText("Entertainment")).toBeInTheDocument();
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
