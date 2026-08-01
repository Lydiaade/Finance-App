import { useEffect, useState } from "react";
import { Alert } from "react-bootstrap";
import "../App.css";
import "bootstrap/dist/css/bootstrap.min.css";
import { BACKEND_URL } from "../config";
import AddTransactionForm from "../components/AddTransactionForm";

function AddTransaction() {
  const [accounts, setAccounts] = useState([]);
  const [accountsLoaded, setAccountsLoaded] = useState(false);

  useEffect(() => {
    fetch(`${BACKEND_URL}/accounts`)
      .then((response) => response.json())
      .then((data) => {
        setAccounts(data);
        setAccountsLoaded(true);
      });
  }, []);

  const noAccounts = accountsLoaded && accounts.length === 0;

  return (
    <div className="container">
      <h1 className="pageTitle">Add Transaction</h1>
      {noAccounts && (
        <Alert variant="warning">
          Add a bank account first to add a transaction.
        </Alert>
      )}
      {accountsLoaded && !noAccounts && (
        <AddTransactionForm accounts={accounts} />
      )}
    </div>
  );
}

export default AddTransaction;