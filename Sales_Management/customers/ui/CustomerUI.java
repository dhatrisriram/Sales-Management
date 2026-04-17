
package customers.ui;

import customers.facade.CustomerFacade;
import customers.command.CustomerCommand;
import customers.command.AddCustomerCommand;
import java.util.Scanner;

@SuppressWarnings("resource")
public class CustomerUI {
    // Added for integration with SalesManagementSystem
    public void displayMenu() {
        main(new String[0]);
    }

    public static void main(String[] args) {
        CustomerFacade customerFacade = new CustomerFacade();
        // Do not close this Scanner to avoid closing System.in
        Scanner scanner = new Scanner(System.in); // NOSONAR

        System.out.println("===================================");
        System.out.println("   SALES SYSTEM: CUSTOMER MODULE   ");
        System.out.println("===================================");

        System.out.print("Enter Customer Name: ");
        String name = scanner.nextLine();

        System.out.print("Enter Customer Email: ");
        String email = scanner.nextLine();

        System.out.print("Enter Customer Phone: ");
        String phone = scanner.nextLine();

        System.out.print("Enter Customer Region: ");
        String region = scanner.nextLine();

        System.out.println("\nProcessing...");

        // Passing the real user input into the Command
        CustomerCommand addCmd = new AddCustomerCommand(
                customerFacade, name, email, phone, region);

        addCmd.execute();

    }
}