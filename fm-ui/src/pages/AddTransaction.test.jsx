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
