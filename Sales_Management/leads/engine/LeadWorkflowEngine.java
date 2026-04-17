package leads.engine;

import leads.exception.LeadException;
import leads.model.Lead;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * LeadWorkflowEngine — enforces valid status transitions for Leads.
 *
 * WHY A WORKFLOW ENGINE?
 *   Leads follow a strict lifecycle. Without validation, a UI bug or bad
 *   input could move a CONVERTED lead back to NEW, corrupting pipeline data.
 *   This engine is the single source of truth for what transitions are allowed.
 *
 * VALID TRANSITIONS:
 *   NEW        → CONTACTED
 *   CONTACTED  → QUALIFIED
 *   QUALIFIED  → CONVERTED (deal created) or LOST (abandoned)
 *   CONVERTED  → (terminal — no further transitions)
 *   LOST       → (terminal — no further transitions)
 *
 * @author Bhumika (Leads + Deals module)
 */
public class LeadWorkflowEngine {

    private static final Logger logger = Logger.getLogger(LeadWorkflowEngine.class.getName());

    /**
     * Validates that transitioning from {@code currentStatus} to {@code newStatus}
     * is a permitted step in the lead lifecycle.
     *
     * @param currentStatus the lead's existing status (from DB)
     * @param newStatus     the requested new status
     * @throws LeadException.LeadUpdateConflict if the transition is not allowed
     */
    public void validateTransition(String currentStatus, String newStatus) {

        // Terminal states — no further transitions allowed
        List<String> terminalStates = Arrays.asList(Lead.STATUS_CONVERTED, Lead.STATUS_LOST);
        if (terminalStates.contains(currentStatus)) {
            throw new LeadException.LeadUpdateConflict(
                "Lead is in terminal state '" + currentStatus + "' and cannot be updated to '" + newStatus + "'."
            );
        }

        // Validate that newStatus is a known, legal status
        List<String> allStatuses = Arrays.asList(
            Lead.STATUS_NEW,
            Lead.STATUS_CONTACTED,
            Lead.STATUS_QUALIFIED,
            Lead.STATUS_CONVERTED,
            Lead.STATUS_LOST
        );
        if (!allStatuses.contains(newStatus)) {
            throw new LeadException.LeadUpdateConflict(
                "Unknown status '" + newStatus + "'. Valid values: " + allStatuses
            );
        }

        // Enforce forward-only progression (except LOST which can come from any active state)
        boolean isValid = isTransitionAllowed(currentStatus, newStatus);
        if (!isValid) {
            throw new LeadException.LeadUpdateConflict(
                "Cannot transition lead from '" + currentStatus + "' to '" + newStatus + "'. "
                + "Check allowed transitions."
            );
        }

        logger.info("LeadWorkflowEngine: transition '" + currentStatus + "' → '" + newStatus + "' approved.");
    }

    /**
     * Validates the initial status when creating a new lead.
     * New leads should always start as NEW.
     *
     * @param status the status to validate
     * @throws LeadException.LeadCreationFailed if the status is not NEW
     */
    public void validateInitialStatus(String status) {
        if (!Lead.STATUS_NEW.equals(status)) {
            throw new LeadException.LeadCreationFailed(
                "New leads must start with status '" + Lead.STATUS_NEW + "'. Got: '" + status + "'."
            );
        }
    }

    /**
     * Validates that the lead name is non-null and non-empty.
     * (Rule from final_db_schema_detailed.pdf: Name should not be empty)
     *
     * @param name the lead name to validate
     * @throws LeadException.LeadCreationFailed if name is blank
     */
    public void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new LeadException.LeadCreationFailed(
                "Lead name must not be empty."
            );
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isTransitionAllowed(String from, String to) {
        switch (from) {
            case Lead.STATUS_NEW:
                // Can only move to CONTACTED or mark as LOST
                return Lead.STATUS_CONTACTED.equals(to) || Lead.STATUS_LOST.equals(to);

            case Lead.STATUS_CONTACTED:
                // Can qualify or mark as LOST
                return Lead.STATUS_QUALIFIED.equals(to) || Lead.STATUS_LOST.equals(to);

            case Lead.STATUS_QUALIFIED:
                // Can convert (becomes a deal) or mark as LOST
                return Lead.STATUS_CONVERTED.equals(to) || Lead.STATUS_LOST.equals(to);

            default:
                return false;
        }
    }
}
