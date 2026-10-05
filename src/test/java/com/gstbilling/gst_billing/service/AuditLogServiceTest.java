package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.LoginRequest;
import com.gstbilling.gst_billing.dto.PaymentRequest;
import com.gstbilling.gst_billing.entity.*;
import com.gstbilling.gst_billing.repository.*;
import com.gstbilling.gst_billing.security.CorrelationContext;
import com.gstbilling.gst_billing.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class AuditLogServiceTest {

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

    @Autowired private AuditLogService auditLogService;
    @Autowired private InvoiceService invoiceService;
    @Autowired private PaymentService paymentService;
    @Autowired private UserManagementService userManagementService;
    @Autowired private BusinessService businessService;
    @Autowired private AuthService authService;

    private Business businessA;
    private Business businessB;
    private User userA;
    private User userB;

    private Customer customerA;
    private Product productA;

    @BeforeEach
    void setUp() {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // Business A & User A
        businessA = new Business();
        businessA.setName("Audit Business A " + runId);
        businessA.setInvoicePrefix("AU" + runId.substring(0, 2).toUpperCase());
        businessA.setState("Karnataka");
        businessA = businessRepository.save(businessA);

        userA = new User();
        userA.setName("Audit User A");
        userA.setEmail("audit_a_" + runId + "@test.com");
        userA.setPassword(passwordEncoder.encode("Password@123"));
        userA.setBusiness(businessA);
        userA.setUserRole(UserRole.OWNER);
        userA = userRepository.save(userA);

        // Business B & User B (for tenant isolation check)
        businessB = new Business();
        businessB.setName("Audit Business B " + runId);
        businessB.setInvoicePrefix("BU" + runId.substring(0, 2).toUpperCase());
        businessB.setState("Maharashtra");
        businessB = businessRepository.save(businessB);

        userB = new User();
        userB.setName("Audit User B");
        userB.setEmail("audit_b_" + runId + "@test.com");
        userB.setPassword(passwordEncoder.encode("Password@123"));
        userB.setBusiness(businessB);
        userB.setUserRole(UserRole.OWNER);
        userB = userRepository.save(userB);

        // Customer & Product for Business A
        customerA = new Customer();
        customerA.setBusiness(businessA);
        customerA.setName("Audit Customer");
        customerA.setEmail("cust_" + runId + "@test.com");
        customerA.setState("Karnataka");
        customerA = customerRepository.save(customerA);

        productA = new Product();
        productA.setBusiness(businessA);
        productA.setName("Audit Item");
        productA.setPrice(BigDecimal.valueOf(1000));
        productA.setGstRate(BigDecimal.valueOf(18));
        productA = productRepository.save(productA);
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
        CorrelationContext.clear();
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();

        // Clean Business A data
        if (businessA != null && businessA.getId() != null) {
            try {
                List<Payment> payments = paymentRepository.findByBusiness_IdOrderByPaymentDateDesc(businessA.getId());
                for (Payment p : payments) {
                    try { paymentRepository.delete(p); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            try {
                List<Invoice> invs = invoiceRepository.findByBusiness_Id(businessA.getId());
                for (Invoice inv : invs) {
                    try { invoiceRepository.delete(inv); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            if (productA != null && productA.getId() != null) {
                try { productRepository.deleteById(productA.getId()); } catch (Exception ignored) {}
            }
            if (customerA != null && customerA.getId() != null) {
                try { customerRepository.deleteById(customerA.getId()); } catch (Exception ignored) {}
            }
            if (userA != null && userA.getId() != null) {
                try { userRepository.deleteById(userA.getId()); } catch (Exception ignored) {}
            }
            try { auditLogRepository.deleteByBusiness_Id(businessA.getId()); } catch (Exception ignored) {}
            try { businessRepository.deleteById(businessA.getId()); } catch (Exception ignored) {}
        }

        // Clean Business B data
        if (businessB != null && businessB.getId() != null) {
            if (userB != null && userB.getId() != null) {
                try { userRepository.deleteById(userB.getId()); } catch (Exception ignored) {}
            }
            try { auditLogRepository.deleteByBusiness_Id(businessB.getId()); } catch (Exception ignored) {}
            try { businessRepository.deleteById(businessB.getId()); } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("Invoice creation and cancellation automatically generate rich audit log entries with correlationId")
    void testInvoiceAuditLogging() {
        String expectedCorrelationId = "corr-" + UUID.randomUUID();
        CorrelationContext.setCorrelationId(expectedCorrelationId);
        authenticateAs(userA, businessA);

        // 1. Create Invoice
        Invoice inv = new Invoice();
        inv.setCustomerId(customerA.getId());
        inv.setInvoiceDate(LocalDate.now());
        InvoiceItem item = new InvoiceItem();
        item.setProductId(productA.getId());
        item.setQuantity(BigDecimal.valueOf(2));
        inv.setItems(List.of(item));

        Invoice created = invoiceService.createInvoice(inv);
        assertNotNull(created.getId());

        // Verify CREATE_INVOICE audit log
        List<AuditLog> logs = auditLogService.getBusinessLogsByEntityType("INVOICE");
        assertFalse(logs.isEmpty());

        AuditLog createLog = logs.stream()
                .filter(l -> "CREATE_INVOICE".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertNotNull(createLog);
        assertEquals(created.getId(), createLog.getEntityId());
        assertEquals(userA.getEmail(), createLog.getUserEmail());
        assertEquals(userA.getId(), createLog.getActorUserId());
        assertEquals(expectedCorrelationId, createLog.getCorrelationId());
        assertNotNull(createLog.getCreatedAt());

        // 2. Cancel Invoice
        invoiceService.cancelInvoice(created.getId());

        List<AuditLog> updatedLogs = auditLogService.getBusinessLogsByEntityType("INVOICE");
        AuditLog cancelLog = updatedLogs.stream()
                .filter(l -> "CANCEL_INVOICE".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertNotNull(cancelLog);
        assertEquals(created.getId(), cancelLog.getEntityId());
        assertEquals("CANCEL_INVOICE", cancelLog.getAction());
        assertEquals(expectedCorrelationId, cancelLog.getCorrelationId());
    }

    @Test
    @DisplayName("Payment recording and deletion generate payment audit logs")
    void testPaymentAuditLogging() {
        authenticateAs(userA, businessA);

        // Create invoice
        Invoice inv = new Invoice();
        inv.setCustomerId(customerA.getId());
        inv.setInvoiceDate(LocalDate.now());
        InvoiceItem item = new InvoiceItem();
        item.setProductId(productA.getId());
        item.setQuantity(BigDecimal.valueOf(1));
        inv.setItems(List.of(item));
        Invoice created = invoiceService.createInvoice(inv);

        // Record payment
        PaymentRequest payReq = new PaymentRequest(created.getId(), BigDecimal.valueOf(500), LocalDate.now(), "UPI", "UPI12345", "Test");
        var payResp = paymentService.recordPayment(payReq);
        assertNotNull(payResp.id());

        // Verify RECORD_PAYMENT audit log
        List<AuditLog> paymentLogs = auditLogService.getBusinessLogsByEntityType("PAYMENT");
        assertFalse(paymentLogs.isEmpty());

        AuditLog recordLog = paymentLogs.stream()
                .filter(l -> "RECORD_PAYMENT".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertNotNull(recordLog);
        assertEquals(payResp.id(), recordLog.getEntityId());
        assertTrue(recordLog.getDetails().contains("500"));

        // Delete payment
        paymentService.deletePayment(payResp.id());

        List<AuditLog> updatedLogs = auditLogService.getBusinessLogsByEntityType("PAYMENT");
        AuditLog deleteLog = updatedLogs.stream()
                .filter(l -> "DELETE_PAYMENT".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertNotNull(deleteLog);
        assertEquals(payResp.id(), deleteLog.getEntityId());
    }

    @Test
    @DisplayName("Team management actions generate user audit logs")
    void testTeamManagementAuditLogging() {
        authenticateAs(userA, businessA);

        UserManagementService.CreateUserRequest createReq = new UserManagementService.CreateUserRequest(
                "Audit Team Member",
                "member_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com",
                "Password@123",
                "ACCOUNTANT"
        );

        var memberDto = userManagementService.createUser(createReq);
        assertNotNull(memberDto.id());

        // Verify INVITE_USER audit log
        List<AuditLog> userLogs = auditLogService.getBusinessLogsByEntityType("USER");
        assertFalse(userLogs.isEmpty());

        AuditLog inviteLog = userLogs.stream()
                .filter(l -> "INVITE_USER".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertNotNull(inviteLog);
        assertEquals(memberDto.id(), inviteLog.getEntityId());
        assertTrue(inviteLog.getDetails().contains("ACCOUNTANT"));

        // Update role
        userManagementService.updateUserRole(memberDto.id(), "SALES");

        List<AuditLog> updatedLogs = auditLogService.getBusinessLogsByEntityType("USER");
        AuditLog roleLog = updatedLogs.stream()
                .filter(l -> "UPDATE_USER_ROLE".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertNotNull(roleLog);
        assertEquals(memberDto.id(), roleLog.getEntityId());
        assertTrue(roleLog.getDetails().contains("SALES"));

        // Cleanup user
        try { userRepository.deleteById(memberDto.id()); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Audit logs are strictly tenant-isolated between businesses")
    void testAuditLogTenantIsolation() {
        // Business A generates an audit log
        authenticateAs(userA, businessA);
        auditLogService.logAction("TEST_ACTION_A", "TEST", 101L, "Action performed by Business A");

        List<AuditLog> logsA = auditLogService.getBusinessLogs();
        assertFalse(logsA.isEmpty());
        assertTrue(logsA.stream().anyMatch(l -> "TEST_ACTION_A".equals(l.getAction())));

        // Switch to Business B
        authenticateAs(userB, businessB);
        List<AuditLog> logsB = auditLogService.getBusinessLogs();

        // Business B must NOT see any of Business A's audit logs
        boolean leaked = logsB.stream().anyMatch(l -> "TEST_ACTION_A".equals(l.getAction()));
        assertFalse(leaked, "Tenant B must never see Tenant A's audit logs!");
    }

    @Test
    @DisplayName("Account lockout triggers an ACCOUNT_LOCKED audit entry")
    void testAccountLockoutAuditLogging() {
        clearAuthentication();
        String victimEmail = "lock_victim_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com";

        User victim = new User();
        victim.setName("Victim");
        victim.setEmail(victimEmail);
        victim.setPassword(passwordEncoder.encode("ValidPassword@123"));
        victim.setBusiness(businessA);
        victim.setUserRole(UserRole.SALES);
        victim = userRepository.save(victim);

        LoginRequest badLogin = new LoginRequest(victimEmail, "BadPass!");

        // 5 failed login attempts
        for (int i = 1; i <= 5; i++) {
            try { authService.login(badLogin); } catch (ResponseStatusException ignored) {}
        }

        // Verify ACCOUNT_LOCKED audit log exists
        List<AuditLog> logs = auditLogRepository.findByBusiness_IdAndEntityTypeOrderByCreatedAtDesc(businessA.getId(), "USER");
        AuditLog lockLog = logs.stream()
                .filter(l -> "ACCOUNT_LOCKED".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertNotNull(lockLog, "An ACCOUNT_LOCKED audit log must be recorded when account lockout triggers");
        assertEquals(victim.getId(), lockLog.getEntityId());
        assertEquals(victimEmail, lockLog.getUserEmail());

        // Cleanup
        try { userRepository.deleteById(victim.getId()); } catch (Exception ignored) {}
    }
}
