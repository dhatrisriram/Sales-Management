package shared.validation;

import customers.exception.CustomerException.InvalidCustomerData;

public class ValidationEngine {
    public static void validateCustomer(String name, String email) throws InvalidCustomerData {
        new NameValidationStrategy().validate(name);
        new EmailValidationStrategy().validate(email);
    }
}