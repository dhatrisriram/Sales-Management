package analytics.db;

import quotes.db.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AnalyticsDAO {

    public double calculateTotalRevenue() throws SQLException {
        double total = 0.0;
        String query = "SELECT SUM(final_amount) FROM quotes";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                total = rs.getDouble(1);
            }
        }
        return total;
    }

    public int getActiveLeadsCount() throws SQLException {
        int count = 0;
        String query = "SELECT COUNT(*) FROM leads";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        }
        return count;
    }
}