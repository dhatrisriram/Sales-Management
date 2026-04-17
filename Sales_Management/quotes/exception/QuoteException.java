package quotes.exception;

/**
 * ============================================================
 * QUOTES MODULE — CUSTOM EXCEPTIONS
 * ============================================================
 *
 * All exceptions below are defined in the team's exception table
 * (SalesManagementExceptions.pdf). Each exception maps to a
 * specific failure case that can occur during quote operations.
 *
 * Categories:
 *   MAJOR   — blocks the operation entirely, must be resolved
 *   WARNING — alerts user but may be overridden (e.g. approval)
 *   MINOR   — soft error, usually recoverable by the user
 *
 * @author Dhatri P Sriram (PES1UG23AM098)
 */

// =============================================================
// MAJOR EXCEPTIONS — Quote cannot proceed without resolution
// =============================================================

/**
 * PRICE_CALCULATION_ERROR (MAJOR)
 *
 * Thrown when an arithmetic error occurs during price computation
 * (e.g., NaN/Infinity, overflow on large quantities).
 *
 * Handling: Recompute automatically using fallback logic and log the issue.
 */
class PriceCalculationException extends RuntimeException {
    public PriceCalculationException(String detail) {
        super("[PRICE_CALCULATION_ERROR] Error in price calculation. Detail: " + detail);
    }
}

/**
 * NEGATIVE_TOTAL_ORDER_VALUE (MAJOR)
 *
 * Thrown when the computed final amount drops below zero.
 * This usually indicates an excessive discount entry.
 *
 * Handling: Block quote submission and highlight the discount causing the issue.
 */
class NegativeTotalOrderValueException extends RuntimeException {
    public NegativeTotalOrderValueException(double computed) {
        super("[NEGATIVE_TOTAL_ORDER_VALUE] Order total cannot be negative. Computed: " + computed);
    }
}

/**
 * QUOTE_GENERATION_FAILED (MINOR)
 *
 * Thrown when quote creation fails due to a DB write error
 * or missing required data that could not be resolved.
 *
 * Handling: Retry once or allow the user to generate manually.
 */
class QuoteGenerationFailedException extends RuntimeException {
    public QuoteGenerationFailedException(String reason) {
        super("[QUOTE_GENERATION_FAILED] Unable to generate quotation. Reason: " + reason);
    }
    public QuoteGenerationFailedException(String reason, Throwable cause) {
        super("[QUOTE_GENERATION_FAILED] Unable to generate quotation. Reason: " + reason, cause);
    }
}

// =============================================================
// WARNING EXCEPTIONS — Require approval or confirmation
// =============================================================

/**
 * DISCOUNT_LIMIT_EXCEEDED (WARNING)
 *
 * Thrown when the applied discount percentage exceeds the allowed
 * maximum of 50% (per validation rules in the DB schema doc).
 *
 * Handling: Pause quote creation, request admin approval before proceeding.
 */
class DiscountLimitExceededException extends RuntimeException {
    private final double attemptedDiscount;

    public DiscountLimitExceededException(double attempted) {
        super("[DISCOUNT_LIMIT_EXCEEDED] Discount exceeds allowed limit. "
            + "Attempted: " + attempted + "%, Max allowed: 50%");
        this.attemptedDiscount = attempted;
    }

    /** Returns the discount value the user tried to apply. */
    public double getAttemptedDiscount() {
        return attemptedDiscount;
    }
}

// =============================================================
// Public-facing wrapper class — all exceptions declared here
// so other packages only import this one file.
// =============================================================

/**
 * Central container for all Quotes-module exceptions.
 *
 * Usage from other classes:
 *   throw new QuoteException.DiscountLimitExceeded(55.0);
 */
public class QuoteException {

    // Prevent instantiation — this is a namespace class only
    private QuoteException() {}

    // ------------------------------------------------------------------
    // MAJOR
    // ------------------------------------------------------------------

    public static class PriceCalculation extends PriceCalculationException {
        public PriceCalculation(String detail) { super(detail); }
    }

    public static class NegativeTotalOrderValue extends NegativeTotalOrderValueException {
        public NegativeTotalOrderValue(double computed) { super(computed); }
    }

    public static class QuoteGenerationFailed extends QuoteGenerationFailedException {
        public QuoteGenerationFailed(String reason)                { super(reason); }
        public QuoteGenerationFailed(String reason, Throwable cause) { super(reason, cause); }
    }

    // ------------------------------------------------------------------
    // WARNING
    // ------------------------------------------------------------------

    public static class DiscountLimitExceeded extends DiscountLimitExceededException {
        public DiscountLimitExceeded(double attempted) { super(attempted); }
    }
}
