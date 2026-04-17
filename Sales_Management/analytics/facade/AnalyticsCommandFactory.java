package analytics.facade;

import analytics.command.GenerateReportCommand;

public class AnalyticsCommandFactory {
    private AnalyticsFacade facade;

    public AnalyticsCommandFactory() {
        this.facade = new AnalyticsFacade();
    }

    public void executeCommand(int choice) {
        if (choice == 1) {
            new GenerateReportCommand(facade).execute();
        } else {
            System.out.println("Invalid Analytics Command choice.");
        }
    }
}