package deals.command;

/**
 * Command interface for the Deals module.
 * All deal operations implement this interface (Command Pattern).
 */
public interface DealCommand {
    /**
     * Executes the encapsulated deal operation.
     * Implementations handle their own exception logging and user messages.
     */
    void execute();
}
