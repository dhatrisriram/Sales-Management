package customers.command;

import customers.facade.CustomerFacade;
import customers.exception.CustomerException.*;

public class AddCustomerCommand implements CustomerCommand {
    private CustomerFacade facade;
    private String name, email, phone, region;

    public AddCustomerCommand(CustomerFacade facade, String name, String email, String phone, String region) {
        this.facade = facade;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.region = region;
    }

    @Override
    public void execute() {
        try {
            facade.createCustomer(name, email, phone, region);
            System.out.println("SUCCESS: Customer created successfully.");
        } catch (InvalidCustomerData e) {
            System.err.println("[MINOR EXCEPTION] " + e.getMessage());
            System.err.println("-> ACTION: Highlight incorrect fields and request correction.");
        } catch (DuplicateCustomerEntry e) {
            System.err.println("[WARNING EXCEPTION] " + e.getMessage());
        }
    }
}