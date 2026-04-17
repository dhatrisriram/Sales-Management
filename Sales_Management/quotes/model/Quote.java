package quotes.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Quote entity in the Sales Management System.
 *
 * Maps directly to the `quotes` table in the `polymorphs` database.
 * A Quote is linked to a Customer and optionally to a Deal.
 * It contains multiple QuoteItems and holds pricing information.
 *
 * Schema reference:
 *   quotes(quote_id, customer_id, deal_id, total_amount, discount, final_amount, created_at)
 *
 * @author Dhatri P Sriram (PES1UG23AM098)
 */
public class Quote {

    // -------------------------------------------------------------------------
    // Fields — mapped to DB columns
    // -------------------------------------------------------------------------

    /** Primary key — auto-incremented by the database. */
    private int quoteId;

    /** Foreign key → customers(customer_id). Required. */
    private int customerId;

    /**
     * Foreign key → deals(deal_id).
     * -1 indicates that this quote is not linked to any deal.
     */
    private int dealId;

    /** Sum of (quantity × unit price) across all line items, before discount. */
    private double totalAmount;

    /**
     * Discount percentage applied to this quote.
     * Validated to be in the range [0, 50] (see: DISCOUNT_LIMIT_EXCEEDED exception).
     */
    private double discount;

    /** Final payable amount after applying discount. Must be >= 0. */
    private double finalAmount;

    /** Timestamp of quote creation — set by DB default, mirrored here for display. */
    private LocalDateTime createdAt;

    /**
     * Line items belonging to this quote.
     * Not stored in the quotes table itself — stored in quote_items.
     */
    private List<QuoteItem> items;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Default constructor — used when loading from DB or via Builder. */
    public Quote() {
        this.items = new ArrayList<>();
        this.dealId = -1; // unlinked by default
    }

    /**
     * Full constructor for creating a new quote (before DB insertion).
     *
     * @param customerId  ID of the customer this quote belongs to
     * @param dealId      ID of the associated deal (-1 if none)
     * @param totalAmount Raw total before discount
     * @param discount    Discount percentage (0–50)
     * @param finalAmount Amount after discount is applied
     */
    public Quote(int customerId, int dealId, double totalAmount, double discount, double finalAmount) {
        this.customerId  = customerId;
        this.dealId      = dealId;
        this.totalAmount = totalAmount;
        this.discount    = discount;
        this.finalAmount = finalAmount;
        this.items       = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public int getQuoteId()                     { return quoteId; }
    public void setQuoteId(int quoteId)         { this.quoteId = quoteId; }

    public int getCustomerId()                  { return customerId; }
    public void setCustomerId(int customerId)   { this.customerId = customerId; }

    public int getDealId()                      { return dealId; }
    public void setDealId(int dealId)           { this.dealId = dealId; }

    public double getTotalAmount()              { return totalAmount; }
    public void setTotalAmount(double total)    { this.totalAmount = total; }

    public double getDiscount()                 { return discount; }
    public void setDiscount(double discount)    { this.discount = discount; }

    public double getFinalAmount()              { return finalAmount; }
    public void setFinalAmount(double final_)   { this.finalAmount = final_; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }

    public List<QuoteItem> getItems()                       { return items; }
    public void setItems(List<QuoteItem> items)             { this.items = items; }

    /** Convenience method to add a single line item to this quote. */
    public void addItem(QuoteItem item) {
        this.items.add(item);
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format(
            "Quote[id=%d, customerId=%d, dealId=%d, total=%.2f, discount=%.1f%%, final=%.2f, items=%d]",
            quoteId, customerId, dealId, totalAmount, discount, finalAmount, items.size()
        );
    }
}
