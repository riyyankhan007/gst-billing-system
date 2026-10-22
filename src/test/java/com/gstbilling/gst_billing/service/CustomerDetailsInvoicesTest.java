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
import com.gstbilling.gst_billing.security.CorrelationIdFilter;
import com.gstbilling.gst_billing.security.JwtAuthenticationFilter;
import com.gstbilling.gst_billing.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class CustomerDetailsInvoicesTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private CorrelationIdFilter correlationIdFilter;
    @Autowired private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired private JwtService jwtService;

    @Autowired private CustomerService customerService;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private UserRepository userRepository;

    private MockMvc mockMvc;
    private Business business;
    private Customer customer;
    private User user;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        business = new Business();
        business.setName("Customer Details Test Business");
        business = businessRepository.save(business);

        String email = "cust_details_" + System.currentTimeMillis() + "@example.com";
        user = new User();
        user.setName("Cust Details Tester");
        user.setEmail(email);
        user.setPassword("secret");
        user.setUserRole(UserRole.OWNER);
        user.setBusiness(business);
        user = userRepository.save(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList())
        );

        jwtToken = jwtService.generateToken(user.getEmail(), user.getId(), business.getId(), user.getRole());

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(correlationIdFilter, jwtAuthenticationFilter)
                .build();

        customer = new Customer();
        customer.setName("Demo Customer");
        customer.setBusiness(business);
        customer.setCustomerType("B2B");
        customer.setOpeningBalance(new BigDecimal("500.00"));
        customer = customerRepository.save(customer);
    }

    private Invoice createInvoice(Customer cust, String num, LocalDate date, LocalDate due, String status, BigDecimal total, BigDecimal balance) {
        Invoice inv = new Invoice();
        inv.setBusiness(cust.getBusiness());
        inv.setCustomer(cust);
        inv.setInvoiceNumber(num);
        inv.setInvoiceDate(date);
        inv.setDueDate(due);
        inv.setStatus(status);
        inv.setGrandTotal(total);
        inv.setPaidAmount(total.subtract(balance));
        inv.setBalanceAmount(balance);
        inv.setTaxableAmount(total.multiply(new BigDecimal("0.847")));
        inv.setTotalTax(total.multiply(new BigDecimal("0.153")));
        inv.setCgst(total.multiply(new BigDecimal("0.0765")));
        inv.setSgst(total.multiply(new BigDecimal("0.0765")));
        inv.setIgst(BigDecimal.ZERO);
        inv.setSupplierState("Delhi");
        inv.setCustomerState("Delhi");
        return invoiceRepository.save(inv);
    }

    @Test
    @DisplayName("Customer details service returns all customer invoices in reverse chronological order with overdue updated")
    void shouldReturnAllCustomerInvoicesInOrder() {
        long ts = System.currentTimeMillis();
        String inv1Num = "INV-CDT-" + ts + "-001";
        String inv2Num = "INV-CDT-" + ts + "-002";
        String inv3Num = "INV-CDT-" + ts + "-003";
        String inv4Num = "INV-CDT-" + ts + "-004";
        String inv5Num = "INV-CDT-" + ts + "-005";

        createInvoice(customer, inv1Num, LocalDate.now().minusDays(30), LocalDate.now().minusDays(15), "PAID", new BigDecimal("1000.00"), BigDecimal.ZERO);
        createInvoice(customer, inv2Num, LocalDate.now().minusDays(20), LocalDate.now().minusDays(5), "ISSUED", new BigDecimal("2000.00"), new BigDecimal("2000.00"));
        createInvoice(customer, inv3Num, LocalDate.now().minusDays(10), LocalDate.now().plusDays(10), "CANCELLED", new BigDecimal("1500.00"), BigDecimal.ZERO);
        createInvoice(customer, inv4Num, LocalDate.now().minusDays(5), LocalDate.now().plusDays(15), "DRAFT", new BigDecimal("500.00"), new BigDecimal("500.00"));
        createInvoice(customer, inv5Num, LocalDate.now(), LocalDate.now().plusDays(30), "ISSUED", new BigDecimal("3000.00"), new BigDecimal("3000.00"));

        // Another customer in the same business
        Customer customerB = new Customer();
        customerB.setName("Other Customer");
        customerB.setBusiness(business);
        customerB = customerRepository.save(customerB);
        createInvoice(customerB, "INV-OTHER-" + ts, LocalDate.now(), LocalDate.now().plusDays(15), "ISSUED", new BigDecimal("999.00"), new BigDecimal("999.00"));

        CustomerDetailsResponse response = customerService.getCustomerDetails(customer.getId());

        assertNotNull(response);
        assertEquals(customer.getId(), response.customer().getId());
        assertEquals("Demo Customer", response.customer().getName());

        List<Invoice> invoices = response.invoices();
        assertNotNull(invoices);
        assertEquals(5, invoices.size());
        assertEquals(5, response.totalInvoicesCount());

        // Backward compatibility: recentInvoices() equals invoices()
        assertEquals(invoices.size(), response.recentInvoices().size());

        // Reverse chronological order: newest invoiceDate first
        assertEquals(inv5Num, invoices.get(0).getInvoiceNumber());
        assertEquals(inv4Num, invoices.get(1).getInvoiceNumber());
        assertEquals(inv3Num, invoices.get(2).getInvoiceNumber());
        assertEquals(inv2Num, invoices.get(3).getInvoiceNumber());
        assertEquals(inv1Num, invoices.get(4).getInvoiceNumber());

        // Customer B invoices excluded
        assertFalse(invoices.stream().anyMatch(i -> i.getInvoiceNumber().startsWith("INV-OTHER-")));

        // Overdue status checked and updated
        Invoice overdueInv = invoices.stream().filter(i -> inv2Num.equals(i.getInvoiceNumber())).findFirst().orElseThrow();
        assertEquals("OVERDUE", overdueInv.getStatus());

        // Cancelled and draft invoices preserved
        assertTrue(invoices.stream().anyMatch(i -> "CANCELLED".equals(i.getStatus())));
        assertTrue(invoices.stream().anyMatch(i -> "DRAFT".equals(i.getStatus())));
    }

    @Test
    @DisplayName("API integration: GET /api/customers/{id}/details returns 200 with both 'invoices' and 'recentInvoices' arrays")
    void shouldReturnCustomerDetailsViaApiWithBothInvoicesAndRecentInvoices() throws Exception {
        long ts = System.currentTimeMillis();
        String invNum1 = "INV-API-" + ts + "-001";
        String invNum2 = "INV-API-" + ts + "-002";

        createInvoice(customer, invNum1, LocalDate.now().minusDays(10), LocalDate.now().plusDays(10), "PAID", new BigDecimal("1000.00"), BigDecimal.ZERO);
        createInvoice(customer, invNum2, LocalDate.now(), LocalDate.now().plusDays(20), "ISSUED", new BigDecimal("2500.00"), new BigDecimal("2500.00"));

        mockMvc.perform(get("/api/customers/" + customer.getId() + "/details")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.id").value(customer.getId()))
                .andExpect(jsonPath("$.customer.name").value("Demo Customer"))
                .andExpect(jsonPath("$.totalInvoicesCount").value(2))
                .andExpect(jsonPath("$.invoices", hasSize(2)))
                .andExpect(jsonPath("$.recentInvoices", hasSize(2)))
                .andExpect(jsonPath("$.invoices[0].invoiceNumber").value(invNum2))
                .andExpect(jsonPath("$.invoices[1].invoiceNumber").value(invNum1))
                .andExpect(jsonPath("$.recentInvoices[0].invoiceNumber").value(invNum2))
                .andExpect(jsonPath("$.recentInvoices[1].invoiceNumber").value(invNum1));
    }

    @Test
    @DisplayName("Customer details for customer with no invoices returns empty list and 0 count")
    void shouldReturnEmptyForCustomerWithNoInvoices() throws Exception {
        CustomerDetailsResponse response = customerService.getCustomerDetails(customer.getId());

        assertNotNull(response);
        assertNotNull(response.invoices());
        assertTrue(response.invoices().isEmpty());
        assertEquals(0, response.totalInvoicesCount());
        assertEquals(0, response.pendingInvoicesCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.totalInvoiced()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.totalPaid()));
        assertEquals(0, new BigDecimal("500.00").compareTo(response.outstandingBalance()));

        // Also verify via HTTP API
        mockMvc.perform(get("/api/customers/" + customer.getId() + "/details")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvoicesCount").value(0))
                .andExpect(jsonPath("$.invoices", hasSize(0)))
                .andExpect(jsonPath("$.recentInvoices", hasSize(0)));
    }

    @Test
    @DisplayName("Multi-tenant isolation: accessing customer of another business returns 404 via service and API")
    void shouldRejectAccessToCustomerFromAnotherBusiness() throws Exception {
        Business otherBusiness = new Business();
        otherBusiness.setName("Rival Corp " + System.currentTimeMillis());
        otherBusiness = businessRepository.save(otherBusiness);

        Customer rivalCustomer = new Customer();
        rivalCustomer.setName("Rival Client");
        rivalCustomer.setBusiness(otherBusiness);
        final Customer savedRival = customerRepository.save(rivalCustomer);

        // 1. Direct service call throws NOT_FOUND
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                customerService.getCustomerDetails(savedRival.getId())
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        // 2. HTTP endpoint returns 404
        mockMvc.perform(get("/api/customers/" + savedRival.getId() + "/details")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }
}
