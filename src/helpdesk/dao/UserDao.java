package helpdesk.dao;

import helpdesk.db.Database;
import helpdesk.model.Role;
import helpdesk.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * CRUD for user_account. Users are never deleted (BR15); they are
 * deactivated instead so their tickets and history stay complete.
 */
public class UserDao {

    private static final String SELECT_USER =
            "SELECT u.user_id, u.first_name, u.last_name, u.email, u.contact_number, u.role, u.is_active, "
            + "u.department_id, d.department_name, u.team_id, t.team_name "
            + "FROM user_account u "
            + "JOIN department d ON d.department_id = u.department_id "
            + "LEFT JOIN support_team t ON t.team_id = u.team_id ";

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_USER + "ORDER BY u.user_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
        }
        return users;
    }

    public Optional<User> findById(int id) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_USER + "WHERE u.user_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Inserts a new user and returns the generated user_id. */
    public int insert(User user, String password) throws SQLException {
        String sql = "INSERT INTO user_account (first_name, last_name, email, contact_number, password_hash, "
                + "role, department_id, team_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.firstName());
            ps.setString(2, user.lastName());
            ps.setString(3, user.email());
            JdbcUtil.setNullableString(ps, 4, user.contactNumber());
            ps.setString(5, hashPassword(password));
            ps.setString(6, user.role().label());
            ps.setInt(7, user.departmentId());
            JdbcUtil.setNullableInt(ps, 8, user.teamId());
            ps.executeUpdate();
            return JdbcUtil.generatedId(ps);
        }
    }

    /** Updates the profile fields of a user. Returns false if the user does not exist. */
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE user_account SET first_name = ?, last_name = ?, email = ?, contact_number = ?, "
                + "role = ?, department_id = ?, team_id = ? WHERE user_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.firstName());
            ps.setString(2, user.lastName());
            ps.setString(3, user.email());
            JdbcUtil.setNullableString(ps, 4, user.contactNumber());
            ps.setString(5, user.role().label());
            ps.setInt(6, user.departmentId());
            JdbcUtil.setNullableInt(ps, 7, user.teamId());
            ps.setInt(8, user.id());
            return ps.executeUpdate() == 1;
        }
    }

    /** Activates or deactivates a user. Returns false if the user does not exist. */
    public boolean setActive(int userId, boolean active) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE user_account SET is_active = ? WHERE user_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("contact_number"),
                Role.fromLabel(rs.getString("role")),
                rs.getBoolean("is_active"),
                rs.getInt("department_id"),
                rs.getString("department_name"),
                JdbcUtil.getNullableInt(rs, "team_id"),
                rs.getString("team_name"));
    }

    /** SHA-256 hash of the password, stored as hex. */
    static String hashPassword(String password) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
