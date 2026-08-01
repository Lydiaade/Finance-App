import { useEffect, useState } from "react";
import Container from "react-bootstrap/Container";
import Nav from "react-bootstrap/Nav";
import Navbar from "react-bootstrap/Navbar";
import { NavDropdown } from "react-bootstrap";
import { BACKEND_URL } from "../../config";

const NO_ACCOUNTS_MESSAGE = "Add a bank account first to add a transaction";

function NavigationBar() {
  // Assume accounts exist until proven otherwise, so the link doesn't
  // flash disabled on every page load while the accounts fetch is in
  // flight. The page itself (§5) independently re-enforces this gate,
  // so a click during that brief window is still safe.
  const [hasAccounts, setHasAccounts] = useState(true);

  useEffect(() => {
    fetch(`${BACKEND_URL}/accounts`)
      .then((response) => response.json())
      .then((data) => setHasAccounts(Array.isArray(data) && data.length > 0))
      .catch(() => setHasAccounts(true));
  }, []);

  return (
    <>
      <Navbar expand="lg" bg="dark" variant="dark">
        <Container>
          <Navbar.Brand href="/">Finance Manager</Navbar.Brand>
          <Navbar.Toggle aria-controls="basic-navbar-nav" />
          <Navbar.Collapse id="basic-navbar-nav">
            <Nav className="me-auto">
              <Nav.Link href="/">Home</Nav.Link>
              <Nav.Link href="/accounts">Accounts</Nav.Link>
              <NavDropdown title="Transactions" id="basic-nav-dropdown">
                <NavDropdown.Item href="/uploadHistory">
                  View Uploads
                </NavDropdown.Item>
                <NavDropdown.Divider />
                <NavDropdown.Item href="/uploadTransactions">
                  Upload Transactions
                </NavDropdown.Item>
                <NavDropdown.Divider />
                {hasAccounts ? (
                  <NavDropdown.Item href="/addTransaction">
                    Add Transaction
                  </NavDropdown.Item>
                ) : (
                  <NavDropdown.Item disabled title={NO_ACCOUNTS_MESSAGE}>
                    Add Transaction
                    <small className="text-muted d-block">
                      {NO_ACCOUNTS_MESSAGE}
                    </small>
                  </NavDropdown.Item>
                )}
              </NavDropdown>
              <Nav.Link href="/segments">Segments</Nav.Link>
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>
    </>
  );
}

export default NavigationBar;
