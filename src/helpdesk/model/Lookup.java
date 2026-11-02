package helpdesk.model;

/**
 * An id and a display name, used for lookup lists such as departments,
 * categories, priorities and statuses. toString() returns the name so the
 * same object can be put straight into a Swing JComboBox later.
 */
public record Lookup(int id, String name) {

    @Override
    public String toString() {
        return name;
    }
}
