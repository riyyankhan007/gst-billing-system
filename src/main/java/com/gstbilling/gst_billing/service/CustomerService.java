package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.CustomerDetailsResponse;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.repository.CustomerRepository;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.util.IndianTaxValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CurrentUserService currentUserService;
    private final InvoiceRepository invoiceRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            CurrentUserService currentUserService,
            InvoiceRepository invoiceRepository
    ) {
        this.customerRepository = customerRepository;
        this.currentUserService = currentUserService;
        this.invoiceRepository = invoiceRepository;
    }

    public Customer createCustomer(Customer customer) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();

        if (customer.getName() == null || customer.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required.");
        }
        customer.setName(customer.getName().trim());

        // Validate and clean GSTIN
        if (customer.getGstin() != null && !customer.getGstin().isBlank()) {
            String cleanGstin = customer.getGstin().trim().toUpperCase();
            if (!IndianTaxValidator.isValidGstin(cleanGstin)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GSTIN format. Expected 15 characters (e.g. 29ABCDE1234F1Z5)");
            }

            if (customerRepository.existsByGstinIgnoreCaseAndBusinessId(cleanGstin, businessId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A customer with this GSTIN already exists in your business.");
            }
            customer.setGstin(cleanGstin);

            if (customer.getPan() == null || customer.getPan().isBlank()) {
                customer.setPan(IndianTaxValidator.extractPanFromGstin(cleanGstin));
            }
            if (customer.getCustomerType() == null || customer.getCustomerType().isBlank()) {
                customer.setCustomerType("B2B");
            }
        } else {
            customer.setGstin(null);
            if (customer.getCustomerType() == null || customer.getCustomerType().isBlank()) {
                customer.setCustomerType("B2C");
            }
        }

        // Validate PAN
        if (customer.getPan() != null && !customer.getPan().isBlank()) {
            String cleanPan = customer.getPan().trim().toUpperCase();
            if (!IndianTaxValidator.isValidPan(cleanPan)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PAN format. Expected 10 characters (e.g. ABCDE1234F)");
            }
            customer.setPan(cleanPan);
        }

        customer.setBusiness(currentUserService.getCurrentUser().getBusiness());
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers(String search, String type) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        List<Customer> list = customerRepository.findByBusinessId(businessId);

        return list.stream()
                .filter(c -> {
                    if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
                        return type.equalsIgnoreCase(c.getCustomerType());
                    }
                    return true;
                })
                .filter(c -> {
                    if (search != null && !search.isBlank()) {
                        String q = search.trim().toLowerCase();
                        boolean matchName = c.getName() != null && c.getName().toLowerCase().contains(q);
                        boolean matchPhone = c.getPhone() != null && c.getPhone().toLowerCase().contains(q);
                        boolean matchGstin = c.getGstin() != null && c.getGstin().toLowerCase().contains(q);
                        boolean matchEmail = c.getEmail() != null && c.getEmail().toLowerCase().contains(q);
                        return matchName || matchPhone || matchGstin || matchEmail;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findByIdAndBusinessId(id, currentUserService.getCurrentUser().getBusiness().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found or not accessible"));
    }

    public CustomerDetailsResponse getCustomerDetails(Long id) {
        Customer customer = getCustomerById(id);
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();

        List<Invoice> customerInvoices = invoiceRepository.findByCustomer_IdAndBusiness_IdOrderByCreatedAtDesc(id, businessId);

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;
        int pendingCount = 0;
        LocalDate today = LocalDate.now();

        for (Invoice inv : customerInvoices) {
            if ("CANCELLED".equalsIgnoreCase(inv.getStatus()) || "DRAFT".equalsIgnoreCase(inv.getStatus())) {
                continue;
            }

            BigDecimal grandTotal = inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO;
            BigDecimal paid = inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal balance = inv.getBalanceAmount() != null ? inv.getBalanceAmount() : grandTotal.subtract(paid);

            totalInvoiced = totalInvoiced.add(grandTotal);
            totalPaid = totalPaid.add(paid);

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                pendingCount++;
                // Check if overdue: dueDate past or invoiceDate + 30 days
                LocalDate due = inv.getDueDate() != null ? inv.getDueDate() : (inv.getInvoiceDate() != null ? inv.getInvoiceDate().plusDays(30) : today);
                if (due.isBefore(today)) {
                    overdueAmount = overdueAmount.add(balance);
                }
            }
        }

        BigDecimal opening = customer.getOpeningBalance() != null ? customer.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal outstandingBalance = totalInvoiced.subtract(totalPaid).add(opening);

        return new CustomerDetailsResponse(
                customer,
                totalInvoiced,
                totalPaid,
                outstandingBalance,
                overdueAmount,
                customerInvoices.size(),
                pendingCount,
                customerInvoices
        );
    }

    public Customer updateCustomer(Long id, Customer changes) {
        Customer customer = getCustomerById(id);
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();

        if (changes.getName() != null && !changes.getName().isBlank()) {
            customer.setName(changes.getName().trim());
        }

        // Validate and check GSTIN uniqueness
        if (changes.getGstin() != null && !changes.getGstin().isBlank()) {
            String cleanGstin = changes.getGstin().trim().toUpperCase();
            if (!IndianTaxValidator.isValidGstin(cleanGstin)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GSTIN format.");
            }

            if (customerRepository.existsByGstinIgnoreCaseAndBusinessIdAndIdNot(cleanGstin, businessId, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another customer in your business is already registered with this GSTIN.");
            }
            customer.setGstin(cleanGstin);

            if (customer.getPan() == null || customer.getPan().isBlank()) {
                customer.setPan(IndianTaxValidator.extractPanFromGstin(cleanGstin));
            }
        } else {
            customer.setGstin(null);
        }

        if (changes.getPan() != null && !changes.getPan().isBlank()) {
            String cleanPan = changes.getPan().trim().toUpperCase();
            if (!IndianTaxValidator.isValidPan(cleanPan)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PAN format.");
            }
            customer.setPan(cleanPan);
        }

        customer.setBillingAddress(changes.getBillingAddress());
        customer.setShippingAddress(changes.getShippingAddress());
        customer.setAddress(changes.getAddress());
        customer.setState(changes.getState());
        customer.setStateCode(changes.getStateCode());
        customer.setPhone(changes.getPhone());
        customer.setEmail(changes.getEmail());

        if (changes.getCustomerType() != null && !changes.getCustomerType().isBlank()) {
            customer.setCustomerType(changes.getCustomerType());
        }
        if (changes.getCreditLimit() != null) {
            customer.setCreditLimit(changes.getCreditLimit());
        }
        if (changes.getPaymentTerms() != null) {
            customer.setPaymentTerms(changes.getPaymentTerms());
        }
        if (changes.getOpeningBalance() != null) {
            customer.setOpeningBalance(changes.getOpeningBalance());
        }

        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        try {
            customerRepository.delete(customer);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete customer because they are linked to existing invoices.");
        }
    }
}
