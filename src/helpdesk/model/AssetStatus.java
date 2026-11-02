package helpdesk.model;

/** Asset states, matching the ENUM in asset.asset_status. */
public enum AssetStatus {
    IN_USE("In Use"),
    IN_STORAGE("In Storage"),
    UNDER_REPAIR("Under Repair"),
    RETIRED("Retired");

    private final String label;

    AssetStatus(String label) {
        this.label = label;
    }

    /** The value stored in the database. */
    public String label() {
        return label;
    }

    public static AssetStatus fromLabel(String label) {
        for (AssetStatus status : values()) {
            if (status.label.equalsIgnoreCase(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown asset status: " + label);
    }

    @Override
    public String toString() {
        return label;
    }
}
