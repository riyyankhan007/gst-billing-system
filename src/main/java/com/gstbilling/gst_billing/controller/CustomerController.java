package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.CustomerDetailsResponse;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.service.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer) {
        return customerService.createCustomer(customer);
    }

    @GetMapping
    public List<Customer> getAllCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type
    ) {
        return customerService.getAllCustomers(search, type);
    }

    @GetMapping("/{id}")
    public Customer getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @GetMapping("/{id}/details")
    public CustomerDetailsResponse getCustomerDetails(@PathVariable Long id) {
        return customerService.getCustomerDetails(id);
    }

    @PutMapping("/{id}")
    public Customer updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        return customerService.updateCustomer(id, customer);
    }

    @DeleteMapping("/{id}")
    public void deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
    }
}
