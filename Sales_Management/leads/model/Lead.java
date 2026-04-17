package leads.model;

import java.time.LocalDateTime;

/**
 * Lead — represents a sales lead in the CRM pipeline.
 *
 * Maps directly to the `leads` table in the `polymorphs` database.
 *
 * Status lifecycle (managed by LeadWorkflowEngine):
 *   NEW → CONTACTED → QUALIFIED → CONVERTED | LOST
 *
 * @author Bhumika (Leads + Deals module)
 */
public class Lead {

    // -------------------------------------------------------------------------
    // Valid status values — enforced by LeadWorkflowEngine
    // -------------------------------------------------------------------------
    public static final String STATUS_NEW        = "NEW";
    public static final String STATUS_CONTACTED  = "CONTACTED";
    public static final String STATUS_QUALIFIED  = "QUALIFIED";
    public static final String STATUS_CONVERTED  = "CONVERTED";
    public static final String STATUS_LOST       = "LOST";

    // -------------------------------------------------------------------------
    // Fields — mirror the `leads` table columns
    // -------------------------------------------------------------------------
    private int           leadId;
    private String        name;
    private String        company;
    private String        status;
    private LocalDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Default constructor — used when mapping from ResultSet. */
    public Lead() {}

    /**
     * Constructor for creating a new lead (before DB insert).
     *
     * @param name    Contact person's name (required, non-empty)
     * @param company Company name (optional)
     * @param status  Initial status — should be STATUS_NEW for new leads
     */
    public Lead(String name, String company, String status) {
        this.name    = name;
        this.company = company;
        this.status  = status;
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public int getLeadId()                    { return leadId; }
    public void setLeadId(int leadId)         { this.leadId = leadId; }

    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }

    public String getCompany()                { return company; }
    public void setCompany(String company)    { this.company = company; }

    public String getStatus()                 { return status; }
    public void setStatus(String status)      { this.status = status; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("Lead{id=%d, name='%s', company='%s', status='%s'}",
                leadId, name, company, status);
    }
}
