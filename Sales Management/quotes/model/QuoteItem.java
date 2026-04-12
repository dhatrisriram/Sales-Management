package quotes.model;

/**
 * Represents a single line item within a Quote.
 *
 * Maps to the `quote_items` table in the `polymorphs` database.
 * Each item belongs to exactly one Quote, identified by quote_id.
 *
 * Schema reference:
 *   quote_items(item_id, quote_id, product_name, quantity, price)
 *
 * @author Dhatri P Sriram (PES1UG23AM098)
 */
public class QuoteItem {

    // -------------------------------------------------------------------------
    // Fields — mapped to DB columns
    // -------------------------------------------------------------------------

    /** Primary key — auto-incremented by the database. */
    private int itemId;

    /** Foreign key → quotes(quote_id). */
    private int quoteId;

    /** Human-readable name of the product/service being quoted. */
    private String productName;

    /** Number of units. Must be >= 1. */
    private int quantity;

    /** Unit price of the product. Must be >= 0. */
    private double price;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Default no-arg constructor for loading from DB. */
    public QuoteItem() {}

    /**
     * Constructor for building a new line item (before DB insertion).
     *
     * @param quoteId     The quote this item belongs to
     * @param productName Name of the product or service
     * @param quantity    Number of units
     * @param price       Unit price
     */
    public QuoteItem(int quoteId, String productName, int quantity, double price) {
        this.quoteId     = quoteId;
        this.productName = productName;
        this.quantity    = quantity;
        this.price       = price;
    }

    // -------------------------------------------------------------------------
    // Computed helper — not stored in DB
    // -------------------------------------------------------------------------

    /**
     * Returns the subtotal for this line item (quantity × price).
     * Used by the pricing engine when computing the quote total.
     *
     * @return subtotal value
     */
    public double getSubtotal() {
        return quantity * price;
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public int getItemId()                          { return itemId; }
    public void setItemId(int itemId)               { this.itemId = itemId; }

    public int getQuoteId()                         { return quoteId; }
    public void setQuoteId(int quoteId)             { this.quoteId = quoteId; }

    public String getProductName()                  { return productName; }
    public void setProductName(String productName)  { this.productName = productName; }

    public int getQuantity()                        { return quantity; }
    public void setQuantity(int quantity)           { this.quantity = quantity; }

    public double getPrice()                        { return price; }
    public void setPrice(double price)              { this.price = price; }

    @Override
    public String toString() {
        return String.format(
            "QuoteItem[id=%d, product='%s', qty=%d, unitPrice=%.2f, subtotal=%.2f]",
            itemId, productName, quantity, price, getSubtotal()
        );
    }
}
