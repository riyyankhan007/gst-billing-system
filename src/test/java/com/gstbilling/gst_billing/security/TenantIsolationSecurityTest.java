package com.gstbilling.gst_billing.security;

import com.gstbilling.gst_billing.entity.*;
import com.gstbilling.gst_billing.repository.*;
import com.gstbilling.gst_billing.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TenantIsolationSecurityTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private InvoiceService invoiceService;
    @Autowired private CustomerService customerService;
    @Autowired private ProductService productService;
    @Autowired private PaymentService paymentService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private InvoicePdfService invoicePdfService;
    @Autowired private ReportService reportService;

    private Business businessA;
    private Business businessB;
    private User userA;
    private User userB;

    private Customer customerA;
    private Product productA;
    private Invoice invoiceA;

    @BeforeEach
    void setUp() {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Business A and User A (Tenant A)
        businessA = new Business();
        businessA.setName("Tenant A Corp " + runId);
        businessA.setInvoicePrefix("TENA");
        businessA.setState("Karnataka");
        businessA = businessRepository.save(businessA);

        userA = new User();
        userA.setName("User A");
        userA.setEmail("user_a_" + runId + "@tenanta.com");
        userA.setPassword("passwordA123");
        userA.setBusiness(businessA);
        userA.setRole("ADMIN");
        userA = userRepository.save(userA);

        // 2. Create Business B and User B (Tenant B)
        businessB = new Business();
        businessB.setName("Tenant B Ltd " + runId);
        businessB.setInvoicePrefix("TENB");
        businessB.setState("Maharashtra");
        businessB = businessRepository.save(businessB);

        userB = new User();
        userB.setName("User B");
        userB.setEmail("user_b_" + runId + "@tenantb.com");
        userB.setPassword("passwordB123");
        userB.setBusiness(businessB);
        userB.setRole("ADMIN");
        userB = userRepository.save(userB);

        // 3. Populate data owned by Tenant A
        authenticateAs(userA, businessA);

        customerA = new Customer();
        customerA.setName("Customer of Tenant A");
        customerA.setState("Karnataka");
        customerA.setCustomerType("B2C");
        customerA = customerService.createCustomer(customerA);

        productA = new Product();
        productA.setName("Product of Tenant A");
        productA.setPrice(BigDecimal.valueOf(1000));
        productA.setGstRate(BigDecimal.valueOf(18));
        productA = productService.createProduct(productA);

        Invoice inv = new Invoice();
        inv.setCustomerId(customerA.getId());
        inv.setInvoiceDate(LocalDate.now());
        InvoiceItem item = new InvoiceItem();
        item.setProductId(productA.getId());
        item.setQuantity(BigDecimal.valueOf(2));
        inv.setItems(List.of(item));
        invoiceA = invoiceService.createInvoice(inv);

        auditLogService.log(businessA, userA.getEmail(), "CREATE", "INVOICE", invoiceA.getId(), "Invoice created by Tenant A");

        clearAuthentication();
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
        if (invoiceA != null && invoiceA.getId() != null) {
            try { invoiceRepository.deleteById(invoiceA.getId()); } catch (Exception ignored) {}
        }
        if (productA != null && productA.getId() != null) {
            try { productRepository.deleteById(productA.getId()); } catch (Exception ignored) {}
        }
        if (customerA != null && customerA.getId() != null) {
            try { customerRepository.deleteById(customerA.getId()); } catch (Exception ignored) {}
        }
        if (userA != null && userA.getId() != null) {
            try { userRepository.deleteById(userA.getId()); } catch (Exception ignored) {}
        }
        if (userB != null && userB.getId() != null) {
            try { userRepository.deleteById(userB.getId()); } catch (Exception ignored) {}
        }
        if (businessA != null && businessA.getId() != null) {
            try { auditLogRepository.deleteByBusiness_Id(businessA.getId()); } catch (Exception ignored) {}
            try { businessRepository.deleteById(businessA.getId()); } catch (Exception ignored) {}
        }
        if (businessB != null && businessB.getId() != null) {
            try { auditLogRepository.deleteByBusiness_Id(businessB.getId()); } catch (Exception ignored) {}
            try { businessRepository.deleteById(businessB.getId()); } catch (Exception ignored) {}
        }
    }

    private void authenticateAs(User user, Business business) {
        TenantContext.setTenantId(business.getId());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Tenant B cannot read Tenant A invoices")
    void testTenantBCannotReadTenantAInvoice() {
        authenticateAs(userB, businessB);

        // Cannot find Tenant A invoice by direct ID
        assertThrows(ResponseStatusException.class, () -> invoiceService.getInvoiceById(invoiceA.getId()));

        // Cannot see Tenant A invoice in invoice list
        List<Invoice> invoices = invoiceService.getAllInvoices();
        assertTrue(invoices.stream().noneMatch(i -> i.getId().equals(invoiceA.getId())));
    }

    @Test
    @DisplayName("Tenant B cannot update or cancel Tenant A invoices")
    void testTenantBCannotUpdateTenantAInvoice() {
        authenticateAs(userB, businessB);

        Invoice attemptUpdate = new Invoice();
        attemptUpdate.setNotes("Tampered by Tenant B");

        assertThrows(ResponseStatusException.class, () -> invoiceService.updateDraftInvoice(invoiceA.getId(), attemptUpdate));
        assertThrows(ResponseStatusException.class, () -> invoiceService.cancelInvoice(invoiceA.getId()));
    }

    @Test
    @DisplayName("Tenant B cannot read, update, or delete Tenant A customers")
    void testTenantBCannotAccessTenantACustomer() {
        authenticateAs(userB, businessB);

        // Cannot read
        assertThrows(ResponseStatusException.class, () -> customerService.getCustomerById(customerA.getId()));

        // Cannot list
        List<Customer> customers = customerService.getAllCustomers("", "ALL");
        assertTrue(customers.stream().noneMatch(c -> c.getId().equals(customerA.getId())));

        // Cannot update
        Customer updateAttempt = new Customer();
        updateAttempt.setName("Malicious Update");
        assertThrows(ResponseStatusException.class, () -> customerService.updateCustomer(customerA.getId(), updateAttempt));

        // Cannot delete
        assertThrows(ResponseStatusException.class, () -> customerService.deleteCustomer(customerA.getId()));
    }

    @Test
    @DisplayName("Tenant B cannot read, update, or delete Tenant A products")
    void testTenantBCannotAccessTenantAProduct() {
        authenticateAs(userB, businessB);

        // Cannot read
        assertThrows(ResponseStatusException.class, () -> productService.getProductById(productA.getId()));

        // Cannot list
        List<Product> products = productService.getAllProducts("", "ALL", false);
        assertTrue(products.stream().noneMatch(p -> p.getId().equals(productA.getId())));

        // Cannot update
        Product updateAttempt = new Product();
        updateAttempt.setName("Hacked Product");
        assertThrows(ResponseStatusException.class, () -> productService.updateProduct(productA.getId(), updateAttempt));

        // Cannot delete
        assertThrows(ResponseStatusException.class, () -> productService.deleteProduct(productA.getId()));
    }

    @Test
    @DisplayName("Tenant B cannot download Tenant A PDF documents")
    void testTenantBCannotDownloadTenantAPdf() {
        authenticateAs(userB, businessB);

        assertThrows(ResponseStatusException.class, () -> invoicePdfService.generateInvoicePdf(invoiceA.getId()));
    }

    @Test
    @DisplayName("Tenant B cannot access Tenant A audit logs")
    void testTenantBCannotAccessTenantAAuditLogs() {
        authenticateAs(userB, businessB);

        List<AuditLog> logs = auditLogService.getBusinessLogs();
        assertTrue(logs.stream().noneMatch(l -> l.getBusiness().getId().equals(businessA.getId())));
    }

    @Test
    @DisplayName("Tenant B cannot access Tenant A reports")
    void testTenantBCannotAccessTenantAReports() {
        authenticateAs(userB, businessB);

        var salesReport = reportService.getSalesReport(LocalDate.now().minusDays(7), LocalDate.now().plusDays(1));
        // Tenant B's sales report total must not reflect Tenant A's sales
        assertEquals(0, BigDecimal.ZERO.compareTo(salesReport.totalSalesAmount()));
    }

    @Test
    @DisplayName("Tenant Context correctly extracts from JWT and sets context")
    void testJwtTenantContextExtraction() {
        JwtService jwt = new JwtService("change-this-development-secret-key-to-at-least-32-bytes-long", 86400000);
        String token = jwt.generateToken(userA.getEmail(), userA.getId(), businessA.getId(), "ADMIN");

        assertEquals(businessA.getId(), jwt.extractTenantId(token));
        assertEquals(userA.getId(), jwt.extractUserId(token));
        assertEquals(userA.getEmail(), jwt.extractEmail(token));
        assertEquals("ADMIN", jwt.extractRole(token));
    }
}
