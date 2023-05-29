import Container from 'react-bootstrap/Container';
import Nav from 'react-bootstrap/Nav';
import Navbar from 'react-bootstrap/Navbar';
import {NavDropdown} from "react-bootstrap";

function NavigationBar() {
    return (
        <>
            <Navbar expand="lg" bg="dark" variant="dark">
                <Container>
                    <Navbar.Brand href="/">Finance Manager</Navbar.Brand>
                    <Navbar.Toggle aria-controls="basic-navbar-nav"/>
                    <Navbar.Collapse id="basic-navbar-nav">
                        <Nav className="me-auto">
                            <Nav.Link href="/">Home</Nav.Link>
                            <Nav.Link href="/accounts">Accounts</Nav.Link>
                            <NavDropdown title="Transactions" id="basic-nav-dropdown">
                                <NavDropdown.Item href="/transactions">View Transactions</NavDropdown.Item>
                                <NavDropdown.Divider/>
                                <NavDropdown.Item href="/uploadTransactions">
                                    Upload Transactions
                                </NavDropdown.Item>
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