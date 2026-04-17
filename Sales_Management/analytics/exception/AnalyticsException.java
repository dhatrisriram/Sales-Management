package analytics.exception;

public class AnalyticsException extends Exception {
    public AnalyticsException(String message) {
        super(message);
    }

    public static class ReportGenerationFailed extends AnalyticsException {
        public ReportGenerationFailed(String message) {
            super("Report Generation Error: " + message);
        }
    }

    public static class ThreadExecutionTimeout extends AnalyticsException {
        public ThreadExecutionTimeout(String message) {
            super("Multithreading Timeout: " + message);
        }
    }
}