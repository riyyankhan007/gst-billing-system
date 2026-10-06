package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.CustomerDetailsResponse;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.entity.UserRole;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.CustomerRepository;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CustomerLedgerTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired private CustomerService customerService;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private UserRepository userRepository;

    private Business business;
    private Customer customer;

    @BeforeEach
    void setUp() {
        business = new Business();
        business.setName("Ledger Test Business");
        business = businessRepository.save(business);

        String email = "ledgertester_" + System.currentTimeMillis() + "@example.com";
        User user = new User();
        user.setName("Ledger Tester");
        user.setEmail(email);
        user.setPassword("secret");
        user.setUserRole(UserRole.OWNER);
        user.setBusiness(business);
        userRepository.save(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList())
        );

        customer = new Customer();
        customer.setName("Ledger Customer");
        customer.setBusiness(business);
        customer.setOpeningBalance(new BigDecimal("100.00"));
        customer = customerRepository.save(customer);
    }

    @Test
    @DisplayName("Customer ledger accurately includes partial payments and excludes drafts from overdue and total invoiced")
    void shouldCalculatePartialPaymentsAndExcludeDrafts() {
        // Invoice 1: Total ₹1000, Paid ₹600, Balance ₹400 (Status: PARTIALLY_PAID, Overdue)
        Invoice inv1 = new Invoice();
        inv1.setBusiness(business);
        inv1.setCustomer(customer);
        inv1.setInvoiceNumber("INV-PARTIAL-" + System.currentTimeMillis());
        inv1.setInvoiceDate(LocalDate.now().minusDays(45));
        inv1.setDueDate(LocalDate.now().minusDays(15));
        inv1.setGrandTotal(new BigDecimal("1000.00"));
        inv1.setPaidAmount(new BigDecimal("600.00"));
        inv1.setBalanceAmount(new BigDecimal("400.00"));
        inv1.setStatus("PARTIALLY_PAID");
        inv1.setTaxableAmount(new BigDecimal("847.46"));
        inv1.setTotalTax(new BigDecimal("152.54"));
        inv1.setCgst(new BigDecimal("76.27"));
        inv1.setSgst(new BigDecimal("76.27"));
        inv1.setIgst(BigDecimal.ZERO);
        inv1.setSupplierState("Delhi");
        inv1.setCustomerState("Delhi");
        invoiceRepository.save(inv1);

        // Invoice 2: DRAFT ₹5000 (Should be completely ignored in ledger sales & overdue)
        Invoice invDraft = new Invoice();
        invDraft.setBusiness(business);
        invDraft.setCustomer(customer);
        invDraft.setInvoiceNumber("INV-DRAFT-" + System.currentTimeMillis());
        invDraft.setInvoiceDate(LocalDate.now().minusDays(50));
        invDraft.setDueDate(LocalDate.now().minusDays(20));
        invDraft.setGrandTotal(new BigDecimal("5000.00"));
        invDraft.setPaidAmount(BigDecimal.ZERO);
        invDraft.setBalanceAmount(new BigDecimal("5000.00"));
        invDraft.setStatus("DRAFT");
        invDraft.setTaxableAmount(new BigDecimal("4237.29"));
        invDraft.setTotalTax(new BigDecimal("762.71"));
        invDraft.setCgst(new BigDecimal("381.35"));
        invDraft.setSgst(new BigDecimal("381.36"));
        invDraft.setIgst(BigDecimal.ZERO);
        invDraft.setSupplierState("Delhi");
        invDraft.setCustomerState("Delhi");
        invoiceRepository.save(invDraft);

        CustomerDetailsResponse response = customerService.getCustomerDetails(customer.getId());

        // totalInvoiced should only be Invoice 1 (₹1000), NOT Draft (₹5000)
        assertEquals(0, new BigDecimal("1000.00").compareTo(response.totalInvoiced()));

        // totalPaid should be ₹600 (not 0, even though not fully PAID)
        assertEquals(0, new BigDecimal("600.00").compareTo(response.totalPaid()));

        // overdueAmount should be ₹400 (remaining balance of inv1), NOT full ₹1000, and NOT draft ₹5000
        assertEquals(0, new BigDecimal("400.00").compareTo(response.overdueAmount()));

        // outstandingBalance = Total Invoiced (1000) - Total Paid (600) + Opening (100) = 500
        assertEquals(0, new BigDecimal("500.00").compareTo(response.outstandingBalance()));

        // pendingCount should be 1 (only inv1 is pending)
        assertEquals(1, response.pendingInvoicesCount());
    }
}
