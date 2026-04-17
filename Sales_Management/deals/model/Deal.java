package deals.model;

import java.time.LocalDateTime;

/**
 * Deal — represents a sales deal linked to a customer.
 *
 * Maps directly to the `deals` table in the `polymorphs` database.
 *
 * Pipeline stages (managed by DealWorkflowEngine):
 *   PROSPECTING → QUALIFICATION → PROPOSAL → NEGOTIATION → CLOSED_WON | CLOSED_LOST
 *
 * @author Bhumika (Leads + Deals module)
 */
public class Deal {

    // -------------------------------------------------------------------------
    // Valid pipeline stage constants
    // -------------------------------------------------------------------------
    public static final String STAGE_PROSPECTING   = "PROSPECTING";
    public static final String STAGE_QUALIFICATION = "QUALIFICATION";
    public static final String STAGE_PROPOSAL      = "PROPOSAL";
    public static final String STAGE_NEGOTIATION   = "NEGOTIATION";
    public static final String STAGE_CLOSED_WON    = "CLOSED_WON";
    public static final String STAGE_CLOSED_LOST   = "CLOSED_LOST";

    // -------------------------------------------------------------------------
    // Valid status constants
    // -------------------------------------------------------------------------
    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_WON      = "WON";
    public static final String STATUS_LOST     = "LOST";

    // -------------------------------------------------------------------------
    // Fields — mirror the `deals` table columns
    // -------------------------------------------------------------------------
    private int           dealId;
    private int           customerId;
    private double        amount;
    private String        stage;
    private String        status;
    private LocalDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Default constructor — used when mapping from ResultSet. */
    public Deal() {}

    /**
     * Constructor for creating a new deal (before DB insert).
     *
     * @param customerId  FK reference to customers.customer_id (required)
     * @param amount      Deal value in currency (must be >= 0)
     * @param stage       Initial stage — should be STAGE_PROSPECTING for new deals
     * @param status      Initial status — typically STATUS_ACTIVE
     */
    public Deal(int customerId, double amount, String stage, String status) {
        this.customerId = customerId;
        this.amount     = amount;
        this.stage      = stage;
        this.status     = status;
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public int getDealId()                    { return dealId; }
    public void setDealId(int dealId)         { this.dealId = dealId; }

    public int getCustomerId()                { return customerId; }
    public void setCustomerId(int id)         { this.customerId = id; }

    public double getAmount()                 { return amount; }
    public void setAmount(double amount)      { this.amount = amount; }

    public String getStage()                  { return stage; }
    public void setStage(String stage)        { this.stage = stage; }

    public String getStatus()                 { return status; }
    public void setStatus(String status)      { this.status = status; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("Deal{id=%d, customerId=%d, amount=%.2f, stage='%s', status='%s'}",
                dealId, customerId, amount, stage, status);
    }
}
