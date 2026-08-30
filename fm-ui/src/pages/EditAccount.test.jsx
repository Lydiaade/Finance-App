import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import EditAccount from "./EditAccount";

const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockNavigate,
}));

const account = {
  id: 5,
  name: "Current Account",
  sortCode: "11-22-33",
  accountNumber: "12345678",
  currentBalance: 100,
};

function response(status, body) {
  return Promise.resolve({
    status,
    ok: status >= 200 && status < 300,
    json: () => Promise.resolve(body),
  });
}

function setupFetchMock({ del } = {}) {
  global.fetch = jest.fn((url, options) => {
    if (options?.method === "DELETE") {
      return del ? del(url) : response(204, null);
    }
    return response(200, account);
  });
}

async function renderPage() {
  render(
    <MemoryRouter>
      <EditAccount />
    </MemoryRouter>
  );
  await screen.findByDisplayValue(account.name);
  global.fetch.mockClear();
}

beforeEach(() => {
  mockNavigate.mockClear();
});

test("clicking Delete Account opens a confirmation dialog without sending a request", async () => {
  setupFetchMock();
  await renderPage();

  await userEvent.click(screen.getByRole("button", { name: "Delete Account" }));

  expect(screen.getByText("Delete this account?")).toBeInTheDocument();
  expect(global.fetch).not.toHaveBeenCalled();
  expect(mockNavigate).not.toHaveBeenCalled();
});

test("the warning copy mentions upload history, not just transactions", async () => {
  setupFetchMock();
  await renderPage();

  expect(screen.getByText(/upload history/i)).toBeInTheDocument();
});

test("confirming the dialog sends the DELETE request and navigates on success", async () => {
  setupFetchMock();
  await renderPage();

  await userEvent.click(screen.getByRole("button", { name: "Delete Account" }));
  await userEvent.click(screen.getByRole("button", { name: "Yes, delete account" }));

  expect(global.fetch).toHaveBeenCalledWith(
    expect.stringContaining(`/accounts/account/${account.id}`),
    expect.objectContaining({ method: "DELETE" })
  );
  expect(mockNavigate).toHaveBeenCalledWith("/accounts");
});

test("canceling the dialog sends no request and leaves the account untouched", async () => {
  setupFetchMock();
  await renderPage();

  await userEvent.click(screen.getByRole("button", { name: "Delete Account" }));
  await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

  expect(screen.queryByText("Delete this account?")).not.toBeInTheDocument();
  expect(global.fetch).not.toHaveBeenCalled();
  expect(mockNavigate).not.toHaveBeenCalled();
});

test("a failed delete response keeps the user on the page and shows an error", async () => {
  setupFetchMock({ del: () => response(500, {}) });
  await renderPage();

  await userEvent.click(screen.getByRole("button", { name: "Delete Account" }));
  await userEvent.click(screen.getByRole("button", { name: "Yes, delete account" }));

  expect(
    await screen.findByText("Couldn't delete this account. Please try again.")
  ).toBeInTheDocument();
  expect(mockNavigate).not.toHaveBeenCalled();
  expect(screen.queryByText("Delete this account?")).not.toBeInTheDocument();
});

test("a network error on delete keeps the user on the page and shows an error", async () => {
  setupFetchMock({ del: () => Promise.reject(new Error("network down")) });
  await renderPage();

  await userEvent.click(screen.getByRole("button", { name: "Delete Account" }));
  await userEvent.click(screen.getByRole("button", { name: "Yes, delete account" }));

  expect(await screen.findByText("network down")).toBeInTheDocument();
  expect(mockNavigate).not.toHaveBeenCalled();
});
