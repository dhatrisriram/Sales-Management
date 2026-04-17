
package quotes.ui;

import quotes.command.QuoteCommand;
import quotes.command.QuoteCommandFactory;
import quotes.db.QuoteDAO;
import quotes.model.QuoteItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * QuoteUI — Command-line interface for the Quotes module.
 *
 * This is Dhatri's Command UI as specified in the division of work document.
 * It follows the Command Pattern: the UI collects input, builds a Command,
 * and calls execute(). The UI has no direct knowledge of business logic.
 *
 * Flow:
 * User selects option → UI collects inputs → creates Command via Factory
 * → command.execute() → output displayed to user
 *
 * @author Dhatri P Sriram (PES1UG23AM098)
 */
public class QuoteUI {

    // Added for integration with SalesManagementSystem
    public void displayMenu() {
        start();
    }

    private static final Logger logger = Logger.getLogger(QuoteUI.class.getName());

    /** Shared DAO — one instance per UI session, reused across all commands. */
    private final QuoteDAO quoteDAO = new QuoteDAO();

    /** Scanner reads from stdin. Closed on exit. */
    private final Scanner scanner = new Scanner(System.in);

    // =========================================================================
    // Entry point
    // =========================================================================

    /**
     * Starts the interactive Quote management menu.
     * Loops until the user selects Exit.
     */
    public void start() {
        System.out.println("\n╔══════════════════════════════════╗");
        System.out.println("║     QUOTES MODULE — Polymorphs   ║");
        System.out.println("║     Sales Management System      ║");
        System.out.println("╚══════════════════════════════════╝");

        boolean running = true;

        while (running) {
            printMenu();

            // Read menu choice — guard against non-integer input
            String choiceStr = scanner.nextLine().trim();
            int choice;

            try {
                choice = Integer.parseInt(choiceStr);
            } catch (NumberFormatException e) {
                System.out.println("⚠ Invalid input. Please enter a number (1–5).");
                continue;
            }

            switch (choice) {
                case 1 -> handleCreateQuote();
                case 2 -> handleViewQuote();
                case 3 -> handleUpdateDiscount();
                case 4 -> handleDeleteQuote();
                case 5 -> {
                    System.out.println("Exiting Quote Management. Goodbye!");
                    running = false;
                }
                default -> System.out.println("⚠ Invalid choice. Please enter 1–5.");
            }
        }

    }

    // =========================================================================
    // Menu
    // =========================================================================

    private void printMenu() {
        System.out.println("\n┌─── Quote Management Menu ───────┐");
        System.out.println("│  1. Create New Quote            │");
        System.out.println("│  2. View Quote by ID            │");
        System.out.println("│  3. Update Quote Discount       │");
        System.out.println("│  4. Delete Quote                │");
        System.out.println("│  5. Exit                        │");
        System.out.println("└─────────────────────────────────┘");
        System.out.print("Enter choice: ");
    }

    // =========================================================================
    // Handler: Create Quote
    // =========================================================================

    private void handleCreateQuote() {
        System.out.println("\n--- Create New Quote ---");

        // Collect customer ID
        int customerId = readInt("Enter Customer ID: ");

        // Collect optional deal ID
        System.out.print("Enter Deal ID (press Enter to skip): ");
        String dealStr = scanner.nextLine().trim();
        int dealId = dealStr.isEmpty() ? -1 : Integer.parseInt(dealStr);

        // Collect line items
        List<QuoteItem> items = new ArrayList<>();
        System.out.println("Add line items (enter 0 when done):");

        while (true) {
            System.out.print("  Product name (or '0' to finish): ");
            String productName = scanner.nextLine().trim();

            if (productName.equals("0"))
                break;

            int qty = readInt("  Quantity: ");
            double unitPrice = readDouble("  Unit price: ");

            // quoteId is 0 — will be assigned after DB insert
            items.add(new QuoteItem(0, productName, qty, unitPrice));
        }

        if (items.isEmpty()) {
            System.out.println("⚠ No items added. Quote creation cancelled.");
            return;
        }

        double discount = readDouble("Enter discount % (0 if none): ");

        // --- Build and execute the command ---
        QuoteCommand cmd = QuoteCommandFactory.createQuote(customerId, dealId, items, discount, quoteDAO);
        cmd.execute();
    }

    // =========================================================================
    // Handler: View Quote
    // =========================================================================

    private void handleViewQuote() {
        System.out.println("\n--- View Quote ---");
        int quoteId = readInt("Enter Quote ID: ");

        QuoteCommand cmd = QuoteCommandFactory.viewQuote(quoteId, quoteDAO);
        cmd.execute();
    }

    // =========================================================================
    // Handler: Update Discount
    // =========================================================================

    private void handleUpdateDiscount() {
        System.out.println("\n--- Update Quote Discount ---");
        int quoteId = readInt("Enter Quote ID: ");
        double newDiscount = readDouble("Enter new discount %: ");

        QuoteCommand cmd = QuoteCommandFactory.updateDiscount(quoteId, newDiscount, quoteDAO);
        cmd.execute();
    }

    // =========================================================================
    // Handler: Delete Quote
    // =========================================================================

    private void handleDeleteQuote() {
        System.out.println("\n--- Delete Quote ---");
        int quoteId = readInt("Enter Quote ID to delete: ");

        // Confirm before destructive action
        System.out.print("Are you sure you want to delete quote " + quoteId + "? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("yes")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        QuoteCommand cmd = QuoteCommandFactory.deleteQuote(quoteId, quoteDAO);
        cmd.execute();
    }

    // =========================================================================
    // Input helpers — guard against bad input
    // =========================================================================

    /**
     * Prompts for an integer. Keeps re-prompting if the user enters non-integer
     * input.
     */
    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("⚠ Please enter a valid integer.");
            }
        }
    }

    /**
     * Prompts for a double. Keeps re-prompting if the user enters non-numeric
     * input.
     */
    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("⚠ Please enter a valid number (e.g., 15.0).");
            }
        }
    }

    // =========================================================================
    // Main — entry point for standalone testing of this module
    // =========================================================================

    /**
     * Standalone entry point for the Quotes module.
     *
     * In the full ERP system, this would be invoked from a central menu
     * (similar to how the Facade pattern integrates all modules per Harshini's
     * design).
     */
    public static void main(String[] args) {
        logger.info("Starting Quotes module standalone UI");
        new QuoteUI().start();
    }
}
