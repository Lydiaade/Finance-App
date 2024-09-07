import {BACKEND_URL} from "../config";
import {useEffect, useRef, useState} from "react";
import Chart from "../components/Chart";
import TransactionContainer from "../components/TransactionContainer";

const {useParams} = require("react-router-dom");


function AccountInformation() {
    const [transactions, setTransactions] = useState([]);
    const [account, setAccount] = useState({id: "", name: "", sortCode: "", accountNumber: "", currentBalance: ""});
    const [chart, setChart] = useState([]);
    const [transactionView, setTransactionView] = useState(false);
    const initialised = useRef(false)

    const {id} = useParams();
    useEffect(() => {
        async function fetchData() {
            // You can await here
            await fetch(`${BACKEND_URL}/accounts/account/${id}`)
                .then((data) => data.json())
                .then((data) => {
                    setAccount(data)
                });
            await fetch(`${BACKEND_URL}/accounts/account/${id}/transactions`)
                .then((data) => data.json())
                .then((data) => {
                    setTransactions(data)
                });
            await fetch(`${BACKEND_URL}/accounts/account/${id}/transactions/monthly`)
                .then((data) => data.json())
                .then((data) => {
                    setChart(data)
                });
        }
        if (!initialised.current) {
            initialised.current = true
            fetchData()
        }
    });

    function changeView() {
        setTransactionView(!transactionView)
    }

    return (
        <div>
            <h1 className="pageTitle">{account.name}</h1>
            <button className="btn btn-primary"
                    onClick={changeView}>{transactionView ? "Hide All Transactions" : "View All Transactions"}</button>
            {transactionView ?
                transactions.length === 0 ? <p>One moment please</p> :
                    <TransactionContainer transactions={transactions}/> :
                <div>
                    <Chart data={chart}/>
                </div>}
        </div>
    );
}

export default AccountInformation;