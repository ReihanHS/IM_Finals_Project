package helpdesk.model;

/** User roles, matching the ENUM in user_account.role. */
public enum Role {
    REQUESTER("Requester"),
    TECHNICIAN("Technician"),
    ADMINISTRATOR("Administrator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** The value stored in the database. */
    public String label() {
        return label;
    }

    public static Role fromLabel(String label) {
        for (Role role : values()) {
            if (role.label.equalsIgnoreCase(label)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + label);
    }

    @Override
    public String toString() {
        return label;
    }
}
