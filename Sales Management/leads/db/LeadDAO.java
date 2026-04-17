package leads.db;

import leads.exception.LeadException;
import leads.model.Lead;
import leads.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * LeadDAO — Data Access Object for Lead persistence.
 *
 * Implements full CRUD for the `leads` table in the `polymorphs` database.
 *
 * Design rules (from final_db_schema_detailed.pdf):
 *   - All queries use PreparedStatement — NO raw SQL string concatenation
 *   - DBConnection.getConnection() is the sole connection source
 *   - Exceptions mapped to: ORDER_CREATION_FAILED, ORDER_NOT_FOUND, ORDER_UPDATE_CONFLICT
 *
 * @author Bhumika (Leads + Deals module)
 */
public class LeadDAO {

    private static final Logger logger = Logger.getLogger(LeadDAO.class.getName());

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Inserts a new lead into the `leads` table.
     * The auto-generated lead_id is written back into the Lead object.
     *
     * @param lead the Lead to persist (name and status must be valid)
     * @throws LeadException.LeadCreationFailed on any DB error
     */
    public void createLead(Lead lead) {
        String sql = "INSERT INTO leads (name, company, status) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, lead.getName());
            stmt.setString(2, lead.getCompany());
            stmt.setString(3, lead.getStatus());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    lead.setLeadId(keys.getInt(1));
                    logger.info("LeadDAO.createLead: inserted lead with ID=" + lead.getLeadId());
                }
            }

        } catch (SQLException e) {
            logger.severe("LeadDAO.createLead failed: " + e.getMessage());
            throw new LeadException.LeadCreationFailed(
                "Database error while creating lead: " + e.getMessage(), e
            );
        }
    }

    // =========================================================================
    // READ — by lead_id
    // =========================================================================

    /**
     * Retrieves a single lead by primary key.
     *
     * @param leadId the lead_id to look up
     * @return the matching Lead
     * @throws LeadException.LeadNotFound if no lead with that ID exists
     * @throws LeadException.LeadCreationFailed on DB connectivity issues
     */
    public Lead getLeadById(int leadId) {
        String sql = "SELECT * FROM leads WHERE lead_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leadId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Lead lead = mapRowToLead(rs);
                    logger.info("LeadDAO.getLeadById: found lead id=" + leadId);
                    return lead;
                }
            }

        } catch (SQLException e) {
            logger.severe("LeadDAO.getLeadById failed: " + e.getMessage());
            throw new LeadException.LeadCreationFailed(
                "DB error retrieving lead id=" + leadId, e
            );
        }

        // Lead not found — throw ORDER_NOT_FOUND
        throw new LeadException.LeadNotFound(leadId);
    }

    // =========================================================================
    // READ — all leads
    // =========================================================================

    /**
     * Returns all leads, ordered by creation date (newest first).
     *
     * @return list of all leads (may be empty)
     */
    public List<Lead> getAllLeads() {
        String sql = "SELECT * FROM leads ORDER BY created_at DESC";
        List<Lead> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                results.add(mapRowToLead(rs));
            }

            logger.info("LeadDAO.getAllLeads: retrieved " + results.size() + " leads.");
            return results;

        } catch (SQLException e) {
            logger.severe("LeadDAO.getAllLeads failed: " + e.getMessage());
            throw new LeadException.LeadCreationFailed(
                "DB error retrieving all leads", e
            );
        }
    }

    // =========================================================================
    // READ — by status
    // =========================================================================

    /**
     * Returns all leads that match the given status.
     * Useful for pipeline views (e.g., show all QUALIFIED leads).
     *
     * @param status the status to filter by (e.g., "NEW", "QUALIFIED")
     * @return list of matching leads
     */
    public List<Lead> getLeadsByStatus(String status) {
        String sql = "SELECT * FROM leads WHERE status = ? ORDER BY created_at DESC";
        List<Lead> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToLead(rs));
                }
            }

            logger.info("LeadDAO.getLeadsByStatus: found " + results.size() + " leads with status=" + status);
            return results;

        } catch (SQLException e) {
            logger.severe("LeadDAO.getLeadsByStatus failed: " + e.getMessage());
            throw new LeadException.LeadCreationFailed(
                "DB error retrieving leads with status=" + status, e
            );
        }
    }

    // =========================================================================
    // UPDATE — status
    // =========================================================================

    /**
     * Updates the status of an existing lead.
     * This should only be called AFTER LeadWorkflowEngine.validateTransition().
     *
     * @param leadId    the lead to update
     * @param newStatus the validated new status
     * @return true if updated, false if lead not found
     * @throws LeadException.LeadCreationFailed on DB error
     */
    public boolean updateLeadStatus(int leadId, String newStatus) {
        String sql = "UPDATE leads SET status = ? WHERE lead_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setInt(2, leadId);

            int affected = stmt.executeUpdate();
            logger.info("LeadDAO.updateLeadStatus: " + affected + " row(s) updated for lead id=" + leadId);
            return affected > 0;

        } catch (SQLException e) {
            logger.severe("LeadDAO.updateLeadStatus failed: " + e.getMessage());
            throw new LeadException.LeadCreationFailed(
                "DB error updating lead id=" + leadId, e
            );
        }
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    /**
     * Deletes a lead by ID.
     *
     * @param leadId the lead to delete
     * @return true if deleted, false if not found
     * @throws LeadException.LeadCreationFailed on DB error
     */
    public boolean deleteLead(int leadId) {
        String sql = "DELETE FROM leads WHERE lead_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leadId);
            int affected = stmt.executeUpdate();

            logger.info("LeadDAO.deleteLead: " + affected + " row(s) deleted for lead id=" + leadId);
            return affected > 0;

        } catch (SQLException e) {
            logger.severe("LeadDAO.deleteLead failed: " + e.getMessage());
            throw new LeadException.LeadCreationFailed(
                "DB error deleting lead id=" + leadId, e
            );
        }
    }

    // =========================================================================
    // Private helper — ResultSet → Lead
    // =========================================================================

    private Lead mapRowToLead(ResultSet rs) throws SQLException {
        Lead lead = new Lead();
        lead.setLeadId(rs.getInt("lead_id"));
        lead.setName(rs.getString("name"));
        lead.setCompany(rs.getString("company"));
        lead.setStatus(rs.getString("status"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            lead.setCreatedAt(ts.toLocalDateTime());
        }
        return lead;
    }
}
