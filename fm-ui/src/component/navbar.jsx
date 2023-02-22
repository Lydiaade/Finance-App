import Container from 'react-bootstrap/Container';
import Nav from 'react-bootstrap/Nav';
import Navbar from 'react-bootstrap/Navbar';
import {NavDropdown} from "react-bootstrap";

function NavigationBar() {
    return (
        <>
            <Navbar expand="xxl" bg="dark" variant="dark">
                <Container>
                    <Navbar.Brand href="/">Finance Manager</Navbar.Brand>
                    <Nav className="me-auto">
                        <Navbar.Collapse id="basic-navbar-nav">
                            <Nav className="me-auto">
                                <Nav.Link href="/">Home</Nav.Link>
                                <NavDropdown title="Transactions" id="basic-nav-dropdown">
                                    <NavDropdown.Item href="/transactions">View Transactions</NavDropdown.Item>
                                    <NavDropdown.Divider />
                                    <NavDropdown.Item href="/uploadTransactions">
                                        Upload Transactions
                                    </NavDropdown.Item>
                                </NavDropdown>
                            </Nav>
                        </Navbar.Collapse>
                    </Nav>
                </Container>
            </Navbar>
        </>
    );
}

export default NavigationBar;