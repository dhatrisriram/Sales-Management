package quotes.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides a shared JDBC connection to the `polymorphs` database.
 *
 * This class follows the exact JDBC template specified in the team's
 * database schema document (final_db_schema_detailed.pdf).
 *
 * All modules (Customers, Leads, Quotes, Analytics) use this same
 * DBConnection class — it is the single source of truth for DB config.
 *
 * NOTE: In a production environment, this would be replaced by a
 * connection pool (e.g., HikariCP) to avoid opening a new connection
 * on every call. For this project scope, direct DriverManager is used.
 *
 * @author Dhatri P Sriram (PES1UG23AM098) — Quotes module
 */
public class DBConnection {

    // -------------------------------------------------------------------------
    // Database configuration constants
    // -------------------------------------------------------------------------

    /** JDBC URL pointing to the local H2 database file. */
    private static final String URL = "jdbc:mysql://localhost:3306/polymorphs";
    private static final String USER = "root";
    private static final String PASSWORD = "Bhumi@Mysql123"; // your actual MySQL root password

    // -------------------------------------------------------------------------
    // Constructor — private to prevent instantiation (utility class)
    // -------------------------------------------------------------------------

    private DBConnection() {
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Opens and returns a new JDBC connection to the polymorphs database.
     *
     * Callers are responsible for closing the connection after use.
     * Best practice: use inside a try-with-resources block.
     *
     * try (Connection conn = DBConnection.getConnection()) {
     * // use conn
     * }
     *
     * @return a live {@link Connection} to the polymorphs database
     * @throws SQLException if the driver is unavailable or credentials are wrong
     */
    public static Connection getConnection() throws SQLException {
        // DriverManager opens a new physical connection each time.
        // This is acceptable for the project; swap for a DataSource in prod.
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
