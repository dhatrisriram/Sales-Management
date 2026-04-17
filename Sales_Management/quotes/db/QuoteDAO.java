package quotes.db;

import quotes.exception.QuoteException;
import quotes.model.Quote;
import quotes.model.QuoteItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * QuoteDAO — Data Access Object for Quote and QuoteItem persistence.
 *
 * Implements full CRUD (Create, Read, Update, Delete) for:
 *   - The `quotes` table
 *   - The `quote_items` table (inserted/deleted together with their parent quote)
 *
 * Design rules (from final_db_schema_detailed.pdf):
 *   - All queries use PreparedStatement — NO raw SQL string concatenation
 *   - DBConnection.getConnection() is used as the sole connection source
 *   - Foreign key consistency is maintained (quote_items reference quotes)
 *   - Exceptions mapped: QUOTE_GENERATION_FAILED, DATABASE_CONNECTION_FAILURE
 *
 * @author Dhatri P Sriram (PES1UG23AM098)
 */
public class QuoteDAO {

    private static final Logger logger = Logger.getLogger(QuoteDAO.class.getName());

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Persists a new Quote and all its QuoteItems to the database.
     *
     * Transactional behaviour:
     *   - Auto-commit is disabled so items are only saved if the quote header saves.
     *   - If any step fails, the entire transaction is rolled back.
     *   - The auto-generated quote_id is written back into the Quote object.
     *
     * @param quote The Quote (built via QuoteBuilder) to persist
     * @throws QuoteException.QuoteGenerationFailed on any DB error during insert
     */
    public void createQuote(Quote quote) {
        // SQL for inserting the quote header row
        String quoteSQL = "INSERT INTO quotes (customer_id, deal_id, total_amount, discount, final_amount) "
                        + "VALUES (?, ?, ?, ?, ?)";

        // SQL for inserting each line item
        String itemSQL  = "INSERT INTO quote_items (quote_id, product_name, quantity, price) "
                        + "VALUES (?, ?, ?, ?)";

        // Use try-with-resources to ensure connection is always closed
        try (Connection conn = DBConnection.getConnection()) {

            // Disable auto-commit so we can roll back on partial failure
            conn.setAutoCommit(false);

            try (PreparedStatement quoteStmt = conn.prepareStatement(
                    quoteSQL, Statement.RETURN_GENERATED_KEYS)) {

                // Bind quote header fields
                quoteStmt.setInt(1, quote.getCustomerId());

                // dealId = -1 means no deal linked; store as NULL in DB
                if (quote.getDealId() == -1) {
                    quoteStmt.setNull(2, Types.INTEGER);
                } else {
                    quoteStmt.setInt(2, quote.getDealId());
                }

                quoteStmt.setDouble(3, quote.getTotalAmount());
                quoteStmt.setDouble(4, quote.getDiscount());
                quoteStmt.setDouble(5, quote.getFinalAmount());

                quoteStmt.executeUpdate();

                // Retrieve the auto-generated quote_id and write it back
                try (ResultSet keys = quoteStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        quote.setQuoteId(keys.getInt(1));
                        logger.info("Quote inserted with ID: " + quote.getQuoteId());
                    }
                }
            }

            // Now insert each line item, referencing the newly created quote_id
            try (PreparedStatement itemStmt = conn.prepareStatement(
                    itemSQL, Statement.RETURN_GENERATED_KEYS)) {

                for (QuoteItem item : quote.getItems()) {
                    itemStmt.setInt(1, quote.getQuoteId());
                    itemStmt.setString(2, item.getProductName());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getPrice());

                    itemStmt.addBatch(); // batch for performance
                }

                itemStmt.executeBatch();

                // Write back generated item IDs
                try (ResultSet itemKeys = itemStmt.getGeneratedKeys()) {
                    int idx = 0;
                    while (itemKeys.next()) {
                        quote.getItems().get(idx++).setItemId(itemKeys.getInt(1));
                    }
                }
            }

            // All steps succeeded — commit the transaction
            conn.commit();
            logger.info("Quote transaction committed. QuoteID=" + quote.getQuoteId());

        } catch (SQLException e) {
            // QUOTE_GENERATION_FAILED: wrap the SQL exception with context
            logger.severe("createQuote failed: " + e.getMessage());
            throw new QuoteException.QuoteGenerationFailed(
                "Database error during quote creation", e
            );
        }
    }

    // =========================================================================
    // READ — by quote_id
    // =========================================================================

    /**
     * Retrieves a single Quote (with its items) by primary key.
     *
     * @param quoteId The quote_id to look up
     * @return the matching Quote, or null if not found
     * @throws QuoteException.QuoteGenerationFailed on DB connectivity issues
     */
    public Quote getQuoteById(int quoteId) {
        String quoteSQL = "SELECT * FROM quotes WHERE quote_id = ?";
        String itemSQL  = "SELECT * FROM quote_items WHERE quote_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement quoteStmt = conn.prepareStatement(quoteSQL);
             PreparedStatement itemStmt  = conn.prepareStatement(itemSQL)) {

            // ---- Fetch quote header ----
            quoteStmt.setInt(1, quoteId);
            Quote quote = null;

            try (ResultSet rs = quoteStmt.executeQuery()) {
                if (rs.next()) {
                    quote = mapRowToQuote(rs);
                }
            }

            if (quote == null) {
                logger.warning("getQuoteById: no quote found for id=" + quoteId);
                return null;
            }

            // ---- Fetch associated line items ----
            itemStmt.setInt(1, quoteId);
            try (ResultSet rs = itemStmt.executeQuery()) {
                while (rs.next()) {
                    quote.addItem(mapRowToQuoteItem(rs));
                }
            }

            logger.info("getQuoteById: loaded quote " + quoteId + " with " + quote.getItems().size() + " items");
            return quote;

        } catch (SQLException e) {
            logger.severe("getQuoteById failed: " + e.getMessage());
            throw new QuoteException.QuoteGenerationFailed(
                "Could not retrieve quote id=" + quoteId, e
            );
        }
    }

    // =========================================================================
    // READ — all quotes for a customer
    // =========================================================================

    /**
     * Returns all quotes belonging to a specific customer.
     * Items are NOT loaded here (header-only) for performance — use getQuoteById for full details.
     *
     * @param customerId the customer whose quotes to retrieve
     * @return list of Quote headers (items list will be empty)
     */
    public List<Quote> getQuotesByCustomer(int customerId) {
        String sql = "SELECT * FROM quotes WHERE customer_id = ? ORDER BY created_at DESC";
        List<Quote> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToQuote(rs));
                }
            }

            logger.info("getQuotesByCustomer: found " + results.size() + " quotes for customer " + customerId);
            return results;

        } catch (SQLException e) {
            logger.severe("getQuotesByCustomer failed: " + e.getMessage());
            throw new QuoteException.QuoteGenerationFailed(
                "Could not retrieve quotes for customer=" + customerId, e
            );
        }
    }

    // =========================================================================
    // UPDATE — discount or amounts
    // =========================================================================

    /**
     * Updates the discount and final_amount for an existing quote.
     *
     * Called when admin approves a DISCOUNT_LIMIT_EXCEEDED override
     * or when pricing is manually adjusted.
     *
     * @param quoteId     ID of the quote to update
     * @param newDiscount The new discount percentage
     * @param newFinal    The recalculated final amount
     * @return true if a row was updated, false if the quote was not found
     */
    public boolean updateQuoteDiscount(int quoteId, double newDiscount, double newFinal) {
        String sql = "UPDATE quotes SET discount = ?, final_amount = ? WHERE quote_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newDiscount);
            stmt.setDouble(2, newFinal);
            stmt.setInt(3, quoteId);

            int affected = stmt.executeUpdate();
            logger.info("updateQuoteDiscount: " + affected + " row(s) updated for quoteId=" + quoteId);
            return affected > 0;

        } catch (SQLException e) {
            logger.severe("updateQuoteDiscount failed: " + e.getMessage());
            throw new QuoteException.QuoteGenerationFailed(
                "Could not update discount for quote id=" + quoteId, e
            );
        }
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    /**
     * Deletes a quote and all its items from the database.
     *
     * Items are deleted first to respect the foreign key constraint
     * (quote_items.quote_id → quotes.quote_id).
     *
     * @param quoteId ID of the quote to delete
     * @return true if the quote was deleted, false if not found
     */
    public boolean deleteQuote(int quoteId) {
        String deleteItemsSQL = "DELETE FROM quote_items WHERE quote_id = ?";
        String deleteQuoteSQL = "DELETE FROM quotes WHERE quote_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Delete items first (child table)
            try (PreparedStatement itemStmt = conn.prepareStatement(deleteItemsSQL)) {
                itemStmt.setInt(1, quoteId);
                int itemsDeleted = itemStmt.executeUpdate();
                logger.info("deleteQuote: removed " + itemsDeleted + " items for quoteId=" + quoteId);
            }

            // Delete the quote header (parent table)
            int quotesDeleted = 0;
            try (PreparedStatement quoteStmt = conn.prepareStatement(deleteQuoteSQL)) {
                quoteStmt.setInt(1, quoteId);
                quotesDeleted = quoteStmt.executeUpdate();
            }

            conn.commit();
            logger.info("deleteQuote: quote " + quoteId + " deleted. Rows affected: " + quotesDeleted);
            return quotesDeleted > 0;

        } catch (SQLException e) {
            logger.severe("deleteQuote failed: " + e.getMessage());
            throw new QuoteException.QuoteGenerationFailed(
                "Could not delete quote id=" + quoteId, e
            );
        }
    }

    // =========================================================================
    // Private helpers — ResultSet → Model mapping
    // =========================================================================

    /**
     * Maps a single ResultSet row from the `quotes` table to a Quote object.
     * Does NOT load items — callers load items separately.
     */
    private Quote mapRowToQuote(ResultSet rs) throws SQLException {
        Quote q = new Quote();
        q.setQuoteId(rs.getInt("quote_id"));
        q.setCustomerId(rs.getInt("customer_id"));

        // deal_id can be NULL in the DB (unlinked quote)
        int dealId = rs.getInt("deal_id");
        q.setDealId(rs.wasNull() ? -1 : dealId);

        q.setTotalAmount(rs.getDouble("total_amount"));
        q.setDiscount(rs.getDouble("discount"));
        q.setFinalAmount(rs.getDouble("final_amount"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            q.setCreatedAt(ts.toLocalDateTime());
        }

        return q;
    }

    /**
     * Maps a single ResultSet row from the `quote_items` table to a QuoteItem.
     */
    private QuoteItem mapRowToQuoteItem(ResultSet rs) throws SQLException {
        QuoteItem item = new QuoteItem();
        item.setItemId(rs.getInt("item_id"));
        item.setQuoteId(rs.getInt("quote_id"));
        item.setProductName(rs.getString("product_name"));
        item.setQuantity(rs.getInt("quantity"));
        item.setPrice(rs.getDouble("price"));
        return item;
    }
}
