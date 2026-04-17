package shared.validation;

import customers.exception.CustomerException.InvalidCustomerData;

public class NameValidationStrategy implements ValidationStrategy {
    @Override
    public void validate(String name) throws InvalidCustomerData {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidCustomerData("Entered customer details are invalid: Name cannot be empty.");
        }
    }
}