package customers.db;

import customers.model.Customer;
import customers.builder.CustomerBuilder;
import customers.exception.CustomerException.*;
import quotes.db.DBConnection; // Reusing Dhatri's shared DB Connection

import java.sql.*;

public class CustomerDAO {

    public void addCustomer(Customer customer) throws DuplicateCustomerEntry {
        String sql = "INSERT INTO customers (name, email, phone, region) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getEmail());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getRegion());
            
            stmt.executeUpdate();
            
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateCustomerEntry("Customer already exists with this email. Show existing record and ask for confirmation.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Customer getCustomer(int id) throws CustomerNotFound {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new CustomerBuilder()
                        .setCustomerId(rs.getInt("customer_id"))
                        .setName(rs.getString("name"))
                        .setEmail(rs.getString("email"))
                        .setPhone(rs.getString("phone"))
                        .setRegion(rs.getString("region"))
                        .build();
            } else {
                throw new CustomerNotFound("Customer record does not exist. Prompt user to create new customer.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}