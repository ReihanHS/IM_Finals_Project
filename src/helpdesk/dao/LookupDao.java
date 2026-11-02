package helpdesk.dao;

import helpdesk.db.Database;
import helpdesk.model.Lookup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Reads the reference (lookup) tables used to fill choice lists. */
public class LookupDao {

    public List<Lookup> findDepartments() throws SQLException {
        return query("SELECT department_id, department_name FROM department ORDER BY department_id");
    }

    public List<Lookup> findSupportTeams() throws SQLException {
        return query("SELECT team_id, team_name FROM support_team ORDER BY team_id");
    }

    public List<Lookup> findCategories() throws SQLException {
        return query("SELECT category_id, category_name FROM category ORDER BY category_id");
    }

    public List<Lookup> findPriorities() throws SQLException {
        return query("SELECT priority_id, priority_name FROM priority ORDER BY priority_id");
    }

    public List<Lookup> findStatuses() throws SQLException {
        return query("SELECT status_id, status_name FROM ticket_status ORDER BY status_id");
    }

    public List<Lookup> findAssets() throws SQLException {
        return query("SELECT asset_id, CONCAT(asset_tag, ' - ', asset_type) FROM asset "
                + "WHERE asset_status <> 'Retired' ORDER BY asset_id");
    }

    /** Active users of any role, e.g. to choose a requester. */
    public List<Lookup> findActiveUsers() throws SQLException {
        return query("SELECT user_id, CONCAT(first_name, ' ', last_name, ' (', role, ')') FROM user_account "
                + "WHERE is_active = 1 ORDER BY user_id");
    }

    /** Active technicians and administrators (IT staff). */
    public List<Lookup> findActiveStaff() throws SQLException {
        return query("SELECT user_id, CONCAT(first_name, ' ', last_name, ' (', role, ')') FROM user_account "
                + "WHERE is_active = 1 AND role IN ('Technician', 'Administrator') ORDER BY user_id");
    }

    /** Active technicians, shown with their team (BR6: only technicians can be assigned). */
    public List<Lookup> findActiveTechnicians() throws SQLException {
        return query("SELECT u.user_id, CONCAT(u.first_name, ' ', u.last_name, ' - ', t.team_name) "
                + "FROM user_account u JOIN support_team t ON t.team_id = u.team_id "
                + "WHERE u.is_active = 1 AND u.role = 'Technician' ORDER BY u.user_id");
    }

    /** Runs a query whose first column is the id and second column is the name. */
    private List<Lookup> query(String sql) throws SQLException {
        List<Lookup> items = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new Lookup(rs.getInt(1), rs.getString(2)));
            }
        }
        return items;
    }
}
