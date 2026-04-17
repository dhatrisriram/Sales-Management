package shared.validation;

import customers.exception.CustomerException.InvalidCustomerData;

public class EmailValidationStrategy implements ValidationStrategy {
    @Override
    public void validate(String email) throws InvalidCustomerData {
        if (email == null || !email.contains("@")) {
            throw new InvalidCustomerData("Entered customer details are invalid: Invalid email format.");
        }
    }
}