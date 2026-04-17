package deals.exception;

/**
 * DealException — Hierarchy of all custom exceptions for the Deals module.
 *
 * Exception codes (from final_db_schema_detailed.pdf):
 *   ORDER_NOT_FOUND         → deal record does not exist in DB
 *   ORDER_UPDATE_CONFLICT   → deal cannot be moved to that pipeline stage
 *   ORDER_CREATION_FAILED   → deal insert failed (DB or validation error)
 *
 * @author Bhumika (Leads + Deals module)
 */
public class DealException extends RuntimeException {

    public DealException(String message) {
        super(message);
    }

    public DealException(String message, Throwable cause) {
        super(message, cause);
    }

    // -------------------------------------------------------------------------
    // ORDER_NOT_FOUND
    // -------------------------------------------------------------------------

    /**
     * Thrown when a deal with the given ID cannot be found in the database.
     */
    public static class DealNotFound extends DealException {
        public DealNotFound(int dealId) {
            super("[ORDER_NOT_FOUND] No deal exists with ID: " + dealId);
        }
        public DealNotFound(String message) {
            super("[ORDER_NOT_FOUND] " + message);
        }
    }

    // -------------------------------------------------------------------------
    // ORDER_UPDATE_CONFLICT
    // -------------------------------------------------------------------------

    /**
     * Thrown when a deal stage transition is invalid (e.g., going from CLOSED_WON
     * back to PROSPECTING, or skipping stages without permission).
     */
    public static class DealUpdateConflict extends DealException {
        public DealUpdateConflict(String message) {
            super("[ORDER_UPDATE_CONFLICT] " + message);
        }
    }

    // -------------------------------------------------------------------------
    // ORDER_CREATION_FAILED
    // -------------------------------------------------------------------------

    /**
     * Thrown when a deal cannot be created — validation failure or DB error.
     */
    public static class DealCreationFailed extends DealException {
        public DealCreationFailed(String message) {
            super("[ORDER_CREATION_FAILED] " + message);
        }
        public DealCreationFailed(String message, Throwable cause) {
            super("[ORDER_CREATION_FAILED] " + message, cause);
        }
    }
}
