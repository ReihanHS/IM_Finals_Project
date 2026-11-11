package helpdesk.cli;

import helpdesk.dao.LookupDao;
import helpdesk.model.Lookup;

import java.sql.SQLException;
import java.util.List;

/** The top-level console menu. */
public class MainMenu {

    private final ConsoleIO io = new ConsoleIO();
    private final TicketMenu ticketMenu = new TicketMenu(io);
    private final UserMenu userMenu = new UserMenu(io);
    private final AssetMenu assetMenu = new AssetMenu(io);
    private final ReportMenu reportMenu = new ReportMenu(io);
    private final LookupDao lookupDao = new LookupDao();

    public void show() {
        while (true) {
            System.out.println();
            System.out.println("=== IT HELP DESK TICKETING SYSTEM ===");
            System.out.println("1. Tickets");
            System.out.println("2. Users");
            System.out.println("3. Assets");
            System.out.println("4. Reference data");
            System.out.println("5. Reports (queries)");
            System.out.println("0. Exit");
            String choice = io.readLine("Choose: ");
            switch (choice) {
                case "1" -> ticketMenu.show();
                case "2" -> userMenu.show();
                case "3" -> assetMenu.show();
                case "4" -> showReferenceData();
                case "5" -> reportMenu.show();
                case "0" -> {
                    System.out.println("Goodbye.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void showReferenceData() {
        try {
            printSection("Departments", lookupDao.findDepartments());
            printSection("Support teams", lookupDao.findSupportTeams());
            printSection("Categories", lookupDao.findCategories());
            printSection("Priorities", lookupDao.findPriorities());
            printSection("Ticket statuses", lookupDao.findStatuses());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private void printSection(String title, List<Lookup> items) {
        System.out.println();
        System.out.println(title + ":");
        for (Lookup item : items) {
            System.out.println("  " + item.id() + ". " + item.name());
        }
    }
}
