package customers.facade;

import customers.model.Customer;
import customers.builder.CustomerBuilder;
import customers.db.CustomerDAO;
import customers.exception.CustomerException.*;
import shared.validation.ValidationEngine;

public class CustomerFacade {
    private CustomerDAO customerDAO;

    public CustomerFacade() {
        this.customerDAO = new CustomerDAO();
    }

    public void createCustomer(String name, String email, String phone, String region) 
            throws InvalidCustomerData, DuplicateCustomerEntry {
        
        // 1. Shared Validation
        ValidationEngine.validateCustomer(name, email);
        
        // 2. Build Object
        Customer customer = new CustomerBuilder()
                .setName(name)
                .setEmail(email)
                .setPhone(phone)
                .setRegion(region)
                .build();
                
        // 3. Persist to DB
        customerDAO.addCustomer(customer);
    }

    public Customer fetchCustomer(int id) throws CustomerNotFound {
        return customerDAO.getCustomer(id);
    }
}