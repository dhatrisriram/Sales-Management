import deals.command.DealCommand;
import deals.command.DealCommandFactory;
import deals.db.DealDAO;
import deals.engine.DealWorkflowEngine;
import deals.model.Deal;
import leads.command.LeadCommand;
import leads.command.LeadCommandFactory;
import leads.db.LeadDAO;
import leads.engine.LeadWorkflowEngine;
import leads.model.Lead;

import java.util.Scanner;

/**
 * LeadDealUI — Command-line UI for the Leads and Deals module.
 *
 * This is Bhumika's entry point for the Sales Management System.
 * Uses the Command Pattern throughout: all actions are dispatched
 * via LeadCommandFactory / DealCommandFactory.
 *
 * HOW TO RUN (from project root, assuming shared.db.DBConnection is set up):
 *   javac -cp mysql-connector-j-9.6.0.jar -d out $(find . -name "*.java")
 *   java  -cp out:mysql-connector-j-9.6.0.jar LeadDealUI
 *
 * @author Bhumika (Leads + Deals module)
 */
public class LeadDealUI {

    private static final Scanner scanner = new Scanner(System.in);

    // Shared DAO and engine instances — created once and reused
    private static final LeadDAO leadDAO          = new LeadDAO();
    private static final DealDAO dealDAO          = new DealDAO();
    private static final LeadWorkflowEngine leadEngine = new LeadWorkflowEngine();
    private static final DealWorkflowEngine dealEngine = new DealWorkflowEngine();

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   Sales Management - Leads & Deals     ║");
        System.out.println("╚════════════════════════════════════════╝");

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": handleLeadMenu();  break;
                case "2": handleDealMenu();  break;
                case "0":
                    System.out.println("Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println(" Invalid choice. Please enter 1, 2, or 0.\n");
            }
        }

        scanner.close();
    }

    // =========================================================================
    // MAIN MENU
    // =========================================================================

    private static void printMainMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Leads");
        System.out.println("2. Deals");
        System.out.println("0. Exit");
        System.out.print("Enter choice: ");
    }

    // =========================================================================
    // LEADS SUB-MENU
    // =========================================================================

    private static void handleLeadMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- LEADS MENU ---");
            System.out.println("1. Create Lead");
            System.out.println("2. View Lead by ID");
            System.out.println("3. List All Leads");
            System.out.println("4. List Leads by Status");
            System.out.println("5. Update Lead Status");
            System.out.println("6. Delete Lead");
            System.out.println("0. Back");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": createLead();              break;
                case "2": viewLead();                break;
                case "3": listAllLeads();            break;
                case "4": listLeadsByStatus();       break;
                case "5": updateLeadStatus();        break;
                case "6": deleteLead();              break;
                case "0": back = true;               break;
                default: System.out.println(" Invalid choice.\n");
            }
        }
    }

    private static void createLead() {
        System.out.print("Enter lead name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter company name (or press Enter to skip): ");
        String company = scanner.nextLine().trim();
        if (company.isEmpty()) company = null;

        LeadCommand cmd = LeadCommandFactory.createLead(name, company, leadDAO, leadEngine);
        cmd.execute();
    }

    private static void viewLead() {
        int id = readInt("Enter Lead ID: ");
        LeadCommandFactory.viewLead(id, leadDAO).execute();
    }

    private static void listAllLeads() {
        LeadCommandFactory.listAllLeads(leadDAO).execute();
    }

    private static void listLeadsByStatus() {
        System.out.println("Valid statuses: " + Lead.STATUS_NEW + ", " + Lead.STATUS_CONTACTED
            + ", " + Lead.STATUS_QUALIFIED + ", " + Lead.STATUS_CONVERTED + ", " + Lead.STATUS_LOST);
        System.out.print("Enter status: ");
        String status = scanner.nextLine().trim().toUpperCase();
        LeadCommandFactory.listLeads(status, leadDAO).execute();
    }

    private static void updateLeadStatus() {
        int id = readInt("Enter Lead ID: ");

        System.out.println("Valid statuses: " + Lead.STATUS_CONTACTED + ", " + Lead.STATUS_QUALIFIED
            + ", " + Lead.STATUS_CONVERTED + ", " + Lead.STATUS_LOST);
        System.out.print("Enter new status: ");
        String newStatus = scanner.nextLine().trim().toUpperCase();

        LeadCommandFactory.updateStatus(id, newStatus, leadDAO, leadEngine).execute();
    }

    private static void deleteLead() {
        int id = readInt("Enter Lead ID to delete: ");
        System.out.print("Are you sure? (yes/no): ");
        if ("yes".equalsIgnoreCase(scanner.nextLine().trim())) {
            LeadCommandFactory.deleteLead(id, leadDAO).execute();
        } else {
            System.out.println("Delete cancelled.");
        }
    }

    // =========================================================================
    // DEALS SUB-MENU
    // =========================================================================

    private static void handleDealMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- DEALS MENU ---");
            System.out.println("1. Create Deal");
            System.out.println("2. View Deal by ID");
            System.out.println("3. List Deals by Pipeline Stage");
            System.out.println("4. List Deals by Customer");
            System.out.println("5. Advance Deal Stage");
            System.out.println("6. Delete Deal");
            System.out.println("0. Back");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": createDeal();              break;
                case "2": viewDeal();                break;
                case "3": listDealsByStage();        break;
                case "4": listDealsByCustomer();     break;
                case "5": advanceDealStage();        break;
                case "6": deleteDeal();              break;
                case "0": back = true;               break;
                default: System.out.println(" Invalid choice.\n");
            }
        }
    }

    private static void createDeal() {
        int customerId = readInt("Enter Customer ID: ");
        double amount  = readDouble("Enter deal amount (Rs): ");
        DealCommandFactory.createDeal(customerId, amount, dealDAO, dealEngine).execute();
    }

    private static void viewDeal() {
        int id = readInt("Enter Deal ID: ");
        DealCommandFactory.viewDeal(id, dealDAO).execute();
    }

    private static void listDealsByStage() {
        System.out.println("Stages: " + Deal.STAGE_PROSPECTING + ", " + Deal.STAGE_QUALIFICATION
            + ", " + Deal.STAGE_PROPOSAL + ", " + Deal.STAGE_NEGOTIATION
            + ", " + Deal.STAGE_CLOSED_WON + ", " + Deal.STAGE_CLOSED_LOST);
        System.out.print("Enter stage: ");
        String stage = scanner.nextLine().trim().toUpperCase();
        DealCommandFactory.listByStage(stage, dealDAO).execute();
    }

    private static void listDealsByCustomer() {
        int id = readInt("Enter Customer ID: ");
        DealCommandFactory.listByCustomer(id, dealDAO).execute();
    }

    private static void advanceDealStage() {
        int id = readInt("Enter Deal ID: ");
        System.out.println("Stages: " + Deal.STAGE_QUALIFICATION + ", " + Deal.STAGE_PROPOSAL
            + ", " + Deal.STAGE_NEGOTIATION + ", " + Deal.STAGE_CLOSED_WON
            + ", " + Deal.STAGE_CLOSED_LOST);
        System.out.print("Enter new stage: ");
        String stage = scanner.nextLine().trim().toUpperCase();
        DealCommandFactory.advanceStage(id, stage, dealDAO, dealEngine).execute();
    }

    private static void deleteDeal() {
        int id = readInt("Enter Deal ID to delete: ");
        System.out.print("Are you sure? (yes/no): ");
        if ("yes".equalsIgnoreCase(scanner.nextLine().trim())) {
            DealCommandFactory.deleteDeal(id, dealDAO).execute();
        } else {
            System.out.println("Delete cancelled.");
        }
    }

    // =========================================================================
    // Input helpers
    // =========================================================================

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(" Please enter a valid integer.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(" Please enter a valid number.");
            }
        }
    }
}
