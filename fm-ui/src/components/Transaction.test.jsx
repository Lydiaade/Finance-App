import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import Transaction from "./Transaction";

function jsonResponse(status, body) {
  return Promise.resolve({
    status,
    ok: status >= 200 && status < 300,
    json: () => Promise.resolve(body),
    text: () =>
      Promise.resolve(typeof body === "string" ? body : JSON.stringify(body ?? {})),
  });
}

// Matches UpdateTransactionSegmentResponse's shape: { transaction: {...}, updatedTransactionCount }.
// `segmentName` is the canonical name the backend decided to save (may differ from whatever the
// caller typed, e.g. after case-insensitive reuse) - persist() reads this back rather than
// trusting the request value.
function updateSuccessResponse(segmentName, updatedTransactionCount = 0) {
  return jsonResponse(200, {
    transaction: {
      id: 1,
      date: "2020-01-15",
      account: { id: 1, name: "Current Account" },
      amount: 25.5,
      segment: segmentName,
      paid_to: "Tesco",
      memo: "Weekly shop",
    },
    updatedTransactionCount,
  });
}

const baseTransaction = {
  id: 1,
  date: "2020-01-15",
  amount: 25.5,
  category: "Groceries",
  paid_to: "Tesco",
  memo: "Weekly shop",
  segment: "Undefined",
};

const baseSegments = [
  { id: 10, name: "Groceries" },
  { id: 11, name: "Bills" },
];

function renderTransaction({
  transaction = baseTransaction,
  segments = baseSegments,
  onSegmentAdded = jest.fn(),
  onSegmentUpdated = jest.fn(),
} = {}) {
  render(
    <table>
      <tbody>
        <Transaction
          transaction={transaction}
          segments={segments}
          onSegmentAdded={onSegmentAdded}
          onSegmentUpdated={onSegmentUpdated}
        />
      </tbody>
    </table>
  );
  return { onSegmentAdded, onSegmentUpdated };
}

// Routes fetch calls by URL/method so tests don't depend on call order. There is no
// "createSegment" branch here on purpose - segment creation/dedup happens server-side inside the
// PATCH .../segment call itself (SegmentService.getOrCreateSegment), so Transaction.jsx should
// never call POST /segments/segment directly. `segmentCreationCall` lets a test assert that.
function setupFetchMock({ preview, update, onSegmentCreationCall } = {}) {
  global.fetch = jest.fn((url, options) => {
    if (url.includes("/segment-preview")) {
      return preview ? preview(url) : jsonResponse(200, { matchingTransactionCount: 0 });
    }
    if (options?.method === "PATCH") {
      return update ? update(options) : updateSuccessResponse("Groceries");
    }
    if (url.endsWith("/segments/segment") && options?.method === "POST") {
      onSegmentCreationCall?.(options);
      return jsonResponse(201, {});
    }
    return jsonResponse(200, []);
  });
}

function segmentSelect() {
  return screen.getByRole("combobox", { name: "Segment for transaction 1" });
}

test("renders the current segment pre-selected and lists options from segments plus add-new", () => {
  renderTransaction();

  expect(segmentSelect()).toHaveValue("Undefined");
  expect(screen.getByRole("option", { name: "Groceries" })).toBeInTheDocument();
  expect(screen.getByRole("option", { name: "Bills" })).toBeInTheDocument();
  expect(screen.getByRole("option", { name: "Undefined" })).toBeInTheDocument();
  expect(screen.getByRole("option", { name: "+ Add new segment" })).toBeInTheDocument();
});

test("selecting an existing segment with 0 other matches saves directly, no popup shown", async () => {
  let updateBody = null;
  setupFetchMock({
    preview: () => jsonResponse(200, { matchingTransactionCount: 0 }),
    update: (options) => {
      updateBody = JSON.parse(options.body);
      return updateSuccessResponse("Groceries");
    },
  });
  const { onSegmentUpdated } = renderTransaction();

  await userEvent.selectOptions(segmentSelect(), "Groceries");

  await waitFor(() => expect(onSegmentUpdated).toHaveBeenCalledWith(1, "Groceries"));
  expect(updateBody).toEqual({ segment: "Groceries", applyToExisting: false });
  expect(screen.queryByText(/will also be updated/)).not.toBeInTheDocument();
});

test("selecting an existing segment with >0 other matches shows the popup with the real fetched count", async () => {
  setupFetchMock({
    preview: () => jsonResponse(200, { matchingTransactionCount: 12 }),
  });
  renderTransaction();

  await userEvent.selectOptions(segmentSelect(), "Groceries");

  await waitFor(() =>
    expect(
      screen.getByText(
        '12 other transactions from Tesco will also be updated to "Groceries".'
      )
    ).toBeInTheDocument()
  );
  expect(screen.getByRole("button", { name: "Confirm" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Decline" })).toBeInTheDocument();
});

test("confirming the popup calls update with applyToExisting true and updates the visible row", async () => {
  let updateBody = null;
  setupFetchMock({
    preview: () => jsonResponse(200, { matchingTransactionCount: 3 }),
    update: (options) => {
      updateBody = JSON.parse(options.body);
      return updateSuccessResponse("Groceries", 3);
    },
  });
  const { onSegmentUpdated } = renderTransaction();

  await userEvent.selectOptions(segmentSelect(), "Groceries");
  await waitFor(() => screen.getByRole("button", { name: "Confirm" }));
  await userEvent.click(screen.getByRole("button", { name: "Confirm" }));

  await waitFor(() => expect(onSegmentUpdated).toHaveBeenCalledWith(1, "Groceries"));
  expect(updateBody).toEqual({ segment: "Groceries", applyToExisting: true });
});

test("declining the popup calls update with applyToExisting false and still saves the edited row's own change", async () => {
  let updateBody = null;
  setupFetchMock({
    preview: () => jsonResponse(200, { matchingTransactionCount: 3 }),
    update: (options) => {
      updateBody = JSON.parse(options.body);
      return updateSuccessResponse("Groceries", 0);
    },
  });
  const { onSegmentUpdated } = renderTransaction();

  await userEvent.selectOptions(segmentSelect(), "Groceries");
  await waitFor(() => screen.getByRole("button", { name: "Decline" }));
  await userEvent.click(screen.getByRole("button", { name: "Decline" }));

  await waitFor(() => expect(onSegmentUpdated).toHaveBeenCalledWith(1, "Groceries"));
  expect(updateBody).toEqual({ segment: "Groceries", applyToExisting: false });
});

test("dismissing the popup without an explicit click (Escape) is treated as decline (AC-17)", async () => {
  let updateBody = null;
  setupFetchMock({
    preview: () => jsonResponse(200, { matchingTransactionCount: 3 }),
    update: (options) => {
      updateBody = JSON.parse(options.body);
      return updateSuccessResponse("Groceries", 0);
    },
  });
  renderTransaction();

  await userEvent.selectOptions(segmentSelect(), "Groceries");
  await waitFor(() => screen.getByRole("button", { name: "Confirm" }));

  // react-bootstrap's Modal listens for Escape via a document keydown
  // listener that checks `keyCode` (legacy API), not the modern `key` field.
  fireEvent.keyDown(document, { key: "Escape", code: "Escape", keyCode: 27, which: 27 });

  await waitFor(() =>
    expect(updateBody).toEqual({ segment: "Groceries", applyToExisting: false })
  );
});

test("typing a brand-new segment name saves it via the update call (no separate segment-creation call) and it's usable immediately", async () => {
  let updateBody = null;
  let segmentCreationCalled = false;
  setupFetchMock({
    onSegmentCreationCall: () => {
      segmentCreationCalled = true;
    },
    preview: () => jsonResponse(200, { matchingTransactionCount: 0 }),
    update: (options) => {
      updateBody = JSON.parse(options.body);
      return updateSuccessResponse("Entertainment");
    },
  });
  const { onSegmentAdded, onSegmentUpdated } = renderTransaction();

  await userEvent.selectOptions(segmentSelect(), "+ Add new segment");
  await userEvent.type(
    screen.getByLabelText("New segment name for transaction 1"),
    "Entertainment"
  );
  await userEvent.click(screen.getByRole("button", { name: "Add" }));

  await waitFor(() => expect(onSegmentUpdated).toHaveBeenCalledWith(1, "Entertainment"));
  expect(updateBody).toEqual({ segment: "Entertainment", applyToExisting: false });
  expect(segmentCreationCalled).toBe(false);
  // The canonical name from the response is merged into the local segment
  // list so it's immediately selectable elsewhere, without a page reload.
  expect(onSegmentAdded).toHaveBeenCalledWith("Entertainment");
});

test("typing an existing segment name in a different case reuses it (server-side dedup) instead of creating a duplicate", async () => {
  let updateBody = null;
  let segmentCreationCalled = false;
  setupFetchMock({
    onSegmentCreationCall: () => {
      segmentCreationCalled = true;
    },
    preview: () => jsonResponse(200, { matchingTransactionCount: 0 }),
    update: (options) => {
      updateBody = JSON.parse(options.body);
      // Simulates the backend's case-insensitive reuse of the existing
      // "Groceries" row rather than the raw "groceries" the user typed.
      return updateSuccessResponse("Groceries");
    },
  });
  const { onSegmentUpdated } = renderTransaction();

  await userEvent.selectOptions(segmentSelect(), "+ Add new segment");
  await userEvent.type(
    screen.getByLabelText("New segment name for transaction 1"),
    "groceries"
  );
  await userEvent.click(screen.getByRole("button", { name: "Add" }));

  await waitFor(() => expect(onSegmentUpdated).toHaveBeenCalledWith(1, "Groceries"));
  expect(updateBody).toEqual({ segment: "groceries", applyToExisting: false });
  expect(segmentCreationCalled).toBe(false);
});

test("preview call failure surfaces an error and leaves the row editable", async () => {
  setupFetchMock({
    preview: () => jsonResponse(500, {}),
  });
  renderTransaction();

  await userEvent.selectOptions(segmentSelect(), "Groceries");

  await waitFor(() =>
    expect(
      screen.getByText(
        "Couldn't check how many other transactions would be affected. Please try again."
      )
    ).toBeInTheDocument()
  );
  expect(segmentSelect()).not.toBeDisabled();
  expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
});

test("update call failure surfaces an error, leaves the row editable, and Retry re-attempts the save", async () => {
  let updateCallCount = 0;
  setupFetchMock({
    preview: () => jsonResponse(200, { matchingTransactionCount: 0 }),
    update: () => {
      updateCallCount += 1;
      return updateCallCount === 1 ? jsonResponse(500, {}) : updateSuccessResponse("Groceries");
    },
  });
  const { onSegmentUpdated } = renderTransaction();

  await userEvent.selectOptions(segmentSelect(), "Groceries");

  await waitFor(() =>
    expect(
      screen.getByText("Couldn't save the segment change. Please try again.")
    ).toBeInTheDocument()
  );
  expect(segmentSelect()).not.toBeDisabled();

  await userEvent.click(screen.getByRole("button", { name: "Retry" }));

  await waitFor(() => expect(onSegmentUpdated).toHaveBeenCalledWith(1, "Groceries"));
  expect(updateCallCount).toBe(2);
});
