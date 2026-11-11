package helpdesk.dao;

import helpdesk.db.Database;
import helpdesk.model.QueryResult;
import helpdesk.model.Report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Week 4 information-retrieval queries.
 * The same SQL is in sql/4-310G3W4_queries.sql so it can be run in MySQL Workbench.
 */
public class ReportDao {

    private static final List<Report> REPORTS = List.of(
            new Report(1, "Which tickets are not yet assigned to a technician?", "WHERE",
                    "SELECT ticket_id AS `ID`, subject AS `Subject`, date_created AS `Submitted` "
                    + "FROM ticket "
                    + "WHERE technician_id IS NULL AND date_closed IS NULL "
                    + "ORDER BY date_created",
                    null),

            new Report(2, "Which tickets were submitted this month, and by whom?", "JOIN",
                    "SELECT t.ticket_id AS `ID`, t.subject AS `Subject`, "
                    + "CONCAT(u.first_name, ' ', u.last_name) AS `Requester`, d.department_name AS `Department`, "
                    + "c.category_name AS `Category`, t.date_created AS `Submitted` "
                    + "FROM ticket t "
                    + "JOIN user_account u ON u.user_id = t.requester_id "
                    + "JOIN department d ON d.department_id = u.department_id "
                    + "JOIN category c ON c.category_id = t.category_id "
                    + "WHERE YEAR(t.date_created) = YEAR(CURDATE()) AND MONTH(t.date_created) = MONTH(CURDATE()) "
                    + "ORDER BY t.date_created",
                    null),

            new Report(3, "How many tickets has each technician handled?", "LEFT JOIN + COUNT + GROUP BY",
                    "SELECT CONCAT(u.first_name, ' ', u.last_name) AS `Technician`, st.team_name AS `Team`, "
                    + "COUNT(t.ticket_id) AS `Total tickets`, "
                    + "SUM(t.date_resolved IS NOT NULL) AS `Resolved` "
                    + "FROM user_account u "
                    + "JOIN support_team st ON st.team_id = u.team_id "
                    + "LEFT JOIN ticket t ON t.technician_id = u.user_id "
                    + "WHERE u.role = 'Technician' "
                    + "GROUP BY u.user_id, u.first_name, u.last_name, st.team_name "
                    + "ORDER BY `Total tickets` DESC",
                    null),

            new Report(4, "Which assets have never had a ticket?", "Subquery (NOT IN)",
                    "SELECT asset_tag AS `Tag`, asset_type AS `Type`, brand AS `Brand`, model AS `Model`, "
                    + "asset_status AS `Status` "
                    + "FROM asset "
                    + "WHERE asset_id NOT IN (SELECT asset_id FROM ticket WHERE asset_id IS NOT NULL) "
                    + "ORDER BY asset_tag",
                    null),

            new Report(5, "Which category receives the most tickets?", "JOIN + COUNT + GROUP BY",
                    "SELECT c.category_name AS `Category`, st.team_name AS `Support team`, "
                    + "COUNT(t.ticket_id) AS `Tickets` "
                    + "FROM category c "
                    + "JOIN support_team st ON st.team_id = c.team_id "
                    + "LEFT JOIN ticket t ON t.category_id = c.category_id "
                    + "GROUP BY c.category_id, c.category_name, st.team_name "
                    + "ORDER BY `Tickets` DESC, c.category_name",
                    null),

            new Report(6, "Which requesters submitted more than N tickets?", "GROUP BY + HAVING",
                    "SELECT CONCAT(u.first_name, ' ', u.last_name) AS `Requester`, u.email AS `Email`, "
                    + "COUNT(t.ticket_id) AS `Tickets` "
                    + "FROM user_account u "
                    + "JOIN ticket t ON t.requester_id = u.user_id "
                    + "GROUP BY u.user_id, u.first_name, u.last_name, u.email "
                    + "HAVING COUNT(t.ticket_id) > ? "
                    + "ORDER BY `Tickets` DESC",
                    "Show requesters with more than how many tickets? "),

            new Report(7, "What are the five oldest unresolved tickets?", "ORDER BY + LIMIT",
                    "SELECT t.ticket_id AS `ID`, t.subject AS `Subject`, s.status_name AS `Status`, "
                    + "p.priority_name AS `Priority`, DATEDIFF(NOW(), t.date_created) AS `Days open` "
                    + "FROM ticket t "
                    + "JOIN ticket_status s ON s.status_id = t.status_id "
                    + "JOIN priority p ON p.priority_id = t.priority_id "
                    + "WHERE t.date_resolved IS NULL AND t.date_closed IS NULL "
                    + "ORDER BY t.date_created "
                    + "LIMIT 5",
                    null),

            new Report(8, "What is the average resolution time per priority?", "AVG + GROUP BY",
                    "SELECT p.priority_name AS `Priority`, COUNT(t.ticket_id) AS `Resolved tickets`, "
                    + "ROUND(AVG(TIMESTAMPDIFF(MINUTE, t.date_created, t.date_resolved)) / 60, 1) AS `Avg hours`, "
                    + "p.resolution_time_hrs AS `Target hours` "
                    + "FROM priority p "
                    + "JOIN ticket t ON t.priority_id = p.priority_id "
                    + "WHERE t.date_resolved IS NOT NULL "
                    + "GROUP BY p.priority_id, p.priority_name, p.resolution_time_hrs "
                    + "ORDER BY p.priority_id",
                    null),

            new Report(9, "Which tickets missed their resolution target (overdue)?", "JOIN + TIMESTAMPDIFF",
                    "SELECT t.ticket_id AS `ID`, t.subject AS `Subject`, p.priority_name AS `Priority`, "
                    + "s.status_name AS `Status`, p.resolution_time_hrs AS `Target hours`, "
                    + "TIMESTAMPDIFF(HOUR, t.date_created, COALESCE(t.date_resolved, NOW())) AS `Hours taken` "
                    + "FROM ticket t "
                    + "JOIN priority p ON p.priority_id = t.priority_id "
                    + "JOIN ticket_status s ON s.status_id = t.status_id "
                    + "WHERE TIMESTAMPDIFF(HOUR, t.date_created, COALESCE(t.date_resolved, NOW())) "
                    + "> p.resolution_time_hrs "
                    + "ORDER BY `Hours taken` DESC",
                    null),

            new Report(10, "Which technicians have more active tickets than the average technician?",
                    "Nested subquery in HAVING",
                    "SELECT CONCAT(u.first_name, ' ', u.last_name) AS `Technician`, "
                    + "COUNT(t.ticket_id) AS `Active tickets` "
                    + "FROM user_account u "
                    + "LEFT JOIN ticket t ON t.technician_id = u.user_id "
                    + "AND t.date_resolved IS NULL AND t.date_closed IS NULL "
                    + "WHERE u.role = 'Technician' AND u.is_active = 1 "
                    + "GROUP BY u.user_id, u.first_name, u.last_name "
                    + "HAVING COUNT(t.ticket_id) > ("
                    + "  SELECT AVG(active_count) FROM ("
                    + "    SELECT COUNT(t2.ticket_id) AS active_count "
                    + "    FROM user_account u2 "
                    + "    LEFT JOIN ticket t2 ON t2.technician_id = u2.user_id "
                    + "    AND t2.date_resolved IS NULL AND t2.date_closed IS NULL "
                    + "    WHERE u2.role = 'Technician' AND u2.is_active = 1 "
                    + "    GROUP BY u2.user_id) AS workload) "
                    + "ORDER BY `Active tickets` DESC",
                    null));

    public List<Report> findAll() {
        return REPORTS;
    }

    /** Runs a report. parameter is used only if the report needs one. */
    public QueryResult run(Report report, Integer parameter) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(report.sql())) {
            if (report.needsParameter()) {
                if (parameter == null) {
                    throw new IllegalArgumentException("Report " + report.number() + " needs a value.");
                }
                ps.setInt(1, parameter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int count = meta.getColumnCount();
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= count; i++) {
                    columns.add(meta.getColumnLabel(i));
                }
                List<List<Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= count; i++) {
                        row.add(rs.getObject(i));
                    }
                    rows.add(row);
                }
                return new QueryResult(columns, rows);
            }
        }
    }
}
