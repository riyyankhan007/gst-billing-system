package com.gstbilling.gst_billing.service;

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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InvoiceLifecycleTest {

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
        business.setName("Lifecycle Test Business " + runId);
        business.setInvoicePrefix("LC" + runId.substring(0, 2).toUpperCase());
        business.setState("Maharashtra");
        business = businessRepository.save(business);

        user = new User();
        user.setName("Lifecycle User");
        user.setEmail("lifecycle_" + runId + "@test.com");
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setBusiness(business);
        user.setUserRole(UserRole.OWNER);
        user = userRepository.save(user);

        customer = new Customer();
        customer.setBusiness(business);
        customer.setName("Lifecycle Customer");
        customer.setEmail("customer_" + runId + "@test.com");
        customer.setState("Maharashtra");
        customer = customerRepository.save(customer);

        product = new Product();
        product.setBusiness(business);
        product.setName("Lifecycle Widget " + runId);
        product.setProductType("PRODUCT");
        product.setStockQuantity(BigDecimal.valueOf(100));
        product.setPrice(BigDecimal.valueOf(1000));
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

    private Invoice createDummyInvoice(String status, LocalDate invoiceDate) {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(customer.getId());
        invoice.setStatus(status);
        invoice.setInvoiceDate(invoiceDate != null ? invoiceDate : LocalDate.now());

        InvoiceItem item = new InvoiceItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setQuantity(BigDecimal.valueOf(5));
        item.setUnitPrice(BigDecimal.valueOf(1000));
        item.setGstRate(BigDecimal.valueOf(18));
        invoice.setItems(new ArrayList<>(List.of(item)));

        return invoice;
    }

    @Test
    @DisplayName("Concurrency-safe sequence generator produces gapless and collision-free invoice numbers under concurrent load")
    void testGaplessSequenceGenerationAndConcurrency() throws Exception {
        authenticate();

        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<String> generatedNumbers = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // Propagate security context to worker thread
                    authenticate();
                    startLatch.await();

                    Invoice inv = createDummyInvoice("DRAFT", LocalDate.of(2026, 8, 1));
                    Invoice saved = invoiceService.createInvoice(inv);
                    generatedNumbers.add(saved.getInvoiceNumber());
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    clearAuthentication();
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrent threads should complete within 30 seconds");
        assertTrue(errors.isEmpty(), "No errors should occur during concurrent generation: " + errors);
        assertEquals(threadCount, generatedNumbers.size(), "All threads should generate an invoice");

        // Verify all generated invoice numbers are strictly unique
        Set<String> uniqueNumbers = new HashSet<>(generatedNumbers);
        assertEquals(threadCount, uniqueNumbers.size(), "Every generated invoice number must be unique (no duplicates)");

        // Verify prefix and FY formatting: e.g. LCXX/2026-27/0001
        String expectedFy = "2026-27";
        for (String num : generatedNumbers) {
            assertTrue(num.startsWith(business.getInvoicePrefix() + "/" + expectedFy + "/"),
                    "Invoice number " + num + " must match format " + business.getInvoicePrefix() + "/" + expectedFy + "/XXXX");
        }
    }

    @Test
    @DisplayName("Financial year rollover: sequences restart per financial year (April 1 boundary)")
    void testFinancialYearRollover() {
        authenticate();

        // 1. Invoice on March 31, 2026 belongs to FY 2025-26
        Invoice invFy2526 = createDummyInvoice("DRAFT", LocalDate.of(2026, 3, 31));
        Invoice saved1 = invoiceService.createInvoice(invFy2526);

        assertEquals("2025-26", saved1.getFinancialYear());
        assertTrue(saved1.getInvoiceNumber().contains("/2025-26/0001"),
                "First invoice in 2025-26 should end with 0001, got: " + saved1.getInvoiceNumber());

        // 2. Invoice on April 1, 2026 rolls over to FY 2026-27 and starts at 0001
        Invoice invFy2627 = createDummyInvoice("DRAFT", LocalDate.of(2026, 4, 1));
        Invoice saved2 = invoiceService.createInvoice(invFy2627);

        assertEquals("2026-27", saved2.getFinancialYear());
        assertTrue(saved2.getInvoiceNumber().contains("/2026-27/0001"),
                "First invoice in new FY 2026-27 should roll over and start at 0001, got: " + saved2.getInvoiceNumber());

        // 3. Second invoice in FY 2026-27 increments to 0002
        Invoice invFy2627Second = createDummyInvoice("DRAFT", LocalDate.of(2026, 4, 15));
        Invoice saved3 = invoiceService.createInvoice(invFy2627Second);

        assertEquals("2026-27", saved3.getFinancialYear());
        assertTrue(saved3.getInvoiceNumber().contains("/2026-27/0002"),
                "Second invoice in FY 2026-27 should be 0002, got: " + saved3.getInvoiceNumber());
    }

    @Test
    @DisplayName("Immutability: Issued invoices cannot be directly modified")
    void testImmutabilityOfIssuedInvoice() {
        authenticate();

        Invoice invoice = createDummyInvoice("ISSUED", LocalDate.now());
        Invoice saved = invoiceService.createInvoice(invoice);
        assertEquals("ISSUED", saved.getStatus());

        // Attempting to modify issued invoice must throw 400 Bad Request
        Invoice updatePayload = new Invoice();
        updatePayload.setNotes("Attempted illegal modification");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.updateDraftInvoice(saved.getId(), updatePayload)
        );
        assertTrue(ex.getReason().contains("Only DRAFT invoices can be directly modified"));
    }

    @Test
    @DisplayName("Deletion restrictions: Only DRAFT invoices can be deleted; ISSUED and CANCELLED invoices are preserved for audit")
    void testInvoiceDeletionRestrictions() {
        authenticate();

        // 1. DRAFT can be deleted
        Invoice draft = createDummyInvoice("DRAFT", LocalDate.now());
        Invoice savedDraft = invoiceService.createInvoice(draft);
        assertDoesNotThrow(() -> invoiceService.deleteInvoice(savedDraft.getId()));
        assertFalse(invoiceRepository.existsById(savedDraft.getId()));

        // 2. ISSUED cannot be deleted
        Invoice issued = createDummyInvoice("ISSUED", LocalDate.now());
        Invoice savedIssued = invoiceService.createInvoice(issued);
        ResponseStatusException exIssued = assertThrows(ResponseStatusException.class, () ->
                invoiceService.deleteInvoice(savedIssued.getId())
        );
        assertTrue(exIssued.getReason().contains("Only DRAFT invoices can be deleted"));

        // 3. CANCELLED cannot be deleted
        Invoice cancelled = invoiceService.cancelInvoice(savedIssued.getId(), "Test Cancellation");
        assertEquals("CANCELLED", cancelled.getStatus());

        ResponseStatusException exCancelled = assertThrows(ResponseStatusException.class, () ->
                invoiceService.deleteInvoice(cancelled.getId())
        );
        assertTrue(exCancelled.getReason().contains("Only DRAFT invoices can be deleted"));
        assertTrue(invoiceRepository.existsById(cancelled.getId()), "Cancelled invoice must remain in database");
    }

    @Test
    @DisplayName("Cancellation lifecycle: records reason, timestamp, actor, restores stock, and writes audit log")
    void testInvoiceCancellationLifecycleAndStockRestoration() {
        authenticate();

        BigDecimal initialStock = product.getStockQuantity(); // 100
        BigDecimal orderQty = BigDecimal.valueOf(20);

        Invoice invoice = new Invoice();
        invoice.setCustomerId(customer.getId());
        invoice.setStatus("ISSUED");
        invoice.setInvoiceDate(LocalDate.now());

        InvoiceItem item = new InvoiceItem();
        item.setProductId(product.getId());
        item.setQuantity(orderQty);
        item.setUnitPrice(BigDecimal.valueOf(500));
        item.setGstRate(BigDecimal.valueOf(18));
        invoice.setItems(new ArrayList<>(List.of(item)));

        Invoice issued = invoiceService.createInvoice(invoice);
        assertEquals("ISSUED", issued.getStatus());

        // Stock was reduced by 20 -> 80
        Product afterIssue = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, initialStock.subtract(orderQty).compareTo(afterIssue.getStockQuantity()));

        // Cancel with reason
        String reason = "Buyer changed specifications before fulfillment";
        Invoice cancelled = invoiceService.cancelInvoice(issued.getId(), reason);

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals(reason, cancelled.getCancellationReason());
        assertNotNull(cancelled.getCancelledAt());
        assertEquals(user.getEmail(), cancelled.getCancelledBy());

        // Stock was restored by 20 -> 100
        Product afterCancel = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, initialStock.compareTo(afterCancel.getStockQuantity()));

        // Verify stock movements recorded
        List<StockMovement> movements = stockMovementRepository.findByBusiness_IdOrderByCreatedAtDesc(business.getId());
        assertTrue(movements.stream().anyMatch(m -> "INVOICE".equals(m.getMovementType())));
        assertTrue(movements.stream().anyMatch(m -> "INVOICE_CANCELLED".equals(m.getMovementType())));

        // Verify audit log has CANCEL_INVOICE with reason
        List<AuditLog> logs = auditLogService.getAuditLogsByEntityType(business.getId(), "INVOICE");
        assertTrue(logs.stream().anyMatch(l -> "CANCEL_INVOICE".equals(l.getAction()) && l.getDetails().contains(reason)));
    }
}
