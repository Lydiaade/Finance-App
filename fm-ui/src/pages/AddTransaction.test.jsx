import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import AddTransaction from "./AddTransaction";

function jsonResponse(status, body) {
  return Promise.resolve({
    status,
    ok: status >= 200 && status < 300,
    json: () => Promise.resolve(body),
  });
}

function mockAccountsAndSegments(accounts) {
  global.fetch = jest.fn((url) => {
    if (url.endsWith("/accounts")) {
      return jsonResponse(200, accounts);
    }
    if (url.endsWith("/segments")) {
      return jsonResponse(200, []);
    }
    return jsonResponse(200, []);
  });
}

test("renders the add-transaction form when accounts exist", async () => {
  mockAccountsAndSegments([{ id: 1, name: "Current Account" }]);

  render(
    <MemoryRouter>
      <AddTransaction />
    </MemoryRouter>
  );

  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Add transaction" })).toBeInTheDocument()
  );
  expect(
    screen.queryByText("Add a bank account first to add a transaction.")
  ).not.toBeInTheDocument();

  // Wait for the form's own segments fetch to settle too, so this test
  // doesn't finish while that state update is still in flight.
  await waitFor(() =>
    expect(screen.getByRole("option", { name: "No segment" })).toBeInTheDocument()
  );
});

test("shows a loading indicator while GET /accounts is in flight", () => {
  let resolveFetch;
  global.fetch = jest.fn(
    () =>
      new Promise((resolve) => {
        resolveFetch = resolve;
      })
  );

  render(
    <MemoryRouter>
      <AddTransaction />
    </MemoryRouter>
  );

  expect(screen.getByText("Loading...")).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Add transaction" })
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Add a bank account first to add a transaction.")
  ).not.toBeInTheDocument();

  // Avoid an unresolved-promise/act warning leaking into other tests.
  resolveFetch(jsonResponse(200, []));
});

test("GET /accounts rejecting shows a distinct load-failure message, not a permanently blank page", async () => {
  global.fetch = jest.fn(() => Promise.reject(new Error("network down")));

  render(
    <MemoryRouter>
      <AddTransaction />
    </MemoryRouter>
  );

  await waitFor(() =>
    expect(
      screen.getByText(
        "We couldn't load your accounts. Please check your connection and try again."
      )
    ).toBeInTheDocument()
  );

  // Must be distinguishable from the "zero accounts" empty state and must
  // not silently render the form.
  expect(
    screen.queryByText("Add a bank account first to add a transaction.")
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Add transaction" })
  ).not.toBeInTheDocument();
});

test("direct navigation to /addTransaction with zero accounts shows the explanatory message and no submittable form", async () => {
  mockAccountsAndSegments([]);

  render(
    <MemoryRouter initialEntries={["/addTransaction"]}>
      <AddTransaction />
    </MemoryRouter>
  );

  await waitFor(() =>
    expect(
      screen.getByText("Add a bank account first to add a transaction.")
    ).toBeInTheDocument()
  );
  expect(
    screen.queryByRole("button", { name: "Add transaction" })
  ).not.toBeInTheDocument();
});
