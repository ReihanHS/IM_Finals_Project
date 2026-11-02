package helpdesk.dao;

import helpdesk.db.Database;
import helpdesk.model.Comment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** CRUD for ticket_comment. */
public class CommentDao {

    public List<Comment> findByTicket(int ticketId) throws SQLException {
        String sql = "SELECT c.comment_id, c.ticket_id, c.user_id, CONCAT(u.first_name, ' ', u.last_name) AS author, "
                + "c.comment_text, c.is_internal, c.date_posted "
                + "FROM ticket_comment c JOIN user_account u ON u.user_id = c.user_id "
                + "WHERE c.ticket_id = ? ORDER BY c.date_posted, c.comment_id";
        List<Comment> comments = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(new Comment(
                            rs.getInt("comment_id"),
                            rs.getInt("ticket_id"),
                            rs.getInt("user_id"),
                            rs.getString("author"),
                            rs.getString("comment_text"),
                            rs.getBoolean("is_internal"),
                            JdbcUtil.getDateTime(rs, "date_posted")));
                }
            }
        }
        return comments;
    }

    /** Adds a comment and returns the generated comment_id. */
    public int insert(int ticketId, int userId, String text, boolean internal) throws SQLException {
        String sql = "INSERT INTO ticket_comment (comment_text, is_internal, ticket_id, user_id) VALUES (?, ?, ?, ?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, text);
            ps.setBoolean(2, internal);
            ps.setInt(3, ticketId);
            ps.setInt(4, userId);
            ps.executeUpdate();
            return JdbcUtil.generatedId(ps);
        }
    }

    /** Returns false if the comment does not exist. */
    public boolean delete(int commentId) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ticket_comment WHERE comment_id = ?")) {
            ps.setInt(1, commentId);
            return ps.executeUpdate() == 1;
        }
    }
}
