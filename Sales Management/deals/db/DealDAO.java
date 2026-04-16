package deals.db;

import deals.exception.DealException;
import deals.model.Deal;
import deals.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DealDAO — Data Access Object for Deal persistence.
 *
 * Implements full CRUD for the `deals` table in the `polymorphs` database.
 *
 * Design rules (from final_db_schema_detailed.pdf):
 *   - All queries use PreparedStatement — NO raw SQL string concatenation
 *   - DBConnection.getConnection() is the sole connection source
 *   - FK constraint: customer_id must reference a valid customers row
 *
 * @author Bhumika (Leads + Deals module)
 */
public class DealDAO {

    private static final Logger logger = Logger.getLogger(DealDAO.class.getName());

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Inserts a new deal into the `deals` table.
     * The auto-generated deal_id is written back into the Deal object.
     *
     * @param deal the Deal to persist
     * @throws DealException.DealCreationFailed on DB error or FK violation
     */
    public void createDeal(Deal deal) {
        String sql = "INSERT INTO deals (customer_id, amount, stage, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, deal.getCustomerId());
            stmt.setDouble(2, deal.getAmount());
            stmt.setString(3, deal.getStage());
            stmt.setString(4, deal.getStatus());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    deal.setDealId(keys.getInt(1));
                    logger.info("DealDAO.createDeal: inserted deal with ID=" + deal.getDealId());
                }
            }

        } catch (SQLException e) {
            logger.severe("DealDAO.createDeal failed: " + e.getMessage());

            // FK violation (customer not found) gives SQLState 23000
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new DealException.DealCreationFailed(
                    "Customer ID " + deal.getCustomerId() + " does not exist. Cannot create deal.", e
                );
            }
            throw new DealException.DealCreationFailed(
                "DB error while creating deal: " + e.getMessage(), e
            );
        }
    }

    // =========================================================================
    // READ — by deal_id
    // =========================================================================

    /**
     * Retrieves a single deal by primary key.
     *
     * @param dealId the deal_id to look up
     * @return the matching Deal
     * @throws DealException.DealNotFound if no deal with that ID exists
     * @throws DealException.DealCreationFailed on DB connectivity issues
     */
    public Deal getDealById(int dealId) {
        String sql = "SELECT * FROM deals WHERE deal_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, dealId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Deal deal = mapRowToDeal(rs);
                    logger.info("DealDAO.getDealById: found deal id=" + dealId);
                    return deal;
                }
            }

        } catch (SQLException e) {
            logger.severe("DealDAO.getDealById failed: " + e.getMessage());
            throw new DealException.DealCreationFailed(
                "DB error retrieving deal id=" + dealId, e
            );
        }

        throw new DealException.DealNotFound(dealId);
    }

    // =========================================================================
    // READ — by customer
    // =========================================================================

    /**
     * Returns all deals for a given customer, ordered newest first.
     *
     * @param customerId the customer's ID
     * @return list of matching deals (may be empty)
     */
    public List<Deal> getDealsByCustomer(int customerId) {
        String sql = "SELECT * FROM deals WHERE customer_id = ? ORDER BY created_at DESC";
        List<Deal> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToDeal(rs));
                }
            }

            logger.info("DealDAO.getDealsByCustomer: found " + results.size() + " deals for customer=" + customerId);
            return results;

        } catch (SQLException e) {
            logger.severe("DealDAO.getDealsByCustomer failed: " + e.getMessage());
            throw new DealException.DealCreationFailed(
                "DB error retrieving deals for customer=" + customerId, e
            );
        }
    }

    // =========================================================================
    // READ — by stage
    // =========================================================================

    /**
     * Returns all deals in a given pipeline stage.
     *
     * @param stage the pipeline stage (e.g., "PROPOSAL")
     * @return list of matching deals
     */
    public List<Deal> getDealsByStage(String stage) {
        String sql = "SELECT * FROM deals WHERE stage = ? ORDER BY created_at DESC";
        List<Deal> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, stage);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToDeal(rs));
                }
            }

            logger.info("DealDAO.getDealsByStage: found " + results.size() + " deals in stage=" + stage);
            return results;

        } catch (SQLException e) {
            logger.severe("DealDAO.getDealsByStage failed: " + e.getMessage());
            throw new DealException.DealCreationFailed(
                "DB error retrieving deals for stage=" + stage, e
            );
        }
    }

    // =========================================================================
    // UPDATE — stage and status
    // =========================================================================

    /**
     * Updates the stage and status of an existing deal.
     * Call AFTER DealWorkflowEngine.validateStageTransition().
     *
     * @param dealId    the deal to update
     * @param newStage  the validated new pipeline stage
     * @param newStatus the corresponding status (ACTIVE / WON / LOST)
     * @return true if updated, false if deal not found
     */
    public boolean updateDealStage(int dealId, String newStage, String newStatus) {
        String sql = "UPDATE deals SET stage = ?, status = ? WHERE deal_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStage);
            stmt.setString(2, newStatus);
            stmt.setInt(3, dealId);

            int affected = stmt.executeUpdate();
            logger.info("DealDAO.updateDealStage: " + affected + " row(s) updated for deal id=" + dealId);
            return affected > 0;

        } catch (SQLException e) {
            logger.severe("DealDAO.updateDealStage failed: " + e.getMessage());
            throw new DealException.DealCreationFailed(
                "DB error updating deal id=" + dealId, e
            );
        }
    }

    /**
     * Updates the amount of an existing deal.
     *
     * @param dealId    the deal to update
     * @param newAmount the new deal value (must be >= 0, validated before call)
     * @return true if updated
     */
    public boolean updateDealAmount(int dealId, double newAmount) {
        String sql = "UPDATE deals SET amount = ? WHERE deal_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newAmount);
            stmt.setInt(2, dealId);

            int affected = stmt.executeUpdate();
            logger.info("DealDAO.updateDealAmount: " + affected + " row(s) updated for deal id=" + dealId);
            return affected > 0;

        } catch (SQLException e) {
            logger.severe("DealDAO.updateDealAmount failed: " + e.getMessage());
            throw new DealException.DealCreationFailed(
                "DB error updating amount for deal id=" + dealId, e
            );
        }
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    /**
     * Deletes a deal by ID.
     * WARNING: Will fail if quotes reference this deal (FK constraint).
     * Delete related quotes first via QuoteDAO if needed.
     *
     * @param dealId the deal to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteDeal(int dealId) {
        String sql = "DELETE FROM deals WHERE deal_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, dealId);
            int affected = stmt.executeUpdate();

            logger.info("DealDAO.deleteDeal: " + affected + " row(s) deleted for deal id=" + dealId);
            return affected > 0;

        } catch (SQLException e) {
            logger.severe("DealDAO.deleteDeal failed: " + e.getMessage());

            // FK violation — quotes still reference this deal
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new DealException.DealCreationFailed(
                    "Cannot delete deal id=" + dealId + ": quotes are still linked to it. Delete those quotes first.", e
                );
            }
            throw new DealException.DealCreationFailed(
                "DB error deleting deal id=" + dealId, e
            );
        }
    }

    // =========================================================================
    // Private helper — ResultSet → Deal
    // =========================================================================

    private Deal mapRowToDeal(ResultSet rs) throws SQLException {
        Deal deal = new Deal();
        deal.setDealId(rs.getInt("deal_id"));
        deal.setCustomerId(rs.getInt("customer_id"));
        deal.setAmount(rs.getDouble("amount"));
        deal.setStage(rs.getString("stage"));
        deal.setStatus(rs.getString("status"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            deal.setCreatedAt(ts.toLocalDateTime());
        }
        return deal;
    }
}
