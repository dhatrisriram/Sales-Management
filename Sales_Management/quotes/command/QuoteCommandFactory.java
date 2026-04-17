package quotes.command;

import quotes.builder.QuoteBuilder;
import quotes.db.QuoteDAO;
import quotes.exception.QuoteException;
import quotes.model.Quote;
import quotes.model.QuoteItem;

import java.util.List;
import java.util.logging.Logger;

/**
 * ============================================================
 * COMMAND PATTERN — Quotes Module
 * ============================================================
 *
 * WHY COMMAND PATTERN?
 * The Command pattern decouples the UI (which triggers an action)
 * from the logic that executes it. This is the agreed design pattern
 * for all team members' Command UIs (sales_management_division.pdf).
 *
 * Benefits:
 * - The UI calls command.execute() without knowing the internals
 * - Commands can be logged, queued, or undone
 * - Easy to add new operations without changing the UI layer
 *
 * STRUCTURE:
 * QuoteCommand (interface) — contract for all commands
 * ├── CreateQuoteCommand
 * ├── ViewQuoteCommand
 * ├── UpdateDiscountCommand
 * └── DeleteQuoteCommand
 *
 * @author Dhatri P Sriram (PES1UG23AM098)
 */

// =============================================================================
// COMMAND 1: Create a new quote via QuoteBuilder
// =============================================================================

/**
 * Builds and persists a new Quote using the Builder + DAO.
 *
 * Handles:
 * DISCOUNT_LIMIT_EXCEEDED → prints warning, stops creation
 * NEGATIVE_TOTAL_ORDER_VALUE → prints error, stops creation
 * QUOTE_GENERATION_FAILED → prints error, stops creation
 */
class CreateQuoteCommand implements QuoteCommand {

    private static final Logger logger = Logger.getLogger(CreateQuoteCommand.class.getName());

    private final int customerId;
    private final int dealId;
    private final List<QuoteItem> items;
    private final double discountPercent;
    private final QuoteDAO quoteDAO;

    /**
     * @param customerId      ID of the customer (required)
     * @param dealId          ID of the deal (-1 if not linked)
     * @param items           Pre-built list of QuoteItems
     * @param discountPercent Discount percentage (0–50)
     * @param quoteDAO        Shared DAO instance injected by the UI
     */
    public CreateQuoteCommand(int customerId, int dealId, List<QuoteItem> items,
            double discountPercent, QuoteDAO quoteDAO) {
        this.customerId = customerId;
        this.dealId = dealId;
        this.items = items;
        this.discountPercent = discountPercent;
        this.quoteDAO = quoteDAO;
    }

    @Override
    public void execute() {
        logger.info("CreateQuoteCommand: starting for customerId=" + customerId);

        try {
            // Build the Quote using Builder pattern
            QuoteBuilder builder = new QuoteBuilder()
                    .forCustomer(customerId)
                    .linkedToDeal(dealId)
                    .withDiscount(discountPercent);

            // Add each item through the builder
            for (QuoteItem item : items) {
                builder.addItem(item);
            }

            // build() triggers PricingEngine validation
            Quote quote = builder.build();

            // Persist to DB
            quoteDAO.createQuote(quote);

            System.out.println("✔ Quote created successfully. Quote ID: " + quote.getQuoteId());
            System.out.printf("  Total: ₹%.2f  |  Discount: %.1f%%  |  Final: ₹%.2f%n",
                    quote.getTotalAmount(), quote.getDiscount(), quote.getFinalAmount());

        } catch (QuoteException.DiscountLimitExceeded e) {
            // DISCOUNT_LIMIT_EXCEEDED — warn and stop; user must request admin approval
            System.out.println("⚠ WARNING: " + e.getMessage());
            System.out.println("  Action required: Request admin approval for discount > 50%.");
            logger.warning(e.getMessage());

        } catch (QuoteException.NegativeTotalOrderValue e) {
            // NEGATIVE_TOTAL_ORDER_VALUE — block entirely
            System.out.println("✘ ERROR: " + e.getMessage());
            System.out.println("  Action: Reduce discount or adjust item pricing.");
            logger.severe(e.getMessage());

        } catch (QuoteException.QuoteGenerationFailed e) {
            // QUOTE_GENERATION_FAILED — could not write to DB
            System.out.println("✘ ERROR: " + e.getMessage());
            System.out.println("  Action: Retry or generate the quote manually.");
            logger.severe(e.getMessage());

        } catch (QuoteException.PriceCalculation e) {
            // PRICE_CALCULATION_ERROR — arithmetic anomaly
            System.out.println("✘ ERROR: " + e.getMessage());
            System.out.println("  Action: Check item quantities and unit prices.");
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// COMMAND 2: View a quote by ID
// =============================================================================

/**
 * Retrieves and displays a quote (with all line items) by its ID.
 */
class ViewQuoteCommand implements QuoteCommand {

    private static final Logger logger = Logger.getLogger(ViewQuoteCommand.class.getName());

    private final int quoteId;
    private final QuoteDAO quoteDAO;

    public ViewQuoteCommand(int quoteId, QuoteDAO quoteDAO) {
        this.quoteId = quoteId;
        this.quoteDAO = quoteDAO;
    }

    @Override
    public void execute() {
        logger.info("ViewQuoteCommand: fetching quoteId=" + quoteId);

        try {
            Quote quote = quoteDAO.getQuoteById(quoteId);

            if (quote == null) {
                System.out.println("⚠ Quote not found for ID: " + quoteId);
                return;
            }

            // ---- Display header ----
            System.out.println("\n=== QUOTE DETAILS ===");
            System.out.println("Quote ID    : " + quote.getQuoteId());
            System.out.println("Customer ID : " + quote.getCustomerId());
            System.out.println("Deal ID     : " + (quote.getDealId() == -1 ? "N/A" : quote.getDealId()));
            System.out.printf("Created At  : %s%n", quote.getCreatedAt());

            // ---- Display line items ----
            System.out.println("\n  Line Items:");
            System.out.printf("  %-30s %6s %12s %12s%n", "Product", "Qty", "Unit Price", "Subtotal");
            System.out.println("  " + "-".repeat(65));

            for (QuoteItem item : quote.getItems()) {
                System.out.printf("  %-30s %6d %12.2f %12.2f%n",
                        item.getProductName(), item.getQuantity(),
                        item.getPrice(), item.getSubtotal());
            }

            // ---- Display totals ----
            System.out.println("  " + "-".repeat(65));
            System.out.printf("  %-48s %12.2f%n", "Subtotal:", quote.getTotalAmount());
            System.out.printf("  %-48s %12.1f%%%n", "Discount:", quote.getDiscount());
            System.out.printf("  %-48s %12.2f%n", "GRAND TOTAL:", quote.getFinalAmount());
            System.out.println("=====================\n");

        } catch (QuoteException.QuoteGenerationFailed e) {
            System.out.println("✘ ERROR: Could not retrieve quote. " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// COMMAND 3: Update discount on an existing quote
// =============================================================================

/**
 * Updates the discount of an existing quote.
 * Re-validates via PricingEngine before writing to DB.
 *
 * Scenario: Admin has approved a discount override after
 * DISCOUNT_LIMIT_EXCEEDED.
 */
class UpdateDiscountCommand implements QuoteCommand {

    private static final Logger logger = Logger.getLogger(UpdateDiscountCommand.class.getName());

    private final int quoteId;
    private final double newDiscount;
    private final QuoteDAO quoteDAO;

    public UpdateDiscountCommand(int quoteId, double newDiscount, QuoteDAO quoteDAO) {
        this.quoteId = quoteId;
        this.newDiscount = newDiscount;
        this.quoteDAO = quoteDAO;
    }

    @Override
    public void execute() {
        logger.info("UpdateDiscountCommand: quoteId=" + quoteId + ", newDiscount=" + newDiscount);

        try {
            // Load the current quote to get the original total
            Quote existing = quoteDAO.getQuoteById(quoteId);

            if (existing == null) {
                System.out.println("⚠ Quote not found for ID: " + quoteId);
                return;
            }

            // Re-run pricing engine to compute new final amount
            // (Validates discount and checks for negative total)
            quotes.engine.PricingEngine engine = new quotes.engine.PricingEngine();
            double newFinal = engine.applyDiscount(existing.getTotalAmount(), newDiscount);

            boolean updated = quoteDAO.updateQuoteDiscount(quoteId, newDiscount, newFinal);

            if (updated) {
                System.out.printf("✔ Quote %d updated. New discount: %.1f%%, New final: ₹%.2f%n",
                        quoteId, newDiscount, newFinal);
            } else {
                System.out.println("⚠ No changes made. Quote ID not found: " + quoteId);
            }

        } catch (QuoteException.DiscountLimitExceeded e) {
            System.out.println("⚠ WARNING: " + e.getMessage());
            System.out.println("  Even with admin override, this discount is above 50%. Aborting.");
            logger.warning(e.getMessage());

        } catch (QuoteException.NegativeTotalOrderValue e) {
            System.out.println("✘ ERROR: " + e.getMessage());
            logger.severe(e.getMessage());

        } catch (QuoteException.QuoteGenerationFailed e) {
            System.out.println("✘ DB ERROR: " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// COMMAND 4: Delete a quote
// =============================================================================

/**
 * Deletes a quote (and its items) from the database.
 */
class DeleteQuoteCommand implements QuoteCommand {

    private static final Logger logger = Logger.getLogger(DeleteQuoteCommand.class.getName());

    private final int quoteId;
    private final QuoteDAO quoteDAO;

    public DeleteQuoteCommand(int quoteId, QuoteDAO quoteDAO) {
        this.quoteId = quoteId;
        this.quoteDAO = quoteDAO;
    }

    @Override
    public void execute() {
        logger.info("DeleteQuoteCommand: deleting quoteId=" + quoteId);

        try {
            boolean deleted = quoteDAO.deleteQuote(quoteId);

            if (deleted) {
                System.out.println("✔ Quote " + quoteId + " and its items deleted successfully.");
            } else {
                System.out.println("⚠ Quote not found for ID: " + quoteId + ". Nothing deleted.");
            }

        } catch (QuoteException.QuoteGenerationFailed e) {
            System.out.println("✘ ERROR: Could not delete quote. " + e.getMessage());
            logger.severe(e.getMessage());
        }
    }
}

// =============================================================================
// PUBLIC FACTORY — exposes command creation to the UI layer
// =============================================================================

/**
 * QuoteCommandFactory
 *
 * The UI layer uses this factory to create commands without importing
 * the concrete command classes directly. This further decouples the UI
 * from the implementation and makes it easy to swap or extend commands.
 *
 * Example (from QuoteUI):
 * QuoteCommand cmd = QuoteCommandFactory.createQuote(101, -1, items, 10.0,
 * dao);
 * cmd.execute();
 */
public class QuoteCommandFactory {

    // Prevent instantiation — this is a static factory class
    private QuoteCommandFactory() {
    }

    public static QuoteCommand createQuote(int customerId, int dealId,
            List<QuoteItem> items, double discount,
            QuoteDAO dao) {
        return new CreateQuoteCommand(customerId, dealId, items, discount, dao);
    }

    public static QuoteCommand viewQuote(int quoteId, QuoteDAO dao) {
        return new ViewQuoteCommand(quoteId, dao);
    }

    public static QuoteCommand updateDiscount(int quoteId, double newDiscount, QuoteDAO dao) {
        return new UpdateDiscountCommand(quoteId, newDiscount, dao);
    }

    public static QuoteCommand deleteQuote(int quoteId, QuoteDAO dao) {
        return new DeleteQuoteCommand(quoteId, dao);
    }
}
