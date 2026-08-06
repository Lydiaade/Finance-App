import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import SegmentContainer from "./SegmentContainer";

function response(status, body) {
  return Promise.resolve({
    status,
    ok: status >= 200 && status < 300,
    json: () => Promise.resolve(body),
    text: () =>
      Promise.resolve(typeof body === "string" ? body : JSON.stringify(body ?? {})),
  });
}

const baseSegments = [
  { id: 1, name: "Groceries" },
  { id: 2, name: "Bills" },
];

function renderContainer({
  segments = baseSegments,
  onSegmentDeleted = jest.fn(),
  onSegmentRenamed = jest.fn(),
} = {}) {
  render(
    <SegmentContainer
      segments={segments}
      onSegmentDeleted={onSegmentDeleted}
      onSegmentRenamed={onSegmentRenamed}
    />
  );
  return { onSegmentDeleted, onSegmentRenamed };
}

// Routes fetch calls by URL/method so tests don't depend on call order.
function setupFetchMock({ usage, rename, del } = {}) {
  global.fetch = jest.fn((url, options) => {
    if (url.includes("/usage")) {
      return usage ? usage(url) : response(200, { transactionCount: 0 });
    }
    if (options?.method === "PATCH") {
      return rename ? rename(options) : response(200, { segment: { id: 1, name: "Renamed" } });
    }
    if (options?.method === "DELETE") {
      return del ? del(url) : response(204, null);
    }
    return response(200, []);
  });
}

function deleteButtonFor(name) {
  const row = screen.getByText(name).closest("tr");
  return row.querySelector("button");
}

test("deleting a segment with 0 usages deletes immediately with no modal (regression)", async () => {
  setupFetchMock({
    usage: () => response(200, { transactionCount: 0 }),
  });
  const { onSegmentDeleted } = renderContainer();

  await userEvent.click(deleteButtonFor("Groceries"));

  await waitFor(() => expect(onSegmentDeleted).toHaveBeenCalledWith(1));
  expect(screen.queryByText(/is in use/)).not.toBeInTheDocument();
  expect(screen.queryByText(/currently using this segment/)).not.toBeInTheDocument();
});

test("deleting a segment with >0 usages shows the confirmation modal with the real count", async () => {
  setupFetchMock({
    usage: () => response(200, { transactionCount: 12 }),
  });
  renderContainer();

  await userEvent.click(deleteButtonFor("Groceries"));

  expect(
    await screen.findByText("12 transactions are currently using this segment.")
  ).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Leave it" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Rename it" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Delete anyway" })).toBeInTheDocument();
});

test('"Leave it" closes the modal without deleting or renaming', async () => {
  setupFetchMock({
    usage: () => response(200, { transactionCount: 5 }),
  });
  const { onSegmentDeleted, onSegmentRenamed } = renderContainer();

  await userEvent.click(deleteButtonFor("Groceries"));
  await screen.findByText(/currently using this segment/);

  await userEvent.click(screen.getByRole("button", { name: "Leave it" }));

  await waitFor(() =>
    expect(screen.queryByText(/currently using this segment/)).not.toBeInTheDocument()
  );
  expect(onSegmentDeleted).not.toHaveBeenCalled();
  expect(onSegmentRenamed).not.toHaveBeenCalled();
  // Only the usage-check call should have happened - no delete/rename call.
  expect(global.fetch).toHaveBeenCalledTimes(1);
});

test('"Rename it" success updates the segment in place and closes the modal', async () => {
  let renameBody = null;
  setupFetchMock({
    usage: () => response(200, { transactionCount: 3 }),
    rename: (options) => {
      renameBody = JSON.parse(options.body);
      return response(200, {
        segment: { id: 1, name: "Groceries & Food" },
        updatedTransactionCount: 3,
        updatedRuleCount: 1,
      });
    },
  });
  const { onSegmentRenamed } = renderContainer();

  await userEvent.click(deleteButtonFor("Groceries"));
  await screen.findByText(/currently using this segment/);

  await userEvent.click(screen.getByRole("button", { name: "Rename it" }));
  const input = screen.getByLabelText("New name");
  await userEvent.clear(input);
  await userEvent.type(input, "Groceries & Food");
  await userEvent.click(screen.getByRole("button", { name: "Save name" }));

  await waitFor(() => expect(onSegmentRenamed).toHaveBeenCalledWith(1, "Groceries & Food"));
  expect(renameBody).toEqual({ name: "Groceries & Food" });
  expect(screen.queryByText(/currently using this segment/)).not.toBeInTheDocument();
});

test('"Rename it" collision (400) keeps the modal open with an inline error', async () => {
  setupFetchMock({
    usage: () => response(200, { transactionCount: 3 }),
    rename: () => response(400, "A segment named 'Bills' already exists."),
  });
  renderContainer();

  await userEvent.click(deleteButtonFor("Groceries"));
  await screen.findByText(/currently using this segment/);

  await userEvent.click(screen.getByRole("button", { name: "Rename it" }));
  const input = screen.getByLabelText("New name");
  await userEvent.clear(input);
  await userEvent.type(input, "Bills");
  await userEvent.click(screen.getByRole("button", { name: "Save name" }));

  expect(
    await screen.findByText("A segment named 'Bills' already exists.")
  ).toBeInTheDocument();
  // Modal itself is still open.
  expect(screen.getByText(/currently using this segment/)).toBeInTheDocument();
  expect(screen.getByLabelText("New name")).toBeInTheDocument();
});

test('"Delete anyway" removes the segment from the visible list and closes the modal', async () => {
  setupFetchMock({
    usage: () => response(200, { transactionCount: 7 }),
    del: () => response(204, null),
  });
  const { onSegmentDeleted } = renderContainer();

  await userEvent.click(deleteButtonFor("Groceries"));
  await screen.findByText(/currently using this segment/);

  await userEvent.click(screen.getByRole("button", { name: "Delete anyway" }));

  await waitFor(() => expect(onSegmentDeleted).toHaveBeenCalledWith(1));
  expect(screen.queryByText(/currently using this segment/)).not.toBeInTheDocument();
});

test("a failed usage check shows an error and does not delete the segment", async () => {
  setupFetchMock({
    usage: () => response(500, {}),
  });
  const { onSegmentDeleted } = renderContainer();

  await userEvent.click(deleteButtonFor("Groceries"));

  expect(
    await screen.findByText(
      "Couldn't check how many transactions use this segment. Please try again."
    )
  ).toBeInTheDocument();
  expect(onSegmentDeleted).not.toHaveBeenCalled();
  expect(screen.queryByText(/currently using this segment/)).not.toBeInTheDocument();
  // The usage-check call happened, but nothing else did (no DELETE call).
  expect(global.fetch).toHaveBeenCalledTimes(1);
});

test("a failed delete-anyway call keeps the modal open with an inline error", async () => {
  setupFetchMock({
    usage: () => response(200, { transactionCount: 4 }),
    del: () => response(500, {}),
  });
  const { onSegmentDeleted } = renderContainer();

  await userEvent.click(deleteButtonFor("Groceries"));
  await screen.findByText(/currently using this segment/);

  await userEvent.click(screen.getByRole("button", { name: "Delete anyway" }));

  expect(
    await screen.findByText("Couldn't delete this segment. Please try again.")
  ).toBeInTheDocument();
  expect(onSegmentDeleted).not.toHaveBeenCalled();
  expect(screen.getByText(/currently using this segment/)).toBeInTheDocument();
});

test("a failed zero-usage delete surfaces an error instead of failing silently", async () => {
  setupFetchMock({
    usage: () => response(200, { transactionCount: 0 }),
    del: () => response(404, "Segment not found"),
  });
  const { onSegmentDeleted } = renderContainer();

  await userEvent.click(deleteButtonFor("Groceries"));

  expect(await screen.findByText("Segment not found")).toBeInTheDocument();
  expect(onSegmentDeleted).not.toHaveBeenCalled();
});
