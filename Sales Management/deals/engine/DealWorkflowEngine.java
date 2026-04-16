package deals.engine;

import deals.exception.DealException;
import deals.model.Deal;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * DealWorkflowEngine — enforces valid pipeline stage transitions for Deals.
 *
 * WHY A WORKFLOW ENGINE?
 *   Sales pipeline integrity depends on deals moving forward through stages.
 *   Allowing backward transitions (e.g., CLOSED_WON → PROSPECTING) would
 *   corrupt forecast reports and pipeline metrics.
 *
 * VALID PIPELINE ORDER:
 *   PROSPECTING → QUALIFICATION → PROPOSAL → NEGOTIATION → CLOSED_WON
 *                                                         → CLOSED_LOST  (from any active stage)
 *
 * @author Bhumika (Leads + Deals module)
 */
public class DealWorkflowEngine {

    private static final Logger logger = Logger.getLogger(DealWorkflowEngine.class.getName());

    // Pipeline order for forward-progression checks
    private static final List<String> PIPELINE_ORDER = Arrays.asList(
        Deal.STAGE_PROSPECTING,
        Deal.STAGE_QUALIFICATION,
        Deal.STAGE_PROPOSAL,
        Deal.STAGE_NEGOTIATION,
        Deal.STAGE_CLOSED_WON
    );

    /**
     * Validates a deal stage transition.
     *
     * Rules:
     *   - CLOSED_WON and CLOSED_LOST are terminal; no further moves allowed.
     *   - CLOSED_LOST can be reached from any active stage.
     *   - All other transitions must move forward in PIPELINE_ORDER.
     *
     * @param currentStage the deal's existing stage
     * @param newStage     the requested new stage
     * @throws DealException.DealUpdateConflict if the transition is not allowed
     */
    public void validateStageTransition(String currentStage, String newStage) {

        // Terminal stages — cannot leave
        List<String> terminalStages = Arrays.asList(Deal.STAGE_CLOSED_WON, Deal.STAGE_CLOSED_LOST);
        if (terminalStages.contains(currentStage)) {
            throw new DealException.DealUpdateConflict(
                "Deal is in terminal stage '" + currentStage + "' and cannot move to '" + newStage + "'."
            );
        }

        // CLOSED_LOST is always allowed from any active stage
        if (Deal.STAGE_CLOSED_LOST.equals(newStage)) {
            logger.info("DealWorkflowEngine: deal marked CLOSED_LOST from '" + currentStage + "'.");
            return;
        }

        // Validate newStage is a known stage
        List<String> allStages = Arrays.asList(
            Deal.STAGE_PROSPECTING, Deal.STAGE_QUALIFICATION,
            Deal.STAGE_PROPOSAL, Deal.STAGE_NEGOTIATION,
            Deal.STAGE_CLOSED_WON, Deal.STAGE_CLOSED_LOST
        );
        if (!allStages.contains(newStage)) {
            throw new DealException.DealUpdateConflict(
                "Unknown stage '" + newStage + "'. Valid stages: " + allStages
            );
        }

        // Enforce forward-only movement through the pipeline
        int currentIndex = PIPELINE_ORDER.indexOf(currentStage);
        int newIndex     = PIPELINE_ORDER.indexOf(newStage);

        if (newIndex <= currentIndex) {
            throw new DealException.DealUpdateConflict(
                "Cannot move deal backward from '" + currentStage + "' to '" + newStage + "'. "
                + "Pipeline moves forward only."
            );
        }

        logger.info("DealWorkflowEngine: stage transition '" + currentStage + "' → '" + newStage + "' approved.");
    }

    /**
     * Validates that a deal amount is non-negative.
     * (from final_db_schema_detailed.pdf: Total >= 0)
     *
     * @param amount the deal value to validate
     * @throws DealException.DealCreationFailed if amount is negative
     */
    public void validateAmount(double amount) {
        if (amount < 0) {
            throw new DealException.DealCreationFailed(
                "Deal amount must be >= 0. Got: " + amount
            );
        }
    }

    /**
     * Validates the initial stage for a new deal.
     * All new deals must start at PROSPECTING.
     *
     * @param stage the initial stage to validate
     * @throws DealException.DealCreationFailed if stage is not PROSPECTING
     */
    public void validateInitialStage(String stage) {
        if (!Deal.STAGE_PROSPECTING.equals(stage)) {
            throw new DealException.DealCreationFailed(
                "New deals must start at stage '" + Deal.STAGE_PROSPECTING + "'. Got: '" + stage + "'."
            );
        }
    }
}
