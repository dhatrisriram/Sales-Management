package analytics.command;

import analytics.facade.AnalyticsFacade;
import analytics.exception.AnalyticsException.*;

public class GenerateReportCommand {
    private AnalyticsFacade facade;

    public GenerateReportCommand(AnalyticsFacade facade) {
        this.facade = facade;
    }

    public void execute() {
        try {
            System.out.println("Requesting system forecast report...");
            facade.runFullSystemReport();
            System.out.println("SUCCESS: Report generated successfully.");
        } catch (ThreadExecutionTimeout | ReportGenerationFailed e) {
            System.err.println("[CRITICAL EXCEPTION] " + e.getMessage());
        }
    }
}