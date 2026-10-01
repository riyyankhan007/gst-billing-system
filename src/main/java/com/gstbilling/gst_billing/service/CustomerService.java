package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CurrentUserService currentUserService;

    public CustomerService(CustomerRepository customerRepository, CurrentUserService currentUserService) {
        this.customerRepository = customerRepository;
        this.currentUserService = currentUserService;
    }

    public Customer createCustomer(Customer customer) {
        customer.setBusiness(currentUserService.getCurrentUser().getBusiness());
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findByBusinessId(currentUserService.getCurrentUser().getBusiness().getId());
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findByIdAndBusinessId(id, currentUserService.getCurrentUser().getBusiness().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer is not available to this business"));
    }

    public Customer updateCustomer(Long id, Customer changes) {
        Customer customer = getCustomerById(id);
        customer.setName(changes.getName()); customer.setGstin(changes.getGstin()); customer.setAddress(changes.getAddress()); customer.setState(changes.getState()); customer.setStateCode(changes.getStateCode()); customer.setPhone(changes.getPhone()); customer.setEmail(changes.getEmail());
        return customerRepository.save(customer);
    }
}
