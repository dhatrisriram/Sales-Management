package deals.command;

import deals.db.DealDAO;
import deals.engine.DealWorkflowEngine;
import deals.exception.DealException;
import deals.model.Deal;

import java.util.List;
import java.util.logging.Logger;

/**
 * ============================================================
 * COMMAND PATTERN — Deals Module
 * ============================================================
 *
 * Structure:
 * DealCommand (interface)
 * ├── CreateDealCommand
 * ├── ViewDealCommand
 * ├── ListDealsByStageCommand
 * ├── AdvanceDealStageCommand
 * └── DeleteDealCommand
 *
 * @author Bhumika (Leads + Deals module)
 */

// =============================================================================
// COMMAND 1: Create a new deal
// =============================================================================

/**
 * Validates and persists a new Deal.
 * Handles: ORDER_CREATION_FAILED, validation errors.
 */
class CreateDealCommand implements DealCommand {

    private static final Logger logger = Logger.getLogger(CreateDealCommand.class.getName());

    private final int customerId;
    private final double amount;
    private final DealDAO dealDAO;
    private final DealWorkflowEngine workflowEngine;

    public CreateDealCommand(int customerId, double amount,
                             DealDAO dealDAO, DealWorkflowEngine workflowEngine) {
        this.customerId    = customerId;
        this.amount        = amount;
        this.dealDAO       = dealDAO;
        this.workflowEngine = workflowEngine;
    }

    @Override
    public void execute() {
        logger.info("CreateDealCommand: customerId=" + customerId + ", amount=" + amount);

        try {
            // Validate amount >= 0
            workflowEngine.validateAmount(amount);

            // New deals start at PROSPECTING + ACTIVE
            workflowEngine.validateInitialStage(Deal.STAGE_PROSPECTING);

            Deal deal = new Deal(customerId, amount, Deal.STAGE_PROSPECTING, Deal.STATUS_ACTIVE);
            dealDAO.createDeal(deal);

            System.out.println(" Deal created successfully.");
            System.out.println("  Deal ID     : " + deal.getDealId());
            System.out.println("  Customer ID : " + deal.getCustomerId());
            System.out.printf( "  Amount      : Rs%.2f%n", deal.getAmount());
            System.out.println("  Stage       : " + deal.getStage());
            System.out.println("  Status      : " + deal.getStatus());

        } catch (DealException.DealCreationFailed e) {
            // ORDER_CREATION_FAILED
            System.out.println(" ERROR: " + e.getMessage());
            System.out.println("  Action: Check customer ID and amount, then retry.");
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// COMMAND 2: View a deal by ID
// =============================================================================

/**
 * Retrieves and displays a single deal by ID.
 * Handles: ORDER_NOT_FOUND.
 */
class ViewDealCommand implements DealCommand {

    private static final Logger logger = Logger.getLogger(ViewDealCommand.class.getName());

    private final int dealId;
    private final DealDAO dealDAO;

    public ViewDealCommand(int dealId, DealDAO dealDAO) {
        this.dealId  = dealId;
        this.dealDAO = dealDAO;
    }

    @Override
    public void execute() {
        logger.info("ViewDealCommand: fetching dealId=" + dealId);

        try {
            Deal deal = dealDAO.getDealById(dealId);

            System.out.println("\n=== DEAL DETAILS ===");
            System.out.println("Deal ID     : " + deal.getDealId());
            System.out.println("Customer ID : " + deal.getCustomerId());
            System.out.printf( "Amount      : Rs%.2f%n", deal.getAmount());
            System.out.println("Stage       : " + deal.getStage());
            System.out.println("Status      : " + deal.getStatus());
            System.out.println("Created At  : " + deal.getCreatedAt());
            System.out.println("====================\n");

        } catch (DealException.DealNotFound e) {
            // ORDER_NOT_FOUND
            System.out.println("!!" + e.getMessage());
            System.out.println("  Action: Verify the Deal ID and try again.");
            logger.warning(e.getMessage());

        } catch (DealException.DealCreationFailed e) {
            System.out.println(" DB ERROR: " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// COMMAND 3: List deals by pipeline stage
// =============================================================================

/**
 * Lists all deals in a given pipeline stage (or all if stage is null).
 */
class ListDealsByStageCommand implements DealCommand {

    private static final Logger logger = Logger.getLogger(ListDealsByStageCommand.class.getName());

    private final int customerId;     // -1 = all customers
    private final String stageFilter; // null = all stages
    private final DealDAO dealDAO;

    public ListDealsByStageCommand(int customerId, String stageFilter, DealDAO dealDAO) {
        this.customerId  = customerId;
        this.stageFilter = stageFilter;
        this.dealDAO     = dealDAO;
    }

    @Override
    public void execute() {
        logger.info("ListDealsByStageCommand: customerId=" + customerId + ", stage=" + stageFilter);

        try {
            List<Deal> deals;

            if (customerId > 0) {
                deals = dealDAO.getDealsByCustomer(customerId);
            } else if (stageFilter != null) {
                deals = dealDAO.getDealsByStage(stageFilter);
            } else {
                // No filter — get by a broad query; for simplicity, show by stage PROSPECTING
                deals = dealDAO.getDealsByStage(Deal.STAGE_PROSPECTING);
            }

            if (deals.isEmpty()) {
                System.out.println("No deals found.");
                return;
            }

            System.out.println("\n=== DEALS" + (stageFilter != null ? " [" + stageFilter + "]" : "") + " ===");
            System.out.printf("%-6s %-10s %15s %-18s %-12s%n",
                "ID", "CustID", "Amount (Rs)", "Stage", "Status");
            System.out.println("-".repeat(65));

            for (Deal deal : deals) {
                System.out.printf("%-6d %-10d %15.2f %-18s %-12s%n",
                    deal.getDealId(), deal.getCustomerId(),
                    deal.getAmount(), deal.getStage(), deal.getStatus()
                );
            }

            System.out.println("-".repeat(65));
            System.out.println("Total: " + deals.size() + " deal(s)\n");

        } catch (DealException.DealCreationFailed e) {
            System.out.println(" DB ERROR: " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// COMMAND 4: Advance deal to next pipeline stage
// =============================================================================

/**
 * Moves a deal forward in the pipeline (or marks it CLOSED_LOST).
 * Uses DealWorkflowEngine to validate the transition before DB write.
 *
 * Handles: ORDER_NOT_FOUND, ORDER_UPDATE_CONFLICT.
 */
class AdvanceDealStageCommand implements DealCommand {

    private static final Logger logger = Logger.getLogger(AdvanceDealStageCommand.class.getName());

    private final int dealId;
    private final String newStage;
    private final DealDAO dealDAO;
    private final DealWorkflowEngine workflowEngine;

    public AdvanceDealStageCommand(int dealId, String newStage,
                                   DealDAO dealDAO, DealWorkflowEngine workflowEngine) {
        this.dealId         = dealId;
        this.newStage       = newStage;
        this.dealDAO        = dealDAO;
        this.workflowEngine = workflowEngine;
    }

    @Override
    public void execute() {
        logger.info("AdvanceDealStageCommand: dealId=" + dealId + ", newStage=" + newStage);

        try {
            // Fetch current deal
            Deal existing = dealDAO.getDealById(dealId);

            // Validate stage transition
            workflowEngine.validateStageTransition(existing.getStage(), newStage);

            // Determine new status based on stage
            String newStatus = resolveStatus(newStage);

            // Persist
            boolean updated = dealDAO.updateDealStage(dealId, newStage, newStatus);

            if (updated) {
                System.out.printf(" Deal %d advanced: '%s' -> '%s' (Status: %s)%n",
                    dealId, existing.getStage(), newStage, newStatus);
            } else {
                System.out.println(" No update made. Deal ID not found: " + dealId);
            }

        } catch (DealException.DealNotFound e) {
            // ORDER_NOT_FOUND
            System.out.println("!! " + e.getMessage());
            System.out.println("  Action: Verify the Deal ID.");
            logger.warning(e.getMessage());

        } catch (DealException.DealUpdateConflict e) {
            // ORDER_UPDATE_CONFLICT
            System.out.println(" CONFLICT: " + e.getMessage());
            System.out.println("  Action: Review the allowed pipeline stage transitions.");
            logger.warning(e.getMessage());

        } catch (DealException.DealCreationFailed e) {
            System.out.println(" DB ERROR: " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }

    /**
     * Maps a pipeline stage to its corresponding status string.
     */
    private String resolveStatus(String stage) {
        switch (stage) {
            case Deal.STAGE_CLOSED_WON:  return Deal.STATUS_WON;
            case Deal.STAGE_CLOSED_LOST: return Deal.STATUS_LOST;
            default:                     return Deal.STATUS_ACTIVE;
        }
    }
}

// =============================================================================
// COMMAND 5: Delete a deal
// =============================================================================

/**
 * Deletes a deal by ID.
 * Handles: ORDER_NOT_FOUND, FK constraint violations.
 */
class DeleteDealCommand implements DealCommand {

    private static final Logger logger = Logger.getLogger(DeleteDealCommand.class.getName());

    private final int dealId;
    private final DealDAO dealDAO;

    public DeleteDealCommand(int dealId, DealDAO dealDAO) {
        this.dealId  = dealId;
        this.dealDAO = dealDAO;
    }

    @Override
    public void execute() {
        logger.info("DeleteDealCommand: deleting dealId=" + dealId);

        try {
            boolean deleted = dealDAO.deleteDeal(dealId);

            if (deleted) {
                System.out.println(" Deal " + dealId + " deleted successfully.");
            } else {
                System.out.println(" Deal not found for ID: " + dealId + ". Nothing deleted.");
            }

        } catch (DealException.DealCreationFailed e) {
            System.out.println(" ERROR: " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// PUBLIC FACTORY
// =============================================================================

/**
 * DealCommandFactory
 *
 * Example usage:
 *   DealDAO dao = new DealDAO();
 *   DealWorkflowEngine engine = new DealWorkflowEngine();
 *
 *   DealCommandFactory.createDeal(1, 50000.0, dao, engine).execute();
 *   DealCommandFactory.advanceStage(2, "PROPOSAL", dao, engine).execute();
 */
public class DealCommandFactory {

    private DealCommandFactory() {}

    public static DealCommand createDeal(int customerId, double amount,
                                         DealDAO dao, DealWorkflowEngine engine) {
        return new CreateDealCommand(customerId, amount, dao, engine);
    }

    public static DealCommand viewDeal(int dealId, DealDAO dao) {
        return new ViewDealCommand(dealId, dao);
    }

    public static DealCommand listByStage(String stage, DealDAO dao) {
        return new ListDealsByStageCommand(-1, stage, dao);
    }

    public static DealCommand listByCustomer(int customerId, DealDAO dao) {
        return new ListDealsByStageCommand(customerId, null, dao);
    }

    public static DealCommand advanceStage(int dealId, String newStage,
                                           DealDAO dao, DealWorkflowEngine engine) {
        return new AdvanceDealStageCommand(dealId, newStage, dao, engine);
    }

    public static DealCommand deleteDeal(int dealId, DealDAO dao) {
        return new DeleteDealCommand(dealId, dao);
    }
}
