package shared.validation;

import customers.exception.CustomerException.InvalidCustomerData;

public interface ValidationStrategy {
    void validate(String input) throws InvalidCustomerData;
}