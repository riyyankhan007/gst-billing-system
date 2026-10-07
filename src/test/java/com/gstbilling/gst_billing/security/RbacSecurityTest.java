package com.gstbilling.gst_billing.security;

import com.gstbilling.gst_billing.controller.*;
import com.gstbilling.gst_billing.dto.CreateInvoiceRequest;
import com.gstbilling.gst_billing.dto.InvoiceItemRequest;
import com.gstbilling.gst_billing.dto.LoginRequest;
import com.gstbilling.gst_billing.dto.PaymentRequest;
import com.gstbilling.gst_billing.entity.*;
import com.gstbilling.gst_billing.repository.*;
import com.gstbilling.gst_billing.service.AuthService;
import com.gstbilling.gst_billing.service.UserManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RbacSecurityTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthService authService;

    // Controllers under RBAC test
    @Autowired private InvoiceController invoiceController;
    @Autowired private PaymentController paymentController;
    @Autowired private UserManagementController userManagementController;
    @Autowired private AuditLogController auditLogController;
    @Autowired private BusinessController businessController;
    @Autowired private CustomerController customerController;
    @Autowired private ProductController productController;

    private Business business;
    private User ownerUser;
    private User adminUser;
    private User accountantUser;
    private User salesUser;
    private User viewerUser;
    private User supportUser;

    private Customer customer;
    private Product product;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Business with unique prefix
        business = new Business();
        business.setName("RBAC Enterprise " + runId);
        business.setInvoicePrefix("R" + runId.substring(0, 4).toUpperCase());
        business.setState("Karnataka");
        business = businessRepository.save(business);

        // 2. Create Users with different roles
        ownerUser = createUser("Owner User", "owner_" + runId + "@test.com", UserRole.OWNER);
        adminUser = createUser("Admin User", "admin_" + runId + "@test.com", UserRole.ADMIN);
        accountantUser = createUser("Accountant User", "accountant_" + runId + "@test.com", UserRole.ACCOUNTANT);
        salesUser = createUser("Sales User", "sales_" + runId + "@test.com", UserRole.SALES);
        viewerUser = createUser("Viewer User", "viewer_" + runId + "@test.com", UserRole.VIEWER);
        supportUser = createUser("Support User", "support_" + runId + "@test.com", UserRole.SUPPORT);

        // 3. Create baseline customer and product
        customer = new Customer();
        customer.setBusiness(business);
        customer.setName("Acme Client");
        customer.setEmail("client@acme.com");
        customer.setState("Karnataka");
        customer = customerRepository.save(customer);

        product = new Product();
        product.setBusiness(business);
        product.setName("Consulting Unit");
        product.setPrice(BigDecimal.valueOf(5000));
        product.setGstRate(BigDecimal.valueOf(18));
        product = productRepository.save(product);

        // 4. Create an invoice
        testInvoice = new Invoice();
        testInvoice.setBusiness(business);
        testInvoice.setCustomer(customer);
        testInvoice.setCustomerId(customer.getId());
        testInvoice.setInvoiceNumber("RBAC-INV-" + runId);
        testInvoice.setInvoiceDate(LocalDate.now());
        testInvoice.setStatus("DRAFT");
        testInvoice.setSupplierState("Karnataka");
        testInvoice.setCustomerState("Karnataka");
        testInvoice.setTaxableAmount(BigDecimal.valueOf(5000));
        testInvoice.setCgst(BigDecimal.valueOf(450));
        testInvoice.setSgst(BigDecimal.valueOf(450));
        testInvoice.setIgst(BigDecimal.ZERO);
        testInvoice.setTotalTax(BigDecimal.valueOf(900));
        testInvoice.setGrandTotal(BigDecimal.valueOf(5900));
        testInvoice.setBalanceAmount(BigDecimal.valueOf(5900));
        testInvoice = invoiceRepository.save(testInvoice);
    }

    private User createUser(String name, String email, UserRole role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("SecurePass123!"));
        u.setBusiness(business);
        u.setUserRole(role);
        return userRepository.save(u);
    }

    private void authenticateAs(User user) {
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

    @AfterEach
    void tearDown() {
        clearAuthentication();

        if (business != null && business.getId() != null) {
            try {
                List<Payment> payments = paymentRepository.findByBusiness_IdOrderByPaymentDateDesc(business.getId());
                for (Payment p : payments) {
                    try { paymentRepository.delete(p); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            try {
                List<Invoice> invs = invoiceRepository.findByBusiness_Id(business.getId());
                for (Invoice inv : invs) {
                    try { invoiceRepository.delete(inv); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        if (product != null && product.getId() != null) {
            try { productRepository.deleteById(product.getId()); } catch (Exception ignored) {}
        }
        if (customer != null && customer.getId() != null) {
            try { customerRepository.deleteById(customer.getId()); } catch (Exception ignored) {}
        }
        if (ownerUser != null && ownerUser.getId() != null) {
            try { userRepository.deleteById(ownerUser.getId()); } catch (Exception ignored) {}
        }
        if (adminUser != null && adminUser.getId() != null) {
            try { userRepository.deleteById(adminUser.getId()); } catch (Exception ignored) {}
        }
        if (accountantUser != null && accountantUser.getId() != null) {
            try { userRepository.deleteById(accountantUser.getId()); } catch (Exception ignored) {}
        }
        if (salesUser != null && salesUser.getId() != null) {
            try { userRepository.deleteById(salesUser.getId()); } catch (Exception ignored) {}
        }
        if (viewerUser != null && viewerUser.getId() != null) {
            try { userRepository.deleteById(viewerUser.getId()); } catch (Exception ignored) {}
        }
        if (supportUser != null && supportUser.getId() != null) {
            try { userRepository.deleteById(supportUser.getId()); } catch (Exception ignored) {}
        }
        if (business != null && business.getId() != null) {
            try { auditLogRepository.deleteByBusiness_Id(business.getId()); } catch (Exception ignored) {}
            try { businessRepository.deleteById(business.getId()); } catch (Exception ignored) {}
        }
    }

    // ==========================================
    // 1. VIEWER ROLE TESTS
    // ==========================================

    @Test
    @DisplayName("VIEWER role can read invoices and customers, but cannot mutate or view audit logs")
    void testViewerPermissions() {
        authenticateAs(viewerUser);

        // Can read invoices
        assertNotNull(invoiceController.getAllInvoices(null, null));
        assertNotNull(invoiceController.getInvoiceById(testInvoice.getId()));

        // Can read customers
        assertNotNull(customerController.getAllCustomers(null, null));

        // CANNOT create invoice
        CreateInvoiceRequest newInv = new CreateInvoiceRequest(
                customer.getId(), LocalDate.now(), null, null, BigDecimal.ZERO, false, null, null, null, "DRAFT",
                List.of(new InvoiceItemRequest(product.getId(), null, null, BigDecimal.ONE, null, null, BigDecimal.ZERO, false))
        );
        assertThrows(AccessDeniedException.class, () -> invoiceController.createInvoice(newInv));

        // CANNOT delete invoice
        assertThrows(AccessDeniedException.class, () -> invoiceController.deleteInvoice(testInvoice.getId()));

        // CANNOT record payment
        PaymentRequest payReq = new PaymentRequest(testInvoice.getId(), BigDecimal.valueOf(500), LocalDate.now(), "CASH", null, null);
        assertThrows(AccessDeniedException.class, () -> paymentController.recordPayment(payReq));

        // CANNOT view audit logs
        assertThrows(AccessDeniedException.class, () -> auditLogController.getAuditLogs());

        // CANNOT manage team
        assertThrows(AccessDeniedException.class, () -> userManagementController.getBusinessUsers());

        // CANNOT update business
        assertThrows(AccessDeniedException.class, () -> businessController.updateBusiness(business));
    }

    // ==========================================
    // 2. SALES ROLE TESTS
    // ==========================================

    @Test
    @DisplayName("SALES role can create invoices, but cannot cancel/delete invoices, record payments, or view audit logs")
    void testSalesPermissions() {
        authenticateAs(salesUser);

        // CAN create invoice
        CreateInvoiceRequest newInv = new CreateInvoiceRequest(
                customer.getId(), LocalDate.now(), null, null, BigDecimal.ZERO, false, null, null, null, "DRAFT",
                List.of(new InvoiceItemRequest(product.getId(), null, null, BigDecimal.ONE, null, null, BigDecimal.ZERO, false))
        );

        var resp = invoiceController.createInvoice(newInv);
        assertNotNull(resp.getBody());
        Long createdInvoiceId = resp.getBody().getId();

        // CANNOT delete invoice
        assertThrows(AccessDeniedException.class, () -> invoiceController.deleteInvoice(createdInvoiceId));

        // CANNOT cancel invoice
        assertThrows(AccessDeniedException.class, () -> invoiceController.cancelInvoice(createdInvoiceId));

        // CANNOT record payment
        PaymentRequest payReq = new PaymentRequest(createdInvoiceId, BigDecimal.valueOf(100), LocalDate.now(), "CASH", null, null);
        assertThrows(AccessDeniedException.class, () -> paymentController.recordPayment(payReq));

        // CANNOT view audit logs
        assertThrows(AccessDeniedException.class, () -> auditLogController.getAuditLogs());

        // CANNOT manage team
        assertThrows(AccessDeniedException.class, () -> userManagementController.getBusinessUsers());
    }

    // ==========================================
    // 3. ACCOUNTANT ROLE TESTS
    // ==========================================

    @Test
    @DisplayName("ACCOUNTANT role can create invoices and record payments, but cannot manage team or audit logs")
    void testAccountantPermissions() {
        authenticateAs(accountantUser);

        // CAN create invoice
        CreateInvoiceRequest newInv = new CreateInvoiceRequest(
                customer.getId(), LocalDate.now(), null, null, BigDecimal.ZERO, false, null, null, null, "DRAFT",
                List.of(new InvoiceItemRequest(product.getId(), null, null, BigDecimal.valueOf(2), null, null, BigDecimal.ZERO, false))
        );

        var resp = invoiceController.createInvoice(newInv);
        assertNotNull(resp.getBody());
        Long invId = resp.getBody().getId();

        // CAN record payment
        PaymentRequest payReq = new PaymentRequest(invId, BigDecimal.valueOf(500), LocalDate.now(), "BANK_TRANSFER", "REF123", "Deposit");
        var payResp = paymentController.recordPayment(payReq);
        assertNotNull(payResp.getBody());

        // CANNOT manage team
        assertThrows(AccessDeniedException.class, () -> userManagementController.getBusinessUsers());

        // CANNOT view audit logs
        assertThrows(AccessDeniedException.class, () -> auditLogController.getAuditLogs());
    }

    // ==========================================
    // 4. SUPPORT ROLE TESTS
    // ==========================================

    @Test
    @DisplayName("SUPPORT role can view audit logs and invoices, but cannot create invoices or payments")
    void testSupportPermissions() {
        authenticateAs(supportUser);

        // CAN view audit logs
        assertNotNull(auditLogController.getAuditLogs());

        // CAN read invoices
        assertNotNull(invoiceController.getAllInvoices(null, null));

        // CANNOT create invoice
        CreateInvoiceRequest newInv = new CreateInvoiceRequest(
                customer.getId(), LocalDate.now(), null, null, BigDecimal.ZERO, false, null, null, null, "DRAFT",
                List.of(new InvoiceItemRequest(product.getId(), null, null, BigDecimal.ONE, null, null, BigDecimal.ZERO, false))
        );
        assertThrows(AccessDeniedException.class, () -> invoiceController.createInvoice(newInv));

        // CANNOT record payment
        PaymentRequest payReq = new PaymentRequest(testInvoice.getId(), BigDecimal.valueOf(100), LocalDate.now(), "CASH", null, null);
        assertThrows(AccessDeniedException.class, () -> paymentController.recordPayment(payReq));

        // CANNOT manage users
        assertThrows(AccessDeniedException.class, () -> userManagementController.getBusinessUsers());
    }

    // ==========================================
    // 5. ADMIN & OWNER ROLE TESTS
    // ==========================================

    @Test
    @DisplayName("ADMIN role can manage team members, but cannot assign OWNER role")
    void testAdminPermissions() {
        authenticateAs(adminUser);

        // CAN view team
        assertNotNull(userManagementController.getBusinessUsers());

        // CAN create team member with SALES role
        UserManagementService.CreateUserRequest req = new UserManagementService.CreateUserRequest(
                "Sales Agent", "agent_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "Password@123", "SALES"
        );
        var created = userManagementController.createUser(req);
        assertNotNull(created.getBody());
        assertEquals("SALES", created.getBody().role());

        // CANNOT assign OWNER role (privilege escalation check)
        UserManagementService.CreateUserRequest ownerReq = new UserManagementService.CreateUserRequest(
                "Hacked Owner", "hacked_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "Password@123", "OWNER"
        );
        assertThrows(ResponseStatusException.class, () -> userManagementController.createUser(ownerReq));
    }

    @Test
    @DisplayName("OWNER role can manage all resources and assign any role including OWNER")
    void testOwnerPermissions() {
        authenticateAs(ownerUser);

        // Full access to team
        assertNotNull(userManagementController.getBusinessUsers());

        // Full access to audit logs
        assertNotNull(auditLogController.getAuditLogs());

        // Full access to business update
        Business updateReq = new Business();
        updateReq.setName("Owner Updated Name");
        assertNotNull(businessController.updateBusiness(updateReq));

        // Full access to invoices
        assertNotNull(invoiceController.getAllInvoices(null, null));
    }

    // ==========================================
    // 6. BRUTE-FORCE LOCKOUT TESTS
    // ==========================================

    @Test
    @DisplayName("5 consecutive failed logins lock account for 15 minutes; 6th attempt is blocked even with correct password")
    void testBruteForceLockout() {
        clearAuthentication();
        String testEmail = "lockout_victim_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
        User victim = createUser("Lockout Victim", testEmail, UserRole.SALES);

        LoginRequest badRequest = new LoginRequest(testEmail, "WrongPassword!");

        // First 4 attempts fail with 401 UNAUTHORIZED
        for (int i = 1; i <= 4; i++) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(badRequest));
            assertEquals(401, ex.getStatusCode().value());
            assertEquals("Invalid email or password", ex.getReason());
        }

        // 5th attempt fails and triggers lockout
        ResponseStatusException fifthEx = assertThrows(ResponseStatusException.class, () -> authService.login(badRequest));
        assertEquals(401, fifthEx.getStatusCode().value());
        assertTrue(fifthEx.getReason().contains("Account is temporarily locked"));

        // Verify entity state in DB
        User lockedUser = userRepository.findById(victim.getId()).orElseThrow();
        assertEquals(5, lockedUser.getFailedLoginAttempts());
        assertNotNull(lockedUser.getLockedUntil());
        assertTrue(lockedUser.isAccountLocked());

        // 6th attempt with CORRECT password must STILL BE REJECTED while locked
        LoginRequest correctRequest = new LoginRequest(testEmail, "SecurePass123!");
        ResponseStatusException lockedEx = assertThrows(ResponseStatusException.class, () -> authService.login(correctRequest));
        assertEquals(401, lockedEx.getStatusCode().value());
        assertTrue(lockedEx.getReason().contains("Account is temporarily locked"));
    }

    @Test
    @DisplayName("Successful login resets failed login attempts counter to 0")
    void testSuccessfulLoginResetsFailedAttempts() {
        clearAuthentication();
        String testEmail = "reset_test_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
        User user = createUser("Reset Test User", testEmail, UserRole.ACCOUNTANT);

        // 2 failed attempts
        LoginRequest badRequest = new LoginRequest(testEmail, "WrongPassword!");
        assertThrows(ResponseStatusException.class, () -> authService.login(badRequest));
        assertThrows(ResponseStatusException.class, () -> authService.login(badRequest));

        User userAfterFails = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(2, userAfterFails.getFailedLoginAttempts());

        // Successful login
        LoginRequest goodRequest = new LoginRequest(testEmail, "SecurePass123!");
        var authResponse = authService.login(goodRequest);
        assertNotNull(authResponse.token());

        // Counter must be reset to 0
        User userAfterSuccess = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(0, userAfterSuccess.getFailedLoginAttempts());
        assertNull(userAfterSuccess.getLockedUntil());
    }
}
