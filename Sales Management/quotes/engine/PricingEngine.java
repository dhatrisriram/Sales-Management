package quotes.engine;

import quotes.exception.QuoteException;
import quotes.model.QuoteItem;

import java.util.List;
import java.util.logging.Logger;

/**
 * PricingEngine — Core business logic for quote price computation.
 *
 * Responsibilities:
 *   1. Compute the raw total from a list of QuoteItems
 *   2. Validate that the discount is within the allowed limit (0–50%)
 *   3. Compute the final amount after discount
 *   4. Guard against negative final amounts
 *
 * Exception coverage (from SalesManagementExceptions.pdf):
 *   - PRICE_CALCULATION_ERROR  → thrown on arithmetic anomalies
 *   - DISCOUNT_LIMIT_EXCEEDED  → thrown when discount > 50%
 *   - NEGATIVE_TOTAL_ORDER_VALUE → thrown when final amount < 0
 *
 * @author Dhatri P Sriram (PES1UG23AM098)
 */
public class PricingEngine {

    private static final Logger logger = Logger.getLogger(PricingEngine.class.getName());

    /** Maximum discount percentage permitted without admin approval. */
    private static final double MAX_DISCOUNT_PERCENT = 50.0;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Computes the raw total (before discount) by summing all QuoteItem subtotals.
     *
     * @param items List of line items in the quote
     * @return sum of (quantity × unit price) for every item
     * @throws QuoteException.PriceCalculation if the result is NaN or infinite
     */
    public double computeTotal(List<QuoteItem> items) {
        if (items == null || items.isEmpty()) {
            // An empty quote has zero total — not an error, just unusual
            logger.warning("PricingEngine.computeTotal called with no items — returning 0.0");
            return 0.0;
        }

        double total = 0.0;

        for (QuoteItem item : items) {
            double subtotal = item.getSubtotal(); // quantity × price

            // Guard against individual item calculation anomalies
            if (Double.isNaN(subtotal) || Double.isInfinite(subtotal)) {
                throw new QuoteException.PriceCalculation(
                    "Item '" + item.getProductName() + "' produced an invalid subtotal: " + subtotal
                );
            }

            total += subtotal;
        }

        // Final check on the aggregated total
        if (Double.isNaN(total) || Double.isInfinite(total)) {
            throw new QuoteException.PriceCalculation(
                "Aggregated total is invalid: " + total
            );
        }

        logger.info("PricingEngine: computed raw total = " + total);
        return total;
    }

    /**
     * Validates the discount percentage and computes the final quote amount.
     *
     * Validation rules (from final_db_schema_detailed.pdf):
     *   - Discount must be in [0, 50]
     *   - Final amount must be >= 0
     *
     * @param rawTotal   The pre-discount total (output of computeTotal)
     * @param discountPct Discount percentage to apply (e.g., 10 means 10%)
     * @return finalAmount after discount is applied
     * @throws QuoteException.DiscountLimitExceeded    if discountPct > 50
     * @throws QuoteException.NegativeTotalOrderValue  if finalAmount < 0
     * @throws QuoteException.PriceCalculation         if computation produces NaN/Infinity
     */
    public double applyDiscount(double rawTotal, double discountPct) {
        // --- Validation: discount must not exceed 50% ---
        if (discountPct > MAX_DISCOUNT_PERCENT) {
            logger.warning("Discount limit exceeded — attempted: " + discountPct + "%");
            // DISCOUNT_LIMIT_EXCEEDED: ask for admin approval (handled by caller/UI)
            throw new QuoteException.DiscountLimitExceeded(discountPct);
        }

        // Negative discount doesn't make business sense (would inflate the price)
        if (discountPct < 0) {
            throw new QuoteException.PriceCalculation(
                "Discount percentage cannot be negative: " + discountPct
            );
        }

        // --- Core calculation ---
        double discountValue = (discountPct / 100.0) * rawTotal;
        double finalAmount   = rawTotal - discountValue;

        // Check for arithmetic anomalies
        if (Double.isNaN(finalAmount) || Double.isInfinite(finalAmount)) {
            throw new QuoteException.PriceCalculation(
                "Final amount computation failed: rawTotal=" + rawTotal + ", discount=" + discountPct
            );
        }

        // NEGATIVE_TOTAL_ORDER_VALUE: block and alert
        if (finalAmount < 0) {
            logger.severe("Negative final amount detected: " + finalAmount);
            throw new QuoteException.NegativeTotalOrderValue(finalAmount);
        }

        logger.info(String.format(
            "PricingEngine: rawTotal=%.2f, discount=%.1f%%, finalAmount=%.2f",
            rawTotal, discountPct, finalAmount
        ));

        return finalAmount;
    }

    /**
     * Convenience method: compute total from items, then apply discount in one call.
     *
     * @param items       Line items for the quote
     * @param discountPct Discount percentage (0–50)
     * @return double[] { rawTotal, finalAmount }
     */
    public double[] computeFinalPrice(List<QuoteItem> items, double discountPct) {
        double rawTotal   = computeTotal(items);
        double finalAmount = applyDiscount(rawTotal, discountPct);
        return new double[]{ rawTotal, finalAmount };
    }
}
