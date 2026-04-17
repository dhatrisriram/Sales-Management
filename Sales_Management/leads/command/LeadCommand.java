package leads.command;

/**
 * Command interface for the Leads module.
 * All lead operations implement this interface (Command Pattern).
 *
 * Follows the same pattern used by Dhatri's QuoteCommand interface
 * as agreed in sales_management_division.pdf.
 */
public interface LeadCommand {
    /**
     * Executes the encapsulated lead operation.
     * Implementations handle their own exception logging and user messages.
     */
    void execute();
}
