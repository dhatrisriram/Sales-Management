package leads.exception;

/**
 * LeadException — Hierarchy of all custom exceptions for the Leads module.
 *
 * Exception codes (from final_db_schema_detailed.pdf):
 *   ORDER_NOT_FOUND         → lead record does not exist in DB
 *   ORDER_UPDATE_CONFLICT   → lead cannot be updated due to state conflict
 *   ORDER_CREATION_FAILED   → lead insert failed (DB or validation error)
 *
 * All exceptions are unchecked (extend RuntimeException) so they propagate
 * cleanly through the Command layer without forcing try-catch everywhere.
 *
 * @author Bhumika (Leads + Deals module)
 */
public class LeadException extends RuntimeException {

    public LeadException(String message) {
        super(message);
    }

    public LeadException(String message, Throwable cause) {
        super(message, cause);
    }

    // -------------------------------------------------------------------------
    // ORDER_NOT_FOUND
    // -------------------------------------------------------------------------

    /**
     * Thrown when a lead with the given ID cannot be found in the database.
     */
    public static class LeadNotFound extends LeadException {
        public LeadNotFound(int leadId) {
            super("[ORDER_NOT_FOUND] No lead exists with ID: " + leadId);
        }
        public LeadNotFound(String message) {
            super("[ORDER_NOT_FOUND] " + message);
        }
    }

    // -------------------------------------------------------------------------
    // ORDER_UPDATE_CONFLICT
    // -------------------------------------------------------------------------

    /**
     * Thrown when a lead update is attempted but the lead is in an invalid
     * state for that transition (e.g., updating a CONVERTED lead).
     */
    public static class LeadUpdateConflict extends LeadException {
        public LeadUpdateConflict(String message) {
            super("[ORDER_UPDATE_CONFLICT] " + message);
        }
    }

    // -------------------------------------------------------------------------
    // ORDER_CREATION_FAILED
    // -------------------------------------------------------------------------

    /**
     * Thrown when a lead cannot be created — either validation failure or DB error.
     */
    public static class LeadCreationFailed extends LeadException {
        public LeadCreationFailed(String message) {
            super("[ORDER_CREATION_FAILED] " + message);
        }
        public LeadCreationFailed(String message, Throwable cause) {
            super("[ORDER_CREATION_FAILED] " + message, cause);
        }
    }
}
