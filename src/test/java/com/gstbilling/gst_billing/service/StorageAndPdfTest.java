package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.*;
import com.gstbilling.gst_billing.repository.*;
import com.gstbilling.gst_billing.security.CorrelationContext;
import com.gstbilling.gst_billing.security.TenantContext;
import com.gstbilling.gst_billing.storage.LocalStorageService;
import com.gstbilling.gst_billing.storage.S3StorageService;
import com.gstbilling.gst_billing.storage.StorageService;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StorageAndPdfTest {

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

    @Autowired private StorageService storageService;
    @Autowired private InvoiceService invoiceService;
    @Autowired private EInvoiceService eInvoiceService;
    @Autowired private InvoicePdfService invoicePdfService;

    private Business business;
    private User user;
    private Customer customer;
    private Product product;
    private String runId;

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID().toString().substring(0, 6);

        business = new Business();
        business.setName("Storage Test Business " + runId);
        business.setGstin("27AAAAA0000A1Z5");
        business.setInvoicePrefix("ST" + runId.substring(0, 2).toUpperCase());
        business.setState("Maharashtra");
        business = businessRepository.save(business);

        user = new User();
        user.setName("Storage User");
        user.setEmail("storage_" + runId + "@test.com");
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setBusiness(business);
        user.setUserRole(UserRole.OWNER);
        user = userRepository.save(user);

        customer = new Customer();
        customer.setBusiness(business);
        customer.setName("PDF Customer");
        customer.setEmail("cust_pdf_" + runId + "@test.com");
        customer.setState("Maharashtra");
        customer = customerRepository.save(customer);

        product = new Product();
        product.setBusiness(business);
        product.setName("Widget " + runId);
        product.setStockQuantity(BigDecimal.valueOf(100));
        product.setPrice(BigDecimal.valueOf(2500));
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

    @Test
    @DisplayName("LocalStorageService stores, retrieves, detects, and safely deletes content with path protection")
    void testLocalStorageService() {
        String testPath = "test/doc-" + UUID.randomUUID() + ".txt";
        byte[] content = "GST Billing Storage Verification Content".getBytes(StandardCharsets.UTF_8);

        // Store
        String url = storageService.store(testPath, content, "text/plain");
        assertNotNull(url);
        assertTrue(storageService.exists(testPath));

        // Retrieve
        byte[] retrieved = storageService.retrieve(testPath);
        assertNotNull(retrieved);
        assertArrayEquals(content, retrieved);

        // Delete
        assertTrue(storageService.delete(testPath));
        assertFalse(storageService.exists(testPath));

        // Path traversal rejection
        assertThrows(IllegalArgumentException.class, () ->
                storageService.store("../evil.txt", "evil".getBytes(), "text/plain")
        );
    }

    @Test
    @DisplayName("PDF Generation renders valid PDF bytes and caches rendered document")
    void testInvoicePdfGenerationAndCaching() {
        authenticate();

        Invoice invoice = new Invoice();
        invoice.setCustomerId(customer.getId());
        invoice.setStatus("ISSUED");
        invoice.setInvoiceDate(LocalDate.now());

        InvoiceItem item = new InvoiceItem();
        item.setProductId(product.getId());
        item.setQuantity(BigDecimal.valueOf(2));
        item.setUnitPrice(BigDecimal.valueOf(2500));
        item.setGstRate(BigDecimal.valueOf(18));
        invoice.setItems(new ArrayList<>(List.of(item)));

        Invoice saved = invoiceService.createInvoice(invoice);

        // Generate E-Invoice so IRN appears on PDF
        eInvoiceService.generateEInvoice(saved.getId());

        // Generate PDF
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(saved.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF bytes should be non-trivial");

        // Verify PDF Magic Bytes (%PDF-)
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5), StandardCharsets.US_ASCII);
        assertEquals("%PDF-", header);

        // Verify cached PDF is present in storage
        String cacheKey = "invoices/" + business.getId() + "/" + saved.getId() + ".pdf";
        assertTrue(storageService.exists(cacheKey), "Generated PDF should be cached in storage");

        // Subsequent call returns cached bytes
        byte[] secondCallBytes = invoicePdfService.generateInvoicePdf(saved.getId());
        assertNotNull(secondCallBytes);
        assertArrayEquals(pdfBytes, secondCallBytes);

        // Cleanup cached PDF
        storageService.delete(cacheKey);
    }
}
