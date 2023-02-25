# Finance Manager Backend

The backend has been created to manage the db for the transactions and hopefully accounts soon.


## To-Do List
- [x] Create method to generate transactions when file is sent through in front end and saves to db
    - Handle error cases on file upload
- [] Add starting salary endpoint
- [] Collate spend and income
- [] Link transactions to an account to view monthly transactions per account

## How to's
- To add dummy data to the db you can access the database in docker using the terminal. An examply transaction has been attached below.
```sql
INSERT INTO transaction(id, amount, category, date, memo, paid_to)
VALUES (nextval('transaction_id_sequence'), 200.0,'paid out', '2022-01-01', 'The other girl', 'Friend Account') ;
```