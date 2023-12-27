# Finance Manager Backend

The backend has been created to manage the db for the transactions and hopefully accounts soon.

## How to start backend
1. Ensure docker is running and your terminal is open to the current directory for the `fm-backend`
2. Run the docker compose file, to load the database and do so in the background `docker-compose up -d` 
3. Run the FinanceManagerApplication file


## Helper
- If there are other ports open using port 8080 you must destroy them, this can easily be done by running the following command ` lsof -i :8080`

## To-Do List 
#### Can now be found in this Notion Kanban Board: https://www.notion.so/80f901f8bbc14207b7769e1c03c299c9?v=724be6485f7f468b8cd5a6e05404945d&pvs=4
- [x] Create method to generate transactions when file is sent through in front end and saves to db
    - [ ] Handle error cases on file upload
- [ ] Add starting salary endpoint
- [ ] Collate spend and income
- [ ] Figure out how to save vsc file uploads to directory to gitignore for security reasons
- [x] Link transactions to an account to view monthly transactions per account

## How to's
- To add dummy data to the db you can access the database in docker using the terminal. An examply transaction has been attached below.
```sql
INSERT INTO transaction(id, amount, category, date, memo, paid_to)
VALUES (nextval('transaction_id_sequence'), 200.0,'paid out', '2022-01-01', 'The other girl', 'Friend Account') ;
```