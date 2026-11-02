package helpdesk.cli;

import helpdesk.dao.AssetDao;
import helpdesk.model.Asset;
import helpdesk.model.AssetStatus;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Optional;

/** Console screens for IT assets. */
public class AssetMenu {

    private final ConsoleIO io;
    private final AssetDao assetDao = new AssetDao();

    public AssetMenu(ConsoleIO io) {
        this.io = io;
    }

    public void show() {
        while (true) {
            System.out.println();
            System.out.println("--- ASSETS ---");
            System.out.println("1. View all assets");
            System.out.println("2. Add asset");
            System.out.println("3. Update asset");
            System.out.println("4. Delete asset");
            System.out.println("0. Back");
            String choice = io.readLine("Choose: ");
            try {
                switch (choice) {
                    case "1" -> viewAll();
                    case "2" -> add();
                    case "3" -> update();
                    case "4" -> delete();
                    case "0" -> {
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private void viewAll() throws SQLException {
        List<Asset> assets = assetDao.findAll();
        if (assets.isEmpty()) {
            System.out.println("No assets yet.");
            return;
        }
        String format = "%-4s %-12s %-12s %-10s %-16s %-14s %-14s %-12s%n";
        System.out.printf(format, "ID", "Tag", "Type", "Brand", "Model", "Serial No.", "Location", "Status");
        for (Asset a : assets) {
            System.out.printf(format, a.id(), a.assetTag(), ConsoleIO.shorten(a.assetType(), 12),
                    ConsoleIO.shorten(a.brand(), 10), ConsoleIO.shorten(a.model(), 16),
                    ConsoleIO.shorten(a.serialNumber(), 14), ConsoleIO.shorten(a.location(), 14), a.status());
        }
    }

    private void add() throws SQLException {
        String tag = io.readRequired("Asset tag: ");
        String type = io.readRequired("Asset type (e.g. Laptop, Printer): ");
        String brand = io.readLine("Brand (optional): ");
        String model = io.readLine("Model (optional): ");
        String serial = io.readLine("Serial number (optional): ");
        String location = io.readLine("Location (optional): ");
        AssetStatus status = io.chooseEnum("Status", AssetStatus.values());

        try {
            int id = assetDao.insert(new Asset(0, tag, type, brand, model, serial, location, status));
            System.out.println("Asset #" + id + " added.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Could not save: asset tag " + tag + " already exists.");
        }
    }

    private void update() throws SQLException {
        Optional<Asset> found = findAsset();
        if (found.isEmpty()) {
            return;
        }
        Asset current = found.get();
        System.out.println("Press Enter to keep the current value.");
        String tag = io.readOrKeep("Asset tag", current.assetTag());
        String type = io.readOrKeep("Asset type", current.assetType());
        String brand = io.readOrKeep("Brand", current.brand());
        String model = io.readOrKeep("Model", current.model());
        String serial = io.readOrKeep("Serial number", current.serialNumber());
        String location = io.readOrKeep("Location", current.location());
        AssetStatus status = current.status();
        if (io.confirm("Change status (now " + status + ")?")) {
            status = io.chooseEnum("Status", AssetStatus.values());
        }

        try {
            assetDao.update(new Asset(current.id(), tag, type, brand, model, serial, location, status));
            System.out.println("Asset updated.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Could not save: asset tag " + tag + " already exists.");
        }
    }

    private void delete() throws SQLException {
        Optional<Asset> found = findAsset();
        if (found.isEmpty()) {
            return;
        }
        Asset asset = found.get();
        if (!io.confirm("Delete asset " + asset.assetTag() + "?")) {
            return;
        }
        try {
            assetDao.delete(asset.id());
            System.out.println("Asset deleted.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Cannot delete: this asset is linked to a ticket. Set its status to Retired instead.");
        }
    }

    private Optional<Asset> findAsset() throws SQLException {
        int id = io.readInt("Asset ID: ");
        Optional<Asset> asset = assetDao.findById(id);
        if (asset.isEmpty()) {
            System.out.println("Asset #" + id + " not found.");
        }
        return asset;
    }
}
