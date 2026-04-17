package quotes.builder;

import quotes.engine.PricingEngine;
import quotes.exception.QuoteException;
import quotes.model.Quote;
import quotes.model.QuoteItem;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * QuoteBuilder — Implements the Builder design pattern for Quote construction.
 *
 * WHY BUILDER?
 *   A Quote has many optional fields (dealId, multiple items, variable discount).
 *   Using a constructor with 6+ parameters is error-prone and unreadable.
 *   The Builder pattern lets callers set only what they need, in any order,
 *   and validate the complete object only when build() is called.
 *
 * USAGE EXAMPLE:
 *   Quote q = new QuoteBuilder()
 *       .forCustomer(101)
 *       .linkedToDeal(5)
 *       .addItem("Enterprise License", 10, 1200.00)
 *       .addItem("Support Package",    1,  500.00)
 *       .withDiscount(15.0)
 *       .build();
 *
 * Exception coverage:
 *   - DISCOUNT_LIMIT_EXCEEDED    → forwarded from PricingEngine
 *   - NEGATIVE_TOTAL_ORDER_VALUE → forwarded from PricingEngine
 *   - QUOTE_GENERATION_FAILED    → thrown if required fields are missing
 *
 * @author Dhatri P Sriram (PES1UG23AM098)
 */
public class QuoteBuilder {

    private static final Logger logger = Logger.getLogger(QuoteBuilder.class.getName());

    // -------------------------------------------------------------------------
    // Builder fields — accumulated via fluent setters
    // -------------------------------------------------------------------------

    private int customerId  = -1;  // -1 = not set yet
    private int dealId      = -1;  // -1 = quote is not linked to a deal (optional)
    private double discount = 0.0; // default: no discount
    private final List<QuoteItem> items = new ArrayList<>();

    /** PricingEngine is used inside build() to compute totals. */
    private final PricingEngine pricingEngine = new PricingEngine();

    // -------------------------------------------------------------------------
    // Fluent builder methods
    // -------------------------------------------------------------------------

    /**
     * Sets the customer this quote is being created for.
     * REQUIRED — build() will throw if this is not set.
     *
     * @param customerId valid ID from the customers table
     * @return this builder (for chaining)
     */
    public QuoteBuilder forCustomer(int customerId) {
        this.customerId = customerId;
        return this;
    }

    /**
     * Links this quote to an existing deal.
     * OPTIONAL — if not called, dealId defaults to -1 (unlinked).
     *
     * @param dealId valid ID from the deals table
     * @return this builder (for chaining)
     */
    public QuoteBuilder linkedToDeal(int dealId) {
        this.dealId = dealId;
        return this;
    }

    /**
     * Sets the discount percentage for this quote.
     * Validated against the 50% cap in PricingEngine.applyDiscount().
     * OPTIONAL — defaults to 0.0 (no discount).
     *
     * @param discountPercent percentage value, e.g. 10.0 means 10%
     * @return this builder (for chaining)
     */
    public QuoteBuilder withDiscount(double discountPercent) {
        this.discount = discountPercent;
        return this;
    }

    /**
     * Adds a line item to this quote.
     * Can be called multiple times to add multiple products/services.
     *
     * @param productName Human-readable product or service name
     * @param quantity    Number of units (must be >= 1)
     * @param unitPrice   Price per unit (must be >= 0)
     * @return this builder (for chaining)
     */
    public QuoteBuilder addItem(String productName, int quantity, double unitPrice) {
        // quoteId is 0 here (not yet assigned by DB); will be set post-insert
        QuoteItem item = new QuoteItem(0, productName, quantity, unitPrice);
        items.add(item);
        logger.fine("QuoteBuilder: added item '" + productName + "' x" + quantity + " @ " + unitPrice);
        return this;
    }

    /**
     * Adds a pre-built QuoteItem object directly.
     * Useful when items are programmatically constructed elsewhere.
     *
     * @param item a fully constructed QuoteItem
     * @return this builder (for chaining)
     */
    public QuoteBuilder addItem(QuoteItem item) {
        items.add(item);
        return this;
    }

    // -------------------------------------------------------------------------
    // Build — validates and constructs the Quote object
    // -------------------------------------------------------------------------

    /**
     * Validates all accumulated fields and returns a fully constructed Quote.
     *
     * Steps:
     *   1. Check required fields (customerId must be set)
     *   2. Delegate to PricingEngine to compute total and apply discount
     *   3. Assemble and return the Quote object
     *
     * @return a validated {@link Quote} ready for DB insertion
     * @throws QuoteException.QuoteGenerationFailed   if customerId is missing
     * @throws QuoteException.DiscountLimitExceeded   if discount > 50%
     * @throws QuoteException.NegativeTotalOrderValue if finalAmount < 0
     * @throws QuoteException.PriceCalculation        if arithmetic fails
     */
    public Quote build() {
        // ---- Step 1: Required field validation ----
        if (customerId == -1) {
            // QUOTE_GENERATION_FAILED: customer is mandatory for any quote
            throw new QuoteException.QuoteGenerationFailed(
                "customerId is required but was not set. Call forCustomer(id) before build()."
            );
        }

        if (items.isEmpty()) {
            // A quote with no line items is meaningless
            throw new QuoteException.QuoteGenerationFailed(
                "Quote must have at least one line item. Call addItem(...) before build()."
            );
        }

        // ---- Step 2: Price computation via PricingEngine ----
        // PricingEngine may throw PRICE_CALCULATION_ERROR, DISCOUNT_LIMIT_EXCEEDED,
        // or NEGATIVE_TOTAL_ORDER_VALUE — all propagate naturally to the caller.
        double[] prices = pricingEngine.computeFinalPrice(items, discount);
        double rawTotal   = prices[0];
        double finalAmount = prices[1];

        // ---- Step 3: Assemble the Quote ----
        Quote quote = new Quote(customerId, dealId, rawTotal, discount, finalAmount);
        quote.setItems(new ArrayList<>(items)); // defensive copy

        logger.info(String.format(
            "QuoteBuilder.build() → customerId=%d, dealId=%d, items=%d, total=%.2f, final=%.2f",
            customerId, dealId, items.size(), rawTotal, finalAmount
        ));

        return quote;
    }
}
