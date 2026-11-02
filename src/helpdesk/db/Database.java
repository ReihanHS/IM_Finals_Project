package helpdesk.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens JDBC connections to the it_helpdesk database.
 * Settings are read from db.properties in the folder the program is started from.
 */
public final class Database {

    private static final String CONFIG_FILE = "db.properties";
    private static final Properties CONFIG = loadConfig();

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"),
                CONFIG.getProperty("db.password"));
    }

    public static String getUrl() {
        return CONFIG.getProperty("db.url");
    }

    private static Properties loadConfig() {
        Properties config = new Properties();
        config.setProperty("db.url", "jdbc:mysql://localhost:3306/it_helpdesk");
        config.setProperty("db.user", "root");
        config.setProperty("db.password", "");
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            config.load(in);
        } catch (IOException e) {
            System.err.println("Warning: " + CONFIG_FILE + " not found, using default settings.");
        }
        return config;
    }
}
