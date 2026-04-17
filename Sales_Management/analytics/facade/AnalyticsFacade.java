package analytics.facade;

import analytics.engine.ReportingEngine;
import analytics.exception.AnalyticsException.*;

public class AnalyticsFacade {
    private ReportingEngine reportingEngine;

    public AnalyticsFacade() {
        this.reportingEngine = new ReportingEngine();
    }

    public void runFullSystemReport() throws ReportGenerationFailed, ThreadExecutionTimeout {
        reportingEngine.generateConcurrentReport();
    }

    public void closeResources() {
        reportingEngine.shutdown();
    }
}