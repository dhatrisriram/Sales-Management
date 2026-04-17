package analytics.engine;

import analytics.db.AnalyticsDAO;
import analytics.exception.AnalyticsException.*;
import java.util.concurrent.*;

public class ReportingEngine {
    private AnalyticsDAO dao;
    private ExecutorService threadPool;

    public ReportingEngine() {
        this.dao = new AnalyticsDAO();
        this.threadPool = Executors.newFixedThreadPool(4);
    }

    public void generateConcurrentReport() throws ReportGenerationFailed, ThreadExecutionTimeout {
        System.out.println("\n[SYSTEM] Initiating multithreaded data aggregation...");

        Callable<Double> revenueTask = () -> dao.calculateTotalRevenue();
        Callable<Integer> leadsTask = () -> dao.getActiveLeadsCount();
        Callable<Double> forecastTask = () -> {
            double currentRev = dao.calculateTotalRevenue();
            int activeLeads = dao.getActiveLeadsCount();
            return currentRev + (activeLeads * 500.0);
        };

        Future<Double> revenueFuture = threadPool.submit(revenueTask);
        Future<Integer> leadsFuture = threadPool.submit(leadsTask);
        Future<Double> forecastFuture = threadPool.submit(forecastTask);

        try {
            double totalRevenue = revenueFuture.get(5, TimeUnit.SECONDS);
            int activeLeads = leadsFuture.get(5, TimeUnit.SECONDS);
            double projectedRevenue = forecastFuture.get(5, TimeUnit.SECONDS);

            System.out.println("\n===================================");
            System.out.println("   SYSTEM ANALYTICS & FORECAST   ");
            System.out.println("===================================");
            System.out.println("Total Generated Revenue: $" + totalRevenue);
            System.out.println("Active Leads in Pipeline: " + activeLeads);
            System.out.println("--> 30-Day Revenue Forecast: $" + projectedRevenue);
            System.out.println("===================================\n");

        } catch (TimeoutException e) {
            throw new ThreadExecutionTimeout("Database aggregation timed out.");
        } catch (Exception e) {
            throw new ReportGenerationFailed("Failed to compile internal data streams.");
        }
    }

    public void shutdown() {
        threadPool.shutdown();
    }
}