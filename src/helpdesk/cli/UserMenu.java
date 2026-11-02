package helpdesk.cli;

import helpdesk.dao.LookupDao;
import helpdesk.dao.UserDao;
import helpdesk.model.Role;
import helpdesk.model.User;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Optional;

/**
 * Console screens for user accounts.
 * Users are deactivated instead of deleted (BR15).
 */
public class UserMenu {

    private final ConsoleIO io;
    private final UserDao userDao = new UserDao();
    private final LookupDao lookupDao = new LookupDao();

    public UserMenu(ConsoleIO io) {
        this.io = io;
    }

    public void show() {
        while (true) {
            System.out.println();
            System.out.println("--- USERS ---");
            System.out.println("1. View all users");
            System.out.println("2. Add user");
            System.out.println("3. Update user");
            System.out.println("4. Deactivate / reactivate user");
            System.out.println("0. Back");
            String choice = io.readLine("Choose: ");
            try {
                switch (choice) {
                    case "1" -> viewAll();
                    case "2" -> add();
                    case "3" -> update();
                    case "4" -> toggleActive();
                    case "0" -> {
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (SQLIntegrityConstraintViolationException e) {
                System.out.println("Could not save: that email is already used by another account.");
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private void viewAll() throws SQLException {
        List<User> users = userDao.findAll();
        if (users.isEmpty()) {
            System.out.println("No users yet.");
            return;
        }
        String format = "%-4s %-20s %-26s %-14s %-22s %-17s %-8s%n";
        System.out.printf(format, "ID", "Name", "Email", "Role", "Department", "Team", "Active");
        for (User u : users) {
            System.out.printf(format, u.id(), ConsoleIO.shorten(u.fullName(), 20), ConsoleIO.shorten(u.email(), 26),
                    u.role(), ConsoleIO.shorten(u.departmentName(), 22), ConsoleIO.shorten(u.teamName(), 17),
                    u.active() ? "Yes" : "No");
        }
    }

    private void add() throws SQLException {
        String firstName = io.readRequired("First name: ");
        String lastName = io.readRequired("Last name: ");
        String email = io.readRequired("Email: ");
        String contact = io.readLine("Contact number (optional): ");
        String password = io.readRequired("Password: ");
        Role role = io.chooseEnum("Role", Role.values());
        int departmentId = io.choose("Department", lookupDao.findDepartments());
        Integer teamId = chooseTeam(role);

        User user = new User(0, firstName, lastName, email, contact, role, true, departmentId, null, teamId, null);
        int id = userDao.insert(user, password);
        System.out.println("User #" + id + " added.");
    }

    private void update() throws SQLException {
        Optional<User> found = findUser();
        if (found.isEmpty()) {
            return;
        }
        User current = found.get();
        System.out.println("Press Enter to keep the current value.");
        String firstName = io.readOrKeep("First name", current.firstName());
        String lastName = io.readOrKeep("Last name", current.lastName());
        String email = io.readOrKeep("Email", current.email());
        String contact = io.readOrKeep("Contact number", current.contactNumber());

        Role role = current.role();
        int departmentId = current.departmentId();
        Integer teamId = current.teamId();
        if (io.confirm("Change role, department or team?")) {
            role = io.chooseEnum("Role", Role.values());
            departmentId = io.choose("Department", lookupDao.findDepartments());
            teamId = chooseTeam(role);
        }

        User updated = new User(current.id(), firstName, lastName, email, contact, role, current.active(),
                departmentId, null, teamId, null);
        userDao.update(updated);
        System.out.println("User updated.");
    }

    private void toggleActive() throws SQLException {
        Optional<User> found = findUser();
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();
        boolean activate = !user.active();
        String action = activate ? "Reactivate" : "Deactivate";
        if (io.confirm(action + " " + user.fullName() + "?")) {
            userDao.setActive(user.id(), activate);
            System.out.println("User " + (activate ? "reactivated." : "deactivated."));
        }
    }

    /** BR3: technicians must belong to a team; other roles have no team. */
    private Integer chooseTeam(Role role) throws SQLException {
        if (role != Role.TECHNICIAN) {
            return null;
        }
        return io.choose("Support team", lookupDao.findSupportTeams());
    }

    private Optional<User> findUser() throws SQLException {
        int id = io.readInt("User ID: ");
        Optional<User> user = userDao.findById(id);
        if (user.isEmpty()) {
            System.out.println("User #" + id + " not found.");
        }
        return user;
    }
}
