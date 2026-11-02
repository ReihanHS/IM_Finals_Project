package helpdesk.dao;

import helpdesk.db.Database;
import helpdesk.model.Asset;
import helpdesk.model.AssetStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD for the asset table. */
public class AssetDao {

    private static final String SELECT_ASSET =
            "SELECT asset_id, asset_tag, asset_type, brand, model, serial_number, location, asset_status FROM asset ";

    public List<Asset> findAll() throws SQLException {
        List<Asset> assets = new ArrayList<>();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ASSET + "ORDER BY asset_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                assets.add(map(rs));
            }
        }
        return assets;
    }

    public Optional<Asset> findById(int id) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ASSET + "WHERE asset_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Inserts a new asset and returns the generated asset_id. */
    public int insert(Asset asset) throws SQLException {
        String sql = "INSERT INTO asset (asset_tag, asset_type, brand, model, serial_number, location, asset_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setFields(ps, asset);
            ps.executeUpdate();
            return JdbcUtil.generatedId(ps);
        }
    }

    /** Returns false if the asset does not exist. */
    public boolean update(Asset asset) throws SQLException {
        String sql = "UPDATE asset SET asset_tag = ?, asset_type = ?, brand = ?, model = ?, serial_number = ?, "
                + "location = ?, asset_status = ? WHERE asset_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setFields(ps, asset);
            ps.setInt(8, asset.id());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Deletes an asset. MySQL rejects this with an
     * SQLIntegrityConstraintViolationException if a ticket still refers to it.
     * Returns false if the asset does not exist.
     */
    public boolean delete(int id) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM asset WHERE asset_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private void setFields(PreparedStatement ps, Asset asset) throws SQLException {
        ps.setString(1, asset.assetTag());
        ps.setString(2, asset.assetType());
        JdbcUtil.setNullableString(ps, 3, asset.brand());
        JdbcUtil.setNullableString(ps, 4, asset.model());
        JdbcUtil.setNullableString(ps, 5, asset.serialNumber());
        JdbcUtil.setNullableString(ps, 6, asset.location());
        ps.setString(7, asset.status().label());
    }

    private Asset map(ResultSet rs) throws SQLException {
        return new Asset(
                rs.getInt("asset_id"),
                rs.getString("asset_tag"),
                rs.getString("asset_type"),
                rs.getString("brand"),
                rs.getString("model"),
                rs.getString("serial_number"),
                rs.getString("location"),
                AssetStatus.fromLabel(rs.getString("asset_status")));
    }
}
