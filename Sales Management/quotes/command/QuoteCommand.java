package quotes.command;

/**
 * Command interface for the Quotes module.
 * All quote operations implement this interface.
 */
public interface QuoteCommand {
    /**
     * Executes the encapsulated quote operation.
     * Implementations handle their own exception logging.
     */
    void execute();
}