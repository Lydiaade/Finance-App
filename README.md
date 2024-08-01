# Finance-App

This app is to replace my current financial manager which currently exists in Microsoft Excel.
Though this app does the job, I would ideally like to have an app that does it all for me and that can be updated to have much cooler features and maybe one day monetized. It will also allow me to learn more about how I handle my finances as well as practicing coding languages I don't use on a regular basis.

## Tech Stack
 - Java 21
 - React (Javascript)

## Managing Feature Merges
In order to manage the features that will be implemented on this project we have made use of a kanban board, which details upcoming features.

**Notion board:** https://www.notion.so/80f901f8bbc14207b7769e1c03c299c9?v=724be6485f7f468b8cd5a6e05404945d&pvs=4

This also dictates the way of which commits are made based on the ticket numbers.

### Merge Commit Format

'```[FM-**] Adding new delete button```'

Where `FM-**` is the ticket number. This will allow for the integration with notion for merge commits to work successfully.
This project used pr's to manage feature merges which should match the ticket number.