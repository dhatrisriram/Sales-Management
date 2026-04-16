package leads.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL      = "jdbc:mysql://localhost:3306/polymorphs";
    private static final String USER     = "root";
    private static final String PASSWORD = "Bhumi@Mysql123";

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}