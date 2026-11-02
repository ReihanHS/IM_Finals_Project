# IT Help Desk Ticketing System — Week 3 (Java + MySQL CRUD)

Class 4-310 | Group 3. Console (CLI) version. Connects Java to MySQL with JDBC.

## How to run

1. Create the database (only needed once, or to reset it):
   ```bash
   mysql -u root -p < sql/4-310G3W2.sql
   ```
2. Check the user name and password in `db.properties`.
3. Compile and run:
   ```bash
   ./run.sh
   ```
   In VS Code you can also open `src/helpdesk/Main.java` and press Run.

## What works

| Requirement | Where in the menu |
|---|---|
| Java connects to MySQL | Shown on startup ("Connected to ...") |
| Main menu | Tickets, Users, Assets, Reference data |
| VIEW | View all tickets, ticket details (with history and comments), users, assets, reference data |
| INSERT | Submit ticket, add user, add asset, add comment |
| UPDATE | Assign technician, change status, change priority, update user, update asset, deactivate user |
| DELETE | Delete asset, delete comment |

Business rules from Weeks 1–2 that the program follows:
- Users and tickets are never deleted (BR15). Users are deactivated instead.
- Every status change is saved in `ticket_history` in the same transaction as the ticket update (BR11).
- Only technicians can be assigned to tickets (BR6). Only technicians have a support team (BR3).
- Resolving a ticket sets `date_resolved`, closing sets `date_closed`, reopening clears both (BR14).
- An asset that a ticket uses cannot be deleted (foreign key). Set it to Retired instead.

Not done yet: login, feedback, attachments, overdue report, search/filter, auto-close after 3 days.

## Project layout

```
src/helpdesk/
  Main.java          starts the app and tests the connection
  db/Database.java   opens JDBC connections (reads db.properties)
  model/             data classes (Ticket, User, Asset, Comment, ...)
  dao/               all SQL lives here (TicketDao, UserDao, AssetDao, ...)
  cli/               console menus and keyboard input
lib/                 MySQL JDBC driver
sql/                 database script from Week 2
```

The `dao` and `model` packages do not print anything or read the keyboard.
When we build the Swing version, we only add a new `gui` package that calls the same DAOs;
the `cli` package can stay as it is.
