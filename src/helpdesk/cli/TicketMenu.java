package helpdesk.cli;

import helpdesk.dao.CommentDao;
import helpdesk.dao.LookupDao;
import helpdesk.dao.TicketDao;
import helpdesk.model.Comment;
import helpdesk.model.HistoryEntry;
import helpdesk.model.Lookup;
import helpdesk.model.Ticket;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Console screens for tickets, their history, and comments. */
public class TicketMenu {

    private final ConsoleIO io;
    private final TicketDao ticketDao = new TicketDao();
    private final CommentDao commentDao = new CommentDao();
    private final LookupDao lookupDao = new LookupDao();

    public TicketMenu(ConsoleIO io) {
        this.io = io;
    }

    public void show() {
        while (true) {
            System.out.println();
            System.out.println("--- TICKETS ---");
            System.out.println("1. View all tickets");
            System.out.println("2. View ticket details");
            System.out.println("3. Submit new ticket");
            System.out.println("4. Assign technician");
            System.out.println("5. Change status");
            System.out.println("6. Change priority");
            System.out.println("7. Add comment");
            System.out.println("8. Delete comment");
            System.out.println("0. Back");
            String choice = io.readLine("Choose: ");
            try {
                switch (choice) {
                    case "1" -> viewAll();
                    case "2" -> viewDetails();
                    case "3" -> submit();
                    case "4" -> assignTechnician();
                    case "5" -> changeStatus();
                    case "6" -> changePriority();
                    case "7" -> addComment();
                    case "8" -> deleteComment();
                    case "0" -> {
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private void viewAll() throws SQLException {
        List<Ticket> tickets = ticketDao.findAll();
        if (tickets.isEmpty()) {
            System.out.println("No tickets yet.");
            return;
        }
        String format = "%-4s %-28s %-12s %-9s %-18s %-18s %-16s%n";
        System.out.printf(format, "ID", "Subject", "Status", "Priority", "Requester", "Technician", "Created");
        for (Ticket t : tickets) {
            System.out.printf(format, t.id(), ConsoleIO.shorten(t.subject(), 28), t.statusName(), t.priorityName(),
                    ConsoleIO.shorten(t.requesterName(), 18), ConsoleIO.shorten(t.technicianName(), 18),
                    ConsoleIO.format(t.dateCreated()));
        }
    }

    private void viewDetails() throws SQLException {
        Optional<Ticket> found = findTicket();
        if (found.isEmpty()) {
            return;
        }
        Ticket t = found.get();
        System.out.println();
        System.out.println("Ticket #" + t.id() + ": " + t.subject());
        System.out.println("Description : " + t.description());
        System.out.println("Status      : " + t.statusName());
        System.out.println("Priority    : " + t.priorityName());
        System.out.println("Category    : " + t.categoryName());
        System.out.println("Requester   : " + t.requesterName());
        System.out.println("Technician  : " + (t.technicianName() == null ? "(unassigned)" : t.technicianName()));
        System.out.println("Asset       : " + (t.assetTag() == null ? "-" : t.assetTag()));
        System.out.println("Created     : " + ConsoleIO.format(t.dateCreated()));
        System.out.println("Updated     : " + ConsoleIO.format(t.dateUpdated()));
        System.out.println("Resolved    : " + ConsoleIO.format(t.dateResolved()));
        System.out.println("Closed      : " + ConsoleIO.format(t.dateClosed()));

        System.out.println();
        System.out.println("History:");
        for (HistoryEntry h : ticketDao.findHistory(t.id())) {
            System.out.printf("  %s  %-12s by %-18s %s%n", ConsoleIO.format(h.dateChanged()), h.statusName(),
                    h.changedByName(), h.remarks() == null ? "" : "- " + h.remarks());
        }

        System.out.println();
        System.out.println("Comments:");
        List<Comment> comments = commentDao.findByTicket(t.id());
        if (comments.isEmpty()) {
            System.out.println("  (none)");
        }
        for (Comment c : comments) {
            System.out.printf("  [%d] %s  %s%s: %s%n", c.id(), ConsoleIO.format(c.datePosted()), c.authorName(),
                    c.internal() ? " (internal)" : "", c.text());
        }
    }

    private void submit() throws SQLException {
        List<Lookup> users = lookupDao.findActiveUsers();
        if (users.isEmpty()) {
            System.out.println("There are no active users yet. Add one in the Users menu first.");
            return;
        }
        String subject = io.readRequired("Subject: ");
        String description = io.readRequired("Description: ");
        int requesterId = io.choose("Requester", users);
        int categoryId = io.choose("Category", lookupDao.findCategories());
        int priorityId = io.choose("Priority", lookupDao.findPriorities());
        Integer assetId = io.chooseOptional("Asset", lookupDao.findAssets());

        int id = ticketDao.create(subject, description, requesterId, categoryId, priorityId, assetId);
        System.out.println("Ticket #" + id + " submitted.");
    }

    private void assignTechnician() throws SQLException {
        Optional<Ticket> found = findTicket();
        if (found.isEmpty()) {
            return;
        }
        List<Lookup> technicians = lookupDao.findActiveTechnicians();
        if (technicians.isEmpty()) {
            System.out.println("There are no active technicians.");
            return;
        }
        int technicianId = io.choose("Technician", technicians);
        int changedBy = io.choose("Assigned by (staff)", lookupDao.findActiveStaff());
        ticketDao.assignTechnician(found.get().id(), technicianId, changedBy);
        System.out.println("Technician assigned.");
    }

    private void changeStatus() throws SQLException {
        Optional<Ticket> found = findTicket();
        if (found.isEmpty()) {
            return;
        }
        System.out.println("Current status: " + found.get().statusName());
        int statusId = io.choose("New status", lookupDao.findStatuses());
        int changedBy = io.choose("Changed by", lookupDao.findActiveUsers());
        String remarks = io.readLine("Remarks (optional): ");
        ticketDao.changeStatus(found.get().id(), statusId, changedBy, remarks);
        System.out.println("Status updated.");
    }

    private void changePriority() throws SQLException {
        Optional<Ticket> found = findTicket();
        if (found.isEmpty()) {
            return;
        }
        System.out.println("Current priority: " + found.get().priorityName());
        int priorityId = io.choose("New priority", lookupDao.findPriorities());
        ticketDao.changePriority(found.get().id(), priorityId);
        System.out.println("Priority updated.");
    }

    private void addComment() throws SQLException {
        Optional<Ticket> found = findTicket();
        if (found.isEmpty()) {
            return;
        }
        int userId = io.choose("Written by", lookupDao.findActiveUsers());
        String text = io.readRequired("Comment: ");
        boolean internal = io.confirm("Internal note (IT staff only)?");
        int id = commentDao.insert(found.get().id(), userId, text, internal);
        System.out.println("Comment #" + id + " added.");
    }

    private void deleteComment() throws SQLException {
        Optional<Ticket> found = findTicket();
        if (found.isEmpty()) {
            return;
        }
        List<Comment> comments = commentDao.findByTicket(found.get().id());
        if (comments.isEmpty()) {
            System.out.println("This ticket has no comments.");
            return;
        }
        for (Comment c : comments) {
            System.out.printf("  [%d] %s: %s%n", c.id(), c.authorName(), ConsoleIO.shorten(c.text(), 60));
        }
        int commentId = io.readInt("Comment ID to delete: ");
        if (comments.stream().noneMatch(c -> c.id() == commentId)) {
            System.out.println("That comment is not on this ticket.");
            return;
        }
        if (io.confirm("Delete comment #" + commentId + "?") && commentDao.delete(commentId)) {
            System.out.println("Comment deleted.");
        }
    }

    /** Asks for a ticket ID and loads it, printing a message if it does not exist. */
    private Optional<Ticket> findTicket() throws SQLException {
        int id = io.readInt("Ticket ID: ");
        Optional<Ticket> ticket = ticketDao.findById(id);
        if (ticket.isEmpty()) {
            System.out.println("Ticket #" + id + " not found.");
        }
        return ticket;
    }
}
