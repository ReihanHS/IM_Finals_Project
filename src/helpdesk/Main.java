package helpdesk;

import helpdesk.cli.MainMenu;
import helpdesk.db.Database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.NoSuchElementException;

/**
 * IT Help Desk Ticketing System - console version.
 * Class 4-310 | Group 3
 */
public class Main {

    public static void main(String[] args) {
        try (Connection con = Database.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            System.out.println("Connected to " + Database.getUrl());
            System.out.println(meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
        } catch (SQLException e) {
            System.out.println("Cannot connect to the database: " + e.getMessage());
            System.out.println("Check that MySQL is running and that db.properties is correct.");
            return;
        }

        try {
            new MainMenu().show();
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("Goodbye.");  // input was closed (Ctrl-D)
        }
    }
}
