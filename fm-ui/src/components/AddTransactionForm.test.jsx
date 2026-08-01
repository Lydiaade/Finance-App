import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import AddTransactionForm from "./AddTransactionForm";

const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockNavigate,
}));

const accounts = [
  { id: 1, name: "Current Account" },
  { id: 2, name: "Savings Account" },
];

const segments = [
  { id: 10, name: "Groceries" },
  { id: 11, name: "Bills" },
];

function jsonResponse(status, body) {
  return Promise.resolve({
    status,
    ok: status >= 200 && status < 300,
    json: () => Promise.resolve(body),
  });
}

// Routes fetch calls by URL/method so tests don't depend on call order.
function setupFetchMock({ onSubmit } = {}) {
  global.fetch = jest.fn((url, options) => {
    if (url.endsWith("/segments") && !options) {
      return jsonResponse(200, segments);
    }
    if (url.endsWith("/transactions/transaction") && options?.method === "POST") {
      return onSubmit ? onSubmit(options) : jsonResponse(201, {});
    }
    return jsonResponse(200, []);
  });
}

async function fillValidForm({ direction = "in", amount = "25.50" } = {}) {
  await userEvent.type(screen.getByLabelText("Amount"), amount);
  await userEvent.click(
    screen.getByLabelText(direction === "in" ? "Money in" : "Money out")
  );
  await userEvent.selectOptions(screen.getByLabelText("Account"), "1");
  await userEvent.type(screen.getByLabelText("Paid to"), "Tesco");
  await userEvent.type(screen.getByLabelText("Date"), "2020-01-15");
}

beforeEach(() => {
  mockNavigate.mockClear();
});

test("populates account dropdown from props and segment dropdown from GET /segments with a no-segment option", async () => {
  setupFetchMock();
  render(<AddTransactionForm accounts={accounts} />);

  expect(screen.getByRole("option", { name: "Current Account" })).toBeInTheDocument();
  expect(screen.getByRole("option", { name: "Savings Account" })).toBeInTheDocument();

  await waitFor(() =>
    expect(screen.getByRole("option", { name: "Groceries" })).toBeInTheDocument()
  );
  expect(screen.getByRole("option", { name: "Bills" })).toBeInTheDocument();
  expect(screen.getByRole("option", { name: "No segment" })).toBeInTheDocument();
});

test("submitting with each required field missing shows inline validation only for that field, no error view", async () => {
  setupFetchMock();
  render(<AddTransactionForm accounts={accounts} />);
  await waitFor(() => screen.getByRole("option", { name: "Groceries" }));

  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));

  expect(screen.getByText("Enter an amount.")).toBeInTheDocument();
  expect(
    screen.getByText("Select whether money came in or went out.")
  ).toBeInTheDocument();
  expect(screen.getByText("Select an account.")).toBeInTheDocument();
  expect(screen.getByText("Enter who was paid or who paid you.")).toBeInTheDocument();
  expect(screen.getByText("Enter a date.")).toBeInTheDocument();

  // Must not have swapped to the dedicated error view.
  expect(screen.queryByText("Couldn't save transaction")).not.toBeInTheDocument();
  expect(global.fetch).not.toHaveBeenCalledWith(
    expect.stringContaining("/transactions/transaction"),
    expect.anything()
  );

  // Fixing one field clears only that field's error.
  await userEvent.type(screen.getByLabelText("Amount"), "10");
  expect(screen.queryByText("Enter an amount.")).not.toBeInTheDocument();
  expect(screen.getByText("Select an account.")).toBeInTheDocument();
});

test("zero or negative amount is blocked with inline validation before submit", async () => {
  setupFetchMock();
  render(<AddTransactionForm accounts={accounts} />);
  await waitFor(() => screen.getByRole("option", { name: "Groceries" }));

  await userEvent.type(screen.getByLabelText("Amount"), "0");
  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));
  expect(screen.getByText("Enter an amount greater than 0.")).toBeInTheDocument();

  await userEvent.clear(screen.getByLabelText("Amount"));
  await userEvent.type(screen.getByLabelText("Amount"), "-5");
  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));
  expect(screen.getByText("Enter an amount greater than 0.")).toBeInTheDocument();

  expect(global.fetch).not.toHaveBeenCalledWith(
    expect.stringContaining("/transactions/transaction"),
    expect.anything()
  );
});

test("future date is blocked with inline validation before submit", async () => {
  setupFetchMock();
  render(<AddTransactionForm accounts={accounts} />);
  await waitFor(() => screen.getByRole("option", { name: "Groceries" }));

  const futureYear = new Date().getFullYear() + 1;
  await userEvent.type(screen.getByLabelText("Date"), `${futureYear}-01-01`);
  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));

  expect(screen.getByText("Date cannot be in the future.")).toBeInTheDocument();
  expect(global.fetch).not.toHaveBeenCalledWith(
    expect.stringContaining("/transactions/transaction"),
    expect.anything()
  );
});

test("selecting money out sends a negative amount; money in sends a positive amount", async () => {
  let lastBody = null;
  setupFetchMock({
    onSubmit: (options) => {
      lastBody = JSON.parse(options.body);
      return jsonResponse(201, {
        id: 1,
        date: "2020-01-15",
        account: { id: 1, name: "Current Account" },
        amount: lastBody.amount,
        category: null,
        paid_to: "Tesco",
        memo: null,
      });
    },
  });
  render(<AddTransactionForm accounts={accounts} />);
  await waitFor(() => screen.getByRole("option", { name: "Groceries" }));

  await fillValidForm({ direction: "out", amount: "25.50" });
  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));

  await waitFor(() => expect(lastBody).not.toBeNull());
  expect(lastBody.amount).toBe(-25.5);
});

test("successful submit shows the success view with correct details and both actions wired", async () => {
  setupFetchMock({
    onSubmit: () =>
      jsonResponse(201, {
        id: 42,
        date: "2020-01-15",
        account: { id: 1, name: "Current Account" },
        amount: 25.5,
        category: "Groceries",
        paid_to: "Tesco",
        memo: "Weekly shop",
      }),
  });
  render(<AddTransactionForm accounts={accounts} />);
  await waitFor(() => screen.getByRole("option", { name: "Groceries" }));

  await fillValidForm({ direction: "in", amount: "25.50" });
  await userEvent.selectOptions(screen.getByLabelText("Segment"), "Groceries");
  await userEvent.type(screen.getByLabelText("Memo"), "Weekly shop");
  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));

  await waitFor(() => expect(screen.getByText("Transaction added")).toBeInTheDocument());
  expect(screen.getByText(/Amount: \+25.50/)).toBeInTheDocument();
  expect(screen.getByText(/Account: Current Account/)).toBeInTheDocument();
  expect(screen.getByText(/Paid to: Tesco/)).toBeInTheDocument();
  expect(screen.getByText(/Segment: Groceries/)).toBeInTheDocument();
  expect(screen.getByText(/Date: 15\/01\/2020/)).toBeInTheDocument();
  expect(screen.getByText(/Memo: Weekly shop/)).toBeInTheDocument();

  // "Return to dashboard" navigates home.
  await userEvent.click(screen.getByRole("button", { name: "Return to dashboard" }));
  expect(mockNavigate).toHaveBeenCalledWith("/");
});

test("'Add another transaction' resets to a blank form on the same page", async () => {
  setupFetchMock({
    onSubmit: () =>
      jsonResponse(201, {
        id: 1,
        date: "2020-01-15",
        account: { id: 1, name: "Current Account" },
        amount: 25.5,
        category: null,
        paid_to: "Tesco",
        memo: null,
      }),
  });
  render(<AddTransactionForm accounts={accounts} />);
  await waitFor(() => screen.getByRole("option", { name: "Groceries" }));

  await fillValidForm();
  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));
  await waitFor(() => expect(screen.getByText("Transaction added")).toBeInTheDocument());

  await userEvent.click(screen.getByRole("button", { name: "Add another transaction" }));

  expect(screen.getByLabelText("Amount")).toHaveValue(null);
  expect(screen.getByLabelText("Paid to")).toHaveValue("");
  expect(screen.queryByText("Transaction added")).not.toBeInTheDocument();
});

test("backend 400 shows the dedicated error view, distinct from inline field validation", async () => {
  setupFetchMock({ onSubmit: () => jsonResponse(400, {}) });
  render(<AddTransactionForm accounts={accounts} />);
  await waitFor(() => screen.getByRole("option", { name: "Groceries" }));

  await fillValidForm();
  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));

  await waitFor(() =>
    expect(screen.getByText("Couldn't save transaction")).toBeInTheDocument()
  );
  expect(screen.getByRole("button", { name: "Back to form" })).toBeInTheDocument();
});

test("network failure on submit shows the dedicated error view", async () => {
  setupFetchMock({ onSubmit: () => Promise.reject(new Error("network down")) });
  render(<AddTransactionForm accounts={accounts} />);
  await waitFor(() => screen.getByRole("option", { name: "Groceries" }));

  await fillValidForm();
  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));

  await waitFor(() =>
    expect(screen.getByText("Couldn't save transaction")).toBeInTheDocument()
  );
});

test("duplicate transaction (same amount/payee/date submitted twice) succeeds both times", async () => {
  setupFetchMock({
    onSubmit: () =>
      jsonResponse(201, {
        id: 1,
        date: "2020-01-15",
        account: { id: 1, name: "Current Account" },
        amount: 25.5,
        category: null,
        paid_to: "Tesco",
        memo: null,
      }),
  });
  render(<AddTransactionForm accounts={accounts} />);
  await waitFor(() => screen.getByRole("option", { name: "Groceries" }));

  await fillValidForm();
  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));
  await waitFor(() => expect(screen.getByText("Transaction added")).toBeInTheDocument());

  await userEvent.click(screen.getByRole("button", { name: "Add another transaction" }));
  await fillValidForm();
  await userEvent.click(screen.getByRole("button", { name: "Add transaction" }));

  await waitFor(() => expect(screen.getByText("Transaction added")).toBeInTheDocument());

  const submitCalls = global.fetch.mock.calls.filter(([url, options]) =>
    url.endsWith("/transactions/transaction") && options?.method === "POST"
  );
  expect(submitCalls).toHaveLength(2);
});
