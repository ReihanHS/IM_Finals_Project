package helpdesk.model;

/**
 * A row of user_account. departmentName and teamName are filled in when the
 * user is read from the database and are ignored on insert/update.
 * teamId is null for users who are not technicians (BR3).
 */
public record User(
        int id,
        String firstName,
        String lastName,
        String email,
        String contactNumber,
        Role role,
        boolean active,
        int departmentId,
        String departmentName,
        Integer teamId,
        String teamName) {

    public String fullName() {
        return firstName + " " + lastName;
    }
}
