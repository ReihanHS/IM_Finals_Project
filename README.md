# IT Help Desk Ticketing System

Class 4-310 | Group 3. Java + MySQL (JDBC). Has a Swing GUI and a console (CLI) version
that share the same database code.

## How to run

1. Create the database (only needed once, or to reset it):
   ```bash
   mysql -u root -p < sql/4-310G3W2.sql
   ```
2. Load the sample data so the reports have something to show (run right after step 1):
   ```bash
   mysql -u root -p < sql/4-310G3W4_sample_data.sql
   ```
3. Check the user name and password in `db.properties`.
4. Compile and run:
   ```bash
   ./run.sh        # GUI version
   ./run.sh cli    # console version
   ```
   On Windows (PowerShell), from the project folder:
   ```powershell
   javac --release 17 -d out -cp lib/mysql-connector-j-8.4.0.jar (Get-ChildItem -Recurse -Filter *.java src).FullName
   java -cp "out;lib/mysql-connector-j-8.4.0.jar" helpdesk.gui.GuiMain   # GUI
   java -cp "out;lib/mysql-connector-j-8.4.0.jar" helpdesk.Main          # console
   ```
   In VS Code, open the Run and Debug panel and pick **Run Help Desk (GUI)** or **Run Help Desk (console)**.

## GUI version

One window with a tab for each part of the system. **File → Exit** (or closing the window) asks before quitting.

| Tab | What you can do |
|---|---|
| Tickets | View all tickets; **New ticket**; **Assign technician**; **Change status**; **Change priority**; **View details / comments** (or double-click a row) shows the status history and lets you add or delete comments |
| Users | View all users; **Add user**; **Edit user**; **Activate / deactivate** (users are never deleted) |
| Assets | View all assets; **Add asset**; **Edit asset**; **Delete asset** |
| Reports | Pick one of the 10 queries and click **Run** (or double-click it); shows the result table and the SQL |
| Reference data | Departments, support teams, categories, priorities and ticket statuses |

Forms check required fields and email format before saving, and database errors are shown in a message box
instead of closing the program.

## Console version

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

Not done yet: login, feedback, attachments, search/filter, auto-close after 3 days.

## Week 4 queries

GUI: **Reports** tab. Console: main menu → **5. Reports (queries)**; pick a number, or **A** to run all ten.
The SQL lives in `src/helpdesk/dao/ReportDao.java`; the same SQL is in
`sql/4-310G3W4_queries.sql` for running in MySQL Workbench.

| # | Business question | SQL feature |
|---|---|---|
| 1 | Which tickets are not yet assigned to a technician? | WHERE |
| 2 | Which tickets were submitted this month, and by whom? | JOIN |
| 3 | How many tickets has each technician handled? | LEFT JOIN + COUNT + GROUP BY |
| 4 | Which assets have never had a ticket? | Subquery (NOT IN) |
| 5 | Which category receives the most tickets? | JOIN + COUNT + GROUP BY |
| 6 | Which requesters submitted more than N tickets? (N is entered by the user) | GROUP BY + HAVING |
| 7 | What are the five oldest unresolved tickets? | ORDER BY + LIMIT |
| 8 | What is the average resolution time per priority? | AVG + GROUP BY |
| 9 | Which tickets missed their resolution target (overdue)? | JOIN + TIMESTAMPDIFF |
| 10 | Which technicians have more active tickets than the average technician? | Nested subquery in HAVING |

Results are printed as aligned tables. Invalid menu choices and non-numeric input
are rejected with a message, and database errors are caught and shown without
stopping the program.

## Project layout

```
src/helpdesk/
  Main.java          starts the console version and tests the connection
  db/Database.java   opens JDBC connections (reads db.properties)
  model/             data classes (Ticket, User, Asset, Comment, ...)
  dao/               all SQL lives here (TicketDao, UserDao, AssetDao, ReportDao, ...)
  cli/               console menus and keyboard input (ReportMenu = Week 4 queries)
  gui/               Swing screens (GuiMain starts it; one panel per tab)
lib/                 MySQL JDBC driver
sql/                 Week 2 database script, Week 4 sample data and queries
```

The `dao` and `model` packages do not print anything or read the keyboard, so the
`gui` and `cli` packages both call the same DAOs.
