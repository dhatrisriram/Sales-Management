package customers.ui;

import customers.facade.CustomerFacade;
import customers.command.CustomerCommand;
import customers.command.AddCustomerCommand;
import java.util.Scanner;

public class CustomerUI {
    public static void main(String[] args) {
        CustomerFacade customerFacade = new CustomerFacade();
        Scanner scanner = new Scanner(System.in);

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
            customerFacade, name, email, phone, region
        );
        
        addCmd.execute();
        
        scanner.close();
    }
}