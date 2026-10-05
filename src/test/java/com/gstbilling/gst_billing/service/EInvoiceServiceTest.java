package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.einvoice.EInvoiceCancelRequest;
import com.gstbilling.gst_billing.dto.einvoice.EInvoiceResult;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EInvoiceServiceTest {

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
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private InvoiceSequenceRepository invoiceSequenceRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private InvoiceService invoiceService;
    @Autowired private EInvoiceService eInvoiceService;
    @Autowired private AuditLogService auditLogService;

    private Business business;
    private User user;
    private Customer customer;
    private Product product;
    private String runId;

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID().toString().substring(0, 6);

        business = new Business();
        business.setName("E-Invoice Business " + runId);
        business.setGstin("27AAAAA0000A1Z5");
        business.setInvoicePrefix("EI" + runId.substring(0, 2).toUpperCase());
        business.setState("Maharashtra");
        business = businessRepository.save(business);

        user = new User();
        user.setName("E-Invoice User");
        user.setEmail("einvoice_" + runId + "@test.com");
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setBusiness(business);
        user.setUserRole(UserRole.OWNER);
        user = userRepository.save(user);

        customer = new Customer();
        customer.setBusiness(business);
        customer.setName("E-Invoice Customer");
        customer.setGstin("27DEFGH5678B1Z2");
        customer.setEmail("cust_ei_" + runId + "@test.com");
        customer.setState("Maharashtra");
        customer = customerRepository.save(customer);

        product = new Product();
        product.setBusiness(business);
        product.setName("Widget " + runId);
        product.setHsnCode("84713010");
        product.setStockQuantity(BigDecimal.valueOf(100));
        product.setPrice(BigDecimal.valueOf(5000));
        product.setGstRate(BigDecimal.valueOf(18));
        product = productRepository.save(product);
    }

    private void authenticate() {
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

        if (business != null && business.getId() != null) {
            try { stockMovementRepository.deleteByBusiness_Id(business.getId()); } catch (Exception ignored) {}
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
            try { invoiceSequenceRepository.deleteByBusiness_Id(business.getId()); } catch (Exception ignored) {}
            if (product != null && product.getId() != null) {
                try { productRepository.deleteById(product.getId()); } catch (Exception ignored) {}
            }
            if (customer != null && customer.getId() != null) {
                try { customerRepository.deleteById(customer.getId()); } catch (Exception ignored) {}
            }
            if (user != null && user.getId() != null) {
                try { userRepository.deleteById(user.getId()); } catch (Exception ignored) {}
            }
            try { auditLogRepository.deleteByBusiness_Id(business.getId()); } catch (Exception ignored) {}
            try { businessRepository.deleteById(business.getId()); } catch (Exception ignored) {}
        }
    }

    private Invoice createAndSaveInvoice(String status) {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(customer.getId());
        invoice.setStatus(status);
        invoice.setInvoiceDate(LocalDate.now());

        InvoiceItem item = new InvoiceItem();
        item.setProductId(product.getId());
        item.setQuantity(BigDecimal.valueOf(2));
        item.setUnitPrice(BigDecimal.valueOf(5000));
        item.setGstRate(BigDecimal.valueOf(18));
        invoice.setItems(new ArrayList<>(List.of(item)));

        return invoiceService.createInvoice(invoice);
    }

    @Test
    @DisplayName("E-Invoice generation fails if invoice is in DRAFT status")
    void testEInvoiceRejectsDraft() {
        authenticate();

        Invoice draft = createAndSaveInvoice("DRAFT");
        assertEquals("DRAFT", draft.getStatus());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                eInvoiceService.generateEInvoice(draft.getId())
        );
        assertTrue(ex.getReason().contains("E-Invoice can only be generated for issued invoices"));
    }

    @Test
    @DisplayName("E-Invoice generation produces deterministic 64-char SHA-256 IRN and signed QR code for ISSUED invoice")
    void testEInvoiceGeneration() {
        authenticate();

        Invoice issued = createAndSaveInvoice("ISSUED");
        assertEquals("ISSUED", issued.getStatus());

        EInvoiceResult result = eInvoiceService.generateEInvoice(issued.getId());

        assertTrue(result.success());
        assertNotNull(result.irn());
        assertEquals(64, result.irn().length(), "IRN must be exactly 64 hex characters");
        assertNotNull(result.ackNo());
        assertNotNull(result.ackDate());
        assertNotNull(result.signedQrCode());
        assertTrue(result.signedQrCode().contains(business.getGstin()));
        assertTrue(result.signedQrCode().contains(result.irn()));

        // Verify entity persisted in database
        Invoice reloaded = invoiceRepository.findById(issued.getId()).orElseThrow();
        assertEquals("GENERATED", reloaded.getEinvoiceStatus());
        assertEquals(result.irn(), reloaded.getIrn());
        assertEquals(result.ackNo(), reloaded.getAckNo());

        // Verify audit log
        List<AuditLog> logs = auditLogService.getAuditLogsByEntityType(business.getId(), "INVOICE");
        assertTrue(logs.stream().anyMatch(l -> "GENERATE_EINVOICE".equals(l.getAction())));
    }

    @Test
    @DisplayName("E-Invoice generation is idempotent: repeated calls return same IRN without error")
    void testEInvoiceIdempotency() {
        authenticate();

        Invoice issued = createAndSaveInvoice("ISSUED");
        EInvoiceResult first = eInvoiceService.generateEInvoice(issued.getId());
        EInvoiceResult second = eInvoiceService.generateEInvoice(issued.getId());

        assertEquals(first.irn(), second.irn());
        assertEquals(first.ackNo(), second.ackNo());
    }

    @Test
    @DisplayName("E-Invoice cancellation updates status to CANCELLED and writes audit log")
    void testEInvoiceCancellation() {
        authenticate();

        Invoice issued = createAndSaveInvoice("ISSUED");
        EInvoiceResult generated = eInvoiceService.generateEInvoice(issued.getId());
        assertTrue(generated.success());

        EInvoiceCancelRequest cancelReq = new EInvoiceCancelRequest(3, "Customer cancelled purchase order");
        EInvoiceResult cancelResult = eInvoiceService.cancelEInvoice(issued.getId(), cancelReq);

        assertTrue(cancelResult.success());
        assertEquals("CANCELLED", cancelResult.status());

        Invoice reloaded = invoiceRepository.findById(issued.getId()).orElseThrow();
        assertEquals("CANCELLED", reloaded.getEinvoiceStatus());

        List<AuditLog> logs = auditLogService.getAuditLogsByEntityType(business.getId(), "INVOICE");
        assertTrue(logs.stream().anyMatch(l -> "CANCEL_EINVOICE".equals(l.getAction()) && l.getDetails().contains("Customer cancelled")));
    }
}
