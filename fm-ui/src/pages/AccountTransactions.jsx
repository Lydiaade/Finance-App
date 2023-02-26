import {BACKEND_URL} from "../config";
import {useEffect, useState} from "react";
import TransactionContainer from "../components/TransactionContainer";

const {useParams} = require("react-router-dom");


function AccountTransactions() {
    const [transactions, setTransactions] = useState([]);
    const [account, setAccount] = useState({id: "", name:"", sortCode: "", accountNumber: "", currentBalance: ""});

    const {id} = useParams();
    useEffect(() => {
        async function fetchData() {
            // You can await here
            await fetch(`${BACKEND_URL}/accounts/account/1`)
                .then((data) => data.json())
                .then((data) => {
                    setAccount(data)
                });
            await fetch(`${BACKEND_URL}/accounts/account/${id}/transactions`)
                .then((data) => data.json())
                .then((data) => {
                    setTransactions(data)
                });
        }
        fetchData()
    });
    console.log(id)
    console.log(transactions)
    return (
        <div>
            <h1 className="pageTitle">{account.name}</h1>
            {transactions.length === 0 ? <p>One moment please</p> :
            <TransactionContainer transactions={transactions}/>}
        </div>
    );
}

export default AccountTransactions;