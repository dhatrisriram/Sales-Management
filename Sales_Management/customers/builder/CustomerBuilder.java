package customers.builder;

import customers.model.Customer;

public class CustomerBuilder {
    private Customer customer;

    public CustomerBuilder() {
        this.customer = new Customer();
    }

    public CustomerBuilder setCustomerId(int id) {
        customer.setCustomerId(id);
        return this;
    }

    public CustomerBuilder setName(String name) {
        customer.setName(name);
        return this;
    }

    public CustomerBuilder setEmail(String email) {
        customer.setEmail(email);
        return this;
    }

    public CustomerBuilder setPhone(String phone) {
        customer.setPhone(phone);
        return this;
    }

    public CustomerBuilder setRegion(String region) {
        customer.setRegion(region);
        return this;
    }

    public Customer build() {
        return customer;
    }
}