import customers.ui.CustomerUI;

import quotes.ui.QuoteUI;
import analytics.facade.AnalyticsCommandFactory;
import java.util.Scanner;

public class SalesManagementSystem {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        CustomerUI customerUI = new CustomerUI();
        LeadDealUI leadDealUI = new LeadDealUI();
        QuoteUI quoteUI = new QuoteUI();
        AnalyticsCommandFactory analyticsFactory = new AnalyticsCommandFactory();

        System.out.println("=================================================");
        System.out.println("  ENTERPRISE SALES MANAGEMENT SYSTEM - STARTING  ");
        System.out.println("=================================================");

        while (running) {
            System.out.println("\n--- MAIN SYSTEM MENU ---");
            System.out.println("1. Customer Management (Namratha)");
            System.out.println("2. Leads & Deals Management (Bhumika)");
            System.out.println("3. Quotes & Pricing (Dhatri)");
            System.out.println("4. System Analytics & Forecasting (Harshini)");
            System.out.println("5. Exit System");
            System.out.print("Enter choice: ");

            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    customerUI.displayMenu();
                    break;
                case 2:
                    leadDealUI.displayMenu();
                    break;
                case 3:
                    quoteUI.displayMenu();
                    break;
                case 4:
                    System.out.println("\n--- ANALYTICS MENU ---");
                    System.out.println("1. Generate Full System Forecast Report");
                    System.out.print("Enter choice: ");
                    int analyticsChoice = scanner.nextInt();
                    analyticsFactory.executeCommand(analyticsChoice);
                    break;
                case 5:
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        scanner.close();
        System.exit(0);
    }
}