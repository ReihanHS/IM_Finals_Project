package helpdesk.model;

/** A row of the asset table. */
public record Asset(
        int id,
        String assetTag,
        String assetType,
        String brand,
        String model,
        String serialNumber,
        String location,
        AssetStatus status) {
}
