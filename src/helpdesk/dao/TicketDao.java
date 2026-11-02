package helpdesk.dao;

import helpdesk.db.Database;
import helpdesk.model.HistoryEntry;
import helpdesk.model.Ticket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for tickets and their history.
 * Tickets are never deleted (BR15). Every status change updates the ticket and
 * adds a ticket_history row in the same transaction (BR11).
 */
public class TicketDao {

    private static final String SELECT_TICKET =
            "SELECT t.ticket_id, t.subject, t.description, t.date_created, t.date_updated, t.date_resolved, "
            + "t.date_closed, t.requester_id, CONCAT(r.first_name, ' ', r.last_name) AS requester_name, "
            + "t.technician_id, CONCAT(tech.first_name, ' ', tech.last_name) AS technician_name, "
            + "t.category_id, c.category_name, t.priority_id, p.priority_name, t.status_id, s.status_name, "
            + "t.asset_id, a.asset_tag "
            + "FROM ticket t "
            + "JOIN user_account r ON r.user_id = t.requester_id "
            + "LEFT JOIN user_account tech ON tech.user_id = t.technician_id "
            + "JOIN category c ON c.category_id = t.category_id "
            + "JOIN priority p ON p.priority_id = t.priority_id "
            + "JOIN ticket_status s ON s.status_id = t.status_id "
            + "LEFT JOIN asset a ON a.asset_id = t.asset_id ";

    public List<Ticket> findAll() throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_TICKET + "ORDER BY t.ticket_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tickets.add(map(rs));
            }
        }
        return tickets;
    }

    public Optional<Ticket> findById(int id) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_TICKET + "WHERE t.ticket_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<HistoryEntry> findHistory(int ticketId) throws SQLException {
        String sql = "SELECT h.history_id, h.date_changed, s.status_name, "
                + "CONCAT(u.first_name, ' ', u.last_name) AS changed_by_name, h.remarks "
                + "FROM ticket_history h "
                + "JOIN ticket_status s ON s.status_id = h.status_id "
                + "JOIN user_account u ON u.user_id = h.changed_by "
                + "WHERE h.ticket_id = ? ORDER BY h.date_changed, h.history_id";
        List<HistoryEntry> history = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new HistoryEntry(
                            rs.getInt("history_id"),
                            JdbcUtil.getDateTime(rs, "date_changed"),
                            rs.getString("status_name"),
                            rs.getString("changed_by_name"),
                            rs.getString("remarks")));
                }
            }
        }
        return history;
    }

    /**
     * Submits a new ticket with status Open and writes its first history record.
     * assetId may be null. Returns the new ticket_id.
     */
    public int create(String subject, String description, int requesterId, int categoryId,
                      int priorityId, Integer assetId) throws SQLException {
        String sql = "INSERT INTO ticket (subject, description, requester_id, category_id, priority_id, status_id, "
                + "asset_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                int openId = statusIdByName(con, "Open");
                int ticketId;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, subject);
                    ps.setString(2, description);
                    ps.setInt(3, requesterId);
                    ps.setInt(4, categoryId);
                    ps.setInt(5, priorityId);
                    ps.setInt(6, openId);
                    JdbcUtil.setNullableInt(ps, 7, assetId);
                    ps.executeUpdate();
                    ticketId = JdbcUtil.generatedId(ps);
                }
                addHistory(con, ticketId, openId, requesterId, "Ticket submitted");
                con.commit();
                return ticketId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Assigns a technician to a ticket. If the ticket is still Open it moves to
     * Assigned and the change is logged. Returns false if the ticket does not exist.
     */
    public boolean assignTechnician(int ticketId, int technicianId, int changedBy) throws SQLException {
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                String currentStatus = currentStatusName(con, ticketId);
                if (currentStatus == null) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ticket SET technician_id = ?, date_updated = NOW() WHERE ticket_id = ?")) {
                    ps.setInt(1, technicianId);
                    ps.setInt(2, ticketId);
                    ps.executeUpdate();
                }
                if (currentStatus.equals("Open")) {
                    int assignedId = statusIdByName(con, "Assigned");
                    updateStatus(con, ticketId, assignedId, "Assigned");
                    addHistory(con, ticketId, assignedId, changedBy, "Assigned to technician");
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Changes the status of a ticket and logs it in ticket_history.
     * Sets date_resolved / date_closed when the ticket is Resolved / Closed,
     * and clears them when it is Reopened. Returns false if the ticket does not exist.
     */
    public boolean changeStatus(int ticketId, int statusId, int changedBy, String remarks) throws SQLException {
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (currentStatusName(con, ticketId) == null) {
                    con.rollback();
                    return false;
                }
                String statusName = statusNameById(con, statusId);
                updateStatus(con, ticketId, statusId, statusName);
                addHistory(con, ticketId, statusId, changedBy, remarks);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Returns false if the ticket does not exist. */
    public boolean changePriority(int ticketId, int priorityId) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE ticket SET priority_id = ?, date_updated = NOW() WHERE ticket_id = ?")) {
            ps.setInt(1, priorityId);
            ps.setInt(2, ticketId);
            return ps.executeUpdate() == 1;
        }
    }

    // ---- helpers that run inside the caller's transaction ----

    private void updateStatus(Connection con, int ticketId, int statusId, String statusName) throws SQLException {
        String dates = switch (statusName) {
            case "Resolved" -> ", date_resolved = NOW()";
            case "Closed" -> ", date_closed = NOW()";
            case "Reopened" -> ", date_resolved = NULL, date_closed = NULL";
            default -> "";
        };
        String sql = "UPDATE ticket SET status_id = ?, date_updated = NOW()" + dates + " WHERE ticket_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, statusId);
            ps.setInt(2, ticketId);
            ps.executeUpdate();
        }
    }

    private void addHistory(Connection con, int ticketId, int statusId, int changedBy, String remarks)
            throws SQLException {
        String sql = "INSERT INTO ticket_history (remarks, ticket_id, status_id, changed_by) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            JdbcUtil.setNullableString(ps, 1, remarks);
            ps.setInt(2, ticketId);
            ps.setInt(3, statusId);
            ps.setInt(4, changedBy);
            ps.executeUpdate();
        }
    }

    /** Returns null if the ticket does not exist. */
    private String currentStatusName(Connection con, int ticketId) throws SQLException {
        String sql = "SELECT s.status_name FROM ticket t JOIN ticket_status s ON s.status_id = t.status_id "
                + "WHERE t.ticket_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private int statusIdByName(Connection con, String name) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT status_id FROM ticket_status WHERE status_name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Status '" + name + "' is missing from ticket_status.");
                }
                return rs.getInt(1);
            }
        }
    }

    private String statusNameById(Connection con, int statusId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT status_name FROM ticket_status WHERE status_id = ?")) {
            ps.setInt(1, statusId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Status ID " + statusId + " does not exist.");
                }
                return rs.getString(1);
            }
        }
    }

    private Ticket map(ResultSet rs) throws SQLException {
        return new Ticket(
                rs.getInt("ticket_id"),
                rs.getString("subject"),
                rs.getString("description"),
                JdbcUtil.getDateTime(rs, "date_created"),
                JdbcUtil.getDateTime(rs, "date_updated"),
                JdbcUtil.getDateTime(rs, "date_resolved"),
                JdbcUtil.getDateTime(rs, "date_closed"),
                rs.getInt("requester_id"),
                rs.getString("requester_name"),
                JdbcUtil.getNullableInt(rs, "technician_id"),
                rs.getString("technician_name"),
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getInt("priority_id"),
                rs.getString("priority_name"),
                rs.getInt("status_id"),
                rs.getString("status_name"),
                JdbcUtil.getNullableInt(rs, "asset_id"),
                rs.getString("asset_tag"));
    }
}
