import {BACKEND_URL} from "../config";
import {useEffect, useRef, useState} from "react";
import Chart from "../components/Chart";
import TransactionContainer from "../components/TransactionContainer";

const {useParams} = require("react-router-dom");


function EditAccount() {
    const [account, setAccount] = useState({id: "", name: "", sortCode: "", accountNumber: "", currentBalance: ""});
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
        }
        if (!initialised.current) {
            initialised.current = true
            fetchData()
        }
    });

    return (
        <div>
            <h1 className="pageTitle">Account Details</h1>

        </div>
    );
}

export default EditAccount;