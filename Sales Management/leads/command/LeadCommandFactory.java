package leads.command;

import leads.db.LeadDAO;
import leads.engine.LeadWorkflowEngine;
import leads.exception.LeadException;
import leads.model.Lead;

import java.util.List;
import java.util.logging.Logger;

/**
 * ============================================================
 * COMMAND PATTERN — Leads Module
 * ============================================================
 *
 * Structure:
 * LeadCommand (interface)
 * ├── CreateLeadCommand
 * ├── ViewLeadCommand
 * ├── ListLeadsCommand
 * ├── UpdateLeadStatusCommand
 * └── DeleteLeadCommand
 *
 * The UI calls LeadCommandFactory.xxx(...).execute() without
 * knowing any implementation details — decoupled by design.
 *
 * @author Bhumika (Leads + Deals module)
 */

// =============================================================================
// COMMAND 1: Create a new lead
// =============================================================================

/**
 * Validates and persists a new Lead.
 * Handles: ORDER_CREATION_FAILED, validation errors.
 */
class CreateLeadCommand implements LeadCommand {

    private static final Logger logger = Logger.getLogger(CreateLeadCommand.class.getName());

    private final String name;
    private final String company;
    private final LeadDAO leadDAO;
    private final LeadWorkflowEngine workflowEngine;

    public CreateLeadCommand(String name, String company, LeadDAO leadDAO,
                             LeadWorkflowEngine workflowEngine) {
        this.name           = name;
        this.company        = company;
        this.leadDAO        = leadDAO;
        this.workflowEngine = workflowEngine;
    }

    @Override
    public void execute() {
        logger.info("CreateLeadCommand: creating lead for name=" + name);

        try {
            // Validate name (must not be empty — from schema rules)
            workflowEngine.validateName(name);

            // New leads always start at STATUS_NEW
            workflowEngine.validateInitialStatus(Lead.STATUS_NEW);

            Lead lead = new Lead(name, company, Lead.STATUS_NEW);
            leadDAO.createLead(lead);

            System.out.println("  Lead created successfully.");
            System.out.println("  Lead ID : " + lead.getLeadId());
            System.out.println("  Name    : " + lead.getName());
            System.out.println("  Company : " + (lead.getCompany() != null ? lead.getCompany() : "N/A"));
            System.out.println("  Status  : " + lead.getStatus());

        } catch (LeadException.LeadCreationFailed e) {
            System.out.println(" ERROR: " + e.getMessage());
            System.out.println("  Action: Check the input values and retry.");
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// COMMAND 2: View a lead by ID
// =============================================================================

/**
 * Retrieves and displays a single lead by ID.
 * Handles: ORDER_NOT_FOUND.
 */
class ViewLeadCommand implements LeadCommand {

    private static final Logger logger = Logger.getLogger(ViewLeadCommand.class.getName());

    private final int leadId;
    private final LeadDAO leadDAO;

    public ViewLeadCommand(int leadId, LeadDAO leadDAO) {
        this.leadId  = leadId;
        this.leadDAO = leadDAO;
    }

    @Override
    public void execute() {
        logger.info("ViewLeadCommand: fetching leadId=" + leadId);

        try {
            Lead lead = leadDAO.getLeadById(leadId);

            System.out.println("\n=== LEAD DETAILS ===");
            System.out.println("Lead ID    : " + lead.getLeadId());
            System.out.println("Name       : " + lead.getName());
            System.out.println("Company    : " + (lead.getCompany() != null ? lead.getCompany() : "N/A"));
            System.out.println("Status     : " + lead.getStatus());
            System.out.println("Created At : " + lead.getCreatedAt());
            System.out.println("====================\n");

        } catch (LeadException.LeadNotFound e) {
            // ORDER_NOT_FOUND
            System.out.println("!! " + e.getMessage());
            System.out.println("  Action: Verify the Lead ID and try again.");
            logger.warning(e.getMessage());

        } catch (LeadException.LeadCreationFailed e) {
            System.out.println(" ERROR: " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// COMMAND 3: List all leads (optionally filtered by status)
// =============================================================================

/**
 * Lists all leads, or filters by a specific status.
 */
class ListLeadsCommand implements LeadCommand {

    private static final Logger logger = Logger.getLogger(ListLeadsCommand.class.getName());

    private final String statusFilter; // null = show all
    private final LeadDAO leadDAO;

    public ListLeadsCommand(String statusFilter, LeadDAO leadDAO) {
        this.statusFilter = statusFilter;
        this.leadDAO      = leadDAO;
    }

    @Override
    public void execute() {
        logger.info("ListLeadsCommand: statusFilter=" + statusFilter);

        try {
            List<Lead> leads = (statusFilter == null)
                ? leadDAO.getAllLeads()
                : leadDAO.getLeadsByStatus(statusFilter);

            if (leads.isEmpty()) {
                System.out.println(" No leads found" + (statusFilter != null ? " for status: " + statusFilter : "") + ".");
                return;
            }

            System.out.println("\n=== LEADS LIST" + (statusFilter != null ? " [" + statusFilter + "]" : "") + " ===");
            System.out.printf("%-6s %-25s %-25s %-15s%n", "ID", "Name", "Company", "Status");
            System.out.println("-".repeat(75));

            for (Lead lead : leads) {
                System.out.printf("%-6d %-25s %-25s %-15s%n",
                    lead.getLeadId(),
                    lead.getName(),
                    lead.getCompany() != null ? lead.getCompany() : "N/A",
                    lead.getStatus()
                );
            }

            System.out.println("-".repeat(75));
            System.out.println("Total: " + leads.size() + " lead(s)\n");

        } catch (LeadException.LeadCreationFailed e) {
            System.out.println(" ERROR: " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// COMMAND 4: Update lead status (with workflow validation)
// =============================================================================

/**
 * Moves a lead to the next status in the lifecycle.
 * Validates transition via LeadWorkflowEngine before writing to DB.
 *
 * Handles: ORDER_NOT_FOUND, ORDER_UPDATE_CONFLICT.
 */
class UpdateLeadStatusCommand implements LeadCommand {

    private static final Logger logger = Logger.getLogger(UpdateLeadStatusCommand.class.getName());

    private final int leadId;
    private final String newStatus;
    private final LeadDAO leadDAO;
    private final LeadWorkflowEngine workflowEngine;

    public UpdateLeadStatusCommand(int leadId, String newStatus,
                                   LeadDAO leadDAO, LeadWorkflowEngine workflowEngine) {
        this.leadId         = leadId;
        this.newStatus      = newStatus;
        this.leadDAO        = leadDAO;
        this.workflowEngine = workflowEngine;
    }

    @Override
    public void execute() {
        logger.info("UpdateLeadStatusCommand: leadId=" + leadId + ", newStatus=" + newStatus);

        try {
            // Fetch current lead to get its existing status
            Lead existing = leadDAO.getLeadById(leadId);

            // Validate the transition using workflow engine
            workflowEngine.validateTransition(existing.getStatus(), newStatus);

            // Persist the new status
            boolean updated = leadDAO.updateLeadStatus(leadId, newStatus);

            if (updated) {
                System.out.printf(" Lead %d status updated: '%s' → '%s'%n",
                    leadId, existing.getStatus(), newStatus);
            } else {
                System.out.println("⚠ No update made. Lead ID not found: " + leadId);
            }

        } catch (LeadException.LeadNotFound e) {
            // ORDER_NOT_FOUND
            System.out.println("!! " + e.getMessage());
            System.out.println("  Action: Verify the Lead ID.");
            logger.warning(e.getMessage());

        } catch (LeadException.LeadUpdateConflict e) {
            // ORDER_UPDATE_CONFLICT
            System.out.println("!! CONFLICT: " + e.getMessage());
            System.out.println("  Action: Check the allowed status transitions.");
            logger.warning(e.getMessage());

        } catch (LeadException.LeadCreationFailed e) {
            System.out.println(" ERROR: " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// COMMAND 5: Delete a lead
// =============================================================================

/**
 * Deletes a lead by ID.
 * Handles: ORDER_NOT_FOUND, DB errors.
 */
class DeleteLeadCommand implements LeadCommand {

    private static final Logger logger = Logger.getLogger(DeleteLeadCommand.class.getName());

    private final int leadId;
    private final LeadDAO leadDAO;

    public DeleteLeadCommand(int leadId, LeadDAO leadDAO) {
        this.leadId  = leadId;
        this.leadDAO = leadDAO;
    }

    @Override
    public void execute() {
        logger.info("DeleteLeadCommand: deleting leadId=" + leadId);

        try {
            boolean deleted = leadDAO.deleteLead(leadId);

            if (deleted) {
                System.out.println(" Lead " + leadId + " deleted successfully.");
            } else {
                System.out.println(" Lead not found for ID: " + leadId + ". Nothing deleted.");
            }

        } catch (LeadException.LeadCreationFailed e) {
            System.out.println("✘ ERROR: Could not delete lead. " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// PUBLIC FACTORY
// =============================================================================

/**
 * LeadCommandFactory
 *
 * The UI layer uses this factory to create and execute commands without
 * importing concrete command classes directly.
 *
 * Example usage:
 *   LeadDAO dao = new LeadDAO();
 *   LeadWorkflowEngine engine = new LeadWorkflowEngine();
 *
 *   LeadCommandFactory.createLead("Ananya Sharma", "TechCorp", dao, engine).execute();
 *   LeadCommandFactory.updateStatus(3, "QUALIFIED", dao, engine).execute();
 */
public class LeadCommandFactory {

    private LeadCommandFactory() {} // no instantiation

    public static LeadCommand createLead(String name, String company,
                                         LeadDAO dao, LeadWorkflowEngine engine) {
        return new CreateLeadCommand(name, company, dao, engine);
    }

    public static LeadCommand viewLead(int leadId, LeadDAO dao) {
        return new ViewLeadCommand(leadId, dao);
    }

    public static LeadCommand listLeads(String statusFilter, LeadDAO dao) {
        return new ListLeadsCommand(statusFilter, dao);
    }

    public static LeadCommand listAllLeads(LeadDAO dao) {
        return new ListLeadsCommand(null, dao);
    }

    public static LeadCommand updateStatus(int leadId, String newStatus,
                                           LeadDAO dao, LeadWorkflowEngine engine) {
        return new UpdateLeadStatusCommand(leadId, newStatus, dao, engine);
    }

    public static LeadCommand deleteLead(int leadId, LeadDAO dao) {
        return new DeleteLeadCommand(leadId, dao);
    }
}
