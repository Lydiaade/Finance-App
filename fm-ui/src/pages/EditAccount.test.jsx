import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
  currentBalanceDate: "2024-01-01",
};

function response(status, body) {
  return Promise.resolve({
    status,
    ok: status >= 200 && status < 300,
    json: () => Promise.resolve(body),
    text: () =>
      Promise.resolve(typeof body === "string" ? body : JSON.stringify(body)),
  });
}

function deferred() {
  let resolve;
  const promise = new Promise((res) => {
    resolve = res;
  });
  return { promise, resolve };
}

function setupFetchMock({ del, get, patch } = {}) {
  global.fetch = jest.fn((url, options) => {
    if (options?.method === "DELETE") {
      return del ? del(url) : response(204, null);
    }
    if (options?.method === "PATCH") {
      return patch ? patch(url, options) : response(200, account);
    }
    return get ? get(url) : response(200, account);
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

test("the Delete Account button is disabled until the account has loaded", async () => {
  const getRequest = deferred();
  setupFetchMock({ get: () => getRequest.promise });

  render(
    <MemoryRouter>
      <EditAccount />
    </MemoryRouter>
  );

  expect(screen.getByRole("button", { name: "Delete Account" })).toBeDisabled();

  getRequest.resolve(response(200, account));
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Delete Account" })).toBeEnabled()
  );
  expect(global.fetch).not.toHaveBeenCalledWith(
    expect.stringContaining("/accounts/account/"),
    expect.objectContaining({ method: "DELETE" })
  );
});

test("confirming twice in a row while the request is in flight only sends one DELETE", async () => {
  const deleteRequest = deferred();
  setupFetchMock({ del: () => deleteRequest.promise });
  await renderPage();

  await userEvent.click(screen.getByRole("button", { name: "Delete Account" }));
  const confirmButton = screen.getByRole("button", { name: "Yes, delete account" });
  await userEvent.click(confirmButton);

  expect(await screen.findByRole("button", { name: "Deleting..." })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Cancel" })).toBeDisabled();

  // A second click while disabled must not be able to fire a second request.
  await userEvent.click(screen.getByRole("button", { name: "Deleting..." }));
  expect(global.fetch).toHaveBeenCalledTimes(1);

  deleteRequest.resolve(response(204, null));
  await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith("/accounts"));
});

test("the dialog cannot be dismissed via Escape while a delete is in flight", async () => {
  const deleteRequest = deferred();
  setupFetchMock({ del: () => deleteRequest.promise });
  await renderPage();

  await userEvent.click(screen.getByRole("button", { name: "Delete Account" }));
  await userEvent.click(screen.getByRole("button", { name: "Yes, delete account" }));
  await screen.findByRole("button", { name: "Deleting..." });

  fireEvent.keyDown(document, { key: "Escape", code: "Escape" });
  expect(screen.getByText("Delete this account?")).toBeInTheDocument();

  deleteRequest.resolve(response(204, null));
  await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith("/accounts"));
});

test("a failed delete followed by reopening the dialog clears the previous error", async () => {
  let callCount = 0;
  setupFetchMock({
    del: () => {
      callCount += 1;
      return callCount === 1 ? response(500, {}) : response(204, null);
    },
  });
  await renderPage();

  await userEvent.click(screen.getByRole("button", { name: "Delete Account" }));
  await userEvent.click(screen.getByRole("button", { name: "Yes, delete account" }));
  await screen.findByText("Couldn't delete this account. Please try again.");

  await userEvent.click(screen.getByRole("button", { name: "Delete Account" }));
  expect(
    screen.queryByText("Couldn't delete this account. Please try again.")
  ).not.toBeInTheDocument();

  await userEvent.click(screen.getByRole("button", { name: "Yes, delete account" }));
  await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith("/accounts"));
});

test("balance and balance date fields pre-populate from the fetched account", async () => {
  const decimalAccount = {
    id: 7,
    name: "Savings",
    sortCode: "44-55-66",
    accountNumber: "87654321",
    currentBalance: 1234.56,
    currentBalanceDate: "2024-03-10",
  };
  global.fetch = jest.fn(() => response(200, decimalAccount));

  render(
    <MemoryRouter>
      <EditAccount />
    </MemoryRouter>
  );

  expect(await screen.findByDisplayValue("1234.56")).toBeInTheDocument();
  expect(screen.getByDisplayValue("2024-03-10")).toBeInTheDocument();
});

test("a successful balance save shows confirmation and calls the endpoint with the correct payload", async () => {
  const updatedAccount = {
    ...account,
    currentBalance: 250.75,
    currentBalanceDate: "2024-02-01",
  };
  setupFetchMock({ patch: () => response(200, updatedAccount) });
  await renderPage();

  const balanceInput = screen.getByLabelText("Balance:");
  await userEvent.clear(balanceInput);
  await userEvent.type(balanceInput, "250.75");

  fireEvent.change(screen.getByLabelText("Balance Date:"), {
    target: { value: "2024-02-01" },
  });

  await userEvent.click(screen.getByRole("button", { name: "Save Balance" }));

  expect(global.fetch).toHaveBeenCalledWith(
    expect.stringContaining(`/accounts/account/${account.id}/balance`),
    expect.objectContaining({
      method: "PATCH",
      body: JSON.stringify({
        currentBalance: 250.75,
        currentBalanceDate: "2024-02-01",
      }),
    })
  );

  expect(
    await screen.findByText("Balance updated successfully.")
  ).toBeInTheDocument();
  expect(screen.getByDisplayValue("250.75")).toBeInTheDocument();
});

test("a failed balance save shows an error and does not indicate success", async () => {
  setupFetchMock({ patch: () => response(400, "Balance is required.") });
  await renderPage();

  await userEvent.click(screen.getByRole("button", { name: "Save Balance" }));

  expect(await screen.findByText("Balance is required.")).toBeInTheDocument();
  expect(
    screen.queryByText("Balance updated successfully.")
  ).not.toBeInTheDocument();
});

test("a 404 balance save response shows an error and does not indicate success", async () => {
  setupFetchMock({ patch: () => response(404, "Account not found.") });
  await renderPage();

  await userEvent.click(screen.getByRole("button", { name: "Save Balance" }));

  expect(await screen.findByText("Account not found.")).toBeInTheDocument();
  expect(
    screen.queryByText("Balance updated successfully.")
  ).not.toBeInTheDocument();
});

test("submitting with the balance field cleared does not send a request or show success", async () => {
  setupFetchMock();
  await renderPage();

  const balanceInput = screen.getByLabelText("Balance:");
  await userEvent.clear(balanceInput);

  await userEvent.click(screen.getByRole("button", { name: "Save Balance" }));

  expect(global.fetch).not.toHaveBeenCalled();
  expect(
    screen.queryByText("Balance updated successfully.")
  ).not.toBeInTheDocument();
});
