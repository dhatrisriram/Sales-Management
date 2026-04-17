package customers.exception;

public class CustomerException extends Exception {
    public CustomerException(String message) { super(message); }

    public static class CustomerNotFound extends CustomerException {
        public CustomerNotFound(String message) { super(message); }
    }

    public static class InvalidCustomerData extends CustomerException {
        public InvalidCustomerData(String message) { super(message); }
    }

    public static class DuplicateCustomerEntry extends CustomerException {
        public DuplicateCustomerEntry(String message) { super(message); }
    }
}