import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import NavigationBar from "./Navbar";

function jsonResponse(status, body) {
  return Promise.resolve({
    status,
    ok: status >= 200 && status < 300,
    json: () => Promise.resolve(body),
  });
}

function mockAccounts(accounts) {
  global.fetch = jest.fn(() => jsonResponse(200, accounts));
}

test("zero accounts: nav entry point renders disabled with explanatory text", async () => {
  mockAccounts([]);
  render(<NavigationBar />);
  // Wait for the accounts fetch to resolve and hasAccounts to flip to false
  // before opening the dropdown, otherwise we can race the state update.
  await waitFor(() => expect(global.fetch).toHaveBeenCalled());

  await userEvent.click(screen.getByText("Transactions"));

  await waitFor(() => {
    const link = screen.getByText("Add Transaction").closest("a");
    expect(link).toHaveClass("disabled");
    expect(link).toHaveAttribute("aria-disabled", "true");
  });
  expect(
    screen.getByText("Add a bank account first to add a transaction")
  ).toBeInTheDocument();
});

test("with accounts: nav entry point is an enabled link to /addTransaction", async () => {
  mockAccounts([{ id: 1, name: "Current Account" }]);
  render(<NavigationBar />);
  await waitFor(() => expect(global.fetch).toHaveBeenCalled());

  await userEvent.click(screen.getByText("Transactions"));

  await waitFor(() => {
    const link = screen.getByText("Add Transaction").closest("a");
    expect(link).not.toHaveClass("disabled");
    expect(link).toHaveAttribute("href", "/addTransaction");
  });
});

// QA gap: the fetch-failure branch (GET /accounts rejecting) was untested.
// Navbar deliberately "fails open" here (hasAccounts stays true) so a backend
// hiccup doesn't block the entry point outright - the page-level gate (AC §5)
// independently re-enforces the real check when /addTransaction actually
// loads. This test pins down that documented fail-open behaviour so a future
// change can't silently flip it without a test noticing.
test("GET /accounts failing leaves the nav entry point enabled (fails open, page-level gate backstops it)", async () => {
  global.fetch = jest.fn(() => Promise.reject(new Error("network down")));
  render(<NavigationBar />);
  await waitFor(() => expect(global.fetch).toHaveBeenCalled());

  await userEvent.click(screen.getByText("Transactions"));

  await waitFor(() => {
    const link = screen.getByText("Add Transaction").closest("a");
    expect(link).not.toHaveClass("disabled");
    expect(link).toHaveAttribute("href", "/addTransaction");
  });
});
