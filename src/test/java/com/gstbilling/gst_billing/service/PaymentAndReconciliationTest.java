package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.controller.PaymentWebhookController;
import com.gstbilling.gst_billing.dto.PaymentRequest;
import com.gstbilling.gst_billing.dto.PaymentResponse;
import com.gstbilling.gst_billing.entity.*;
import com.gstbilling.gst_billing.integration.payment.MockRazorpayProvider;
import com.gstbilling.gst_billing.integration.payment.PaymentOrderRequest;
import com.gstbilling.gst_billing.integration.payment.PaymentOrderResponse;
import com.gstbilling.gst_billing.repository.*;
import com.gstbilling.gst_billing.security.CorrelationContext;
import com.gstbilling.gst_billing.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
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
class PaymentAndReconciliationTest {

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

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentReceiptPdfService receiptPdfService;
    @Autowired private PaymentReconciliationService reconciliationService;
    @Autowired private MockRazorpayProvider mockRazorpayProvider;
    @Autowired private PaymentWebhookController webhookController;
    @Autowired private InvoiceService invoiceService;

    @Autowired(required = false) private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Business business;
    private User user;
    private Customer customer;
    private Product product;
    private String runId;

    @BeforeEach
    void setUp() {
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("ALTER TABLE payment ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'SUCCESS'");
                jdbcTemplate.execute("ALTER TABLE payment ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(100)");
                jdbcTemplate.execute("ALTER TABLE payment ADD COLUMN IF NOT EXISTS gateway_provider VARCHAR(50)");
                jdbcTemplate.execute("ALTER TABLE payment ADD COLUMN IF NOT EXISTS gateway_payment_id VARCHAR(100)");
                jdbcTemplate.execute("ALTER TABLE payment ADD COLUMN IF NOT EXISTS gateway_order_id VARCHAR(100)");
                jdbcTemplate.execute("ALTER TABLE payment ADD COLUMN IF NOT EXISTS gateway_signature VARCHAR(255)");
            } catch (Exception ignored) {}
        }

        runId = UUID.randomUUID().toString().substring(0, 6);

        business = new Business();
        business.setName("Payment Test Corp " + runId);
        business.setInvoicePrefix("PAY" + runId.substring(0, 2).toUpperCase());
        business.setGstin("27AAPFU12341Z1");
        business.setState("Maharashtra");
        business.setStateCode("27");
        business.setEmail("pay_" + runId + "@test.com");
        business.setPhone("9988776655");
        business = businessRepository.save(business);

        user = new User();
        user.setName("Pay Admin " + runId);
        user.setEmail("admin_" + runId + "@pay.com");
        user.setPassword(passwordEncoder.encode("Pass1234!"));
        user.setUserRole(UserRole.OWNER);
        user.setBusiness(business);
        user = userRepository.save(user);

        customer = new Customer();
        customer.setName("Retail Client " + runId);
        customer.setGstin("27ABCDE1234F1Z5");
        customer.setState("Maharashtra");
        customer.setStateCode("27");
        customer.setEmail("client_" + runId + "@client.com");
        customer.setBusiness(business);
        customer = customerRepository.save(customer);

        product = new Product();
        product.setName("Widget " + runId);
        product.setPrice(BigDecimal.valueOf(500.00));
        product.setGstRate(BigDecimal.valueOf(18.00));
        product.setStockQuantity(BigDecimal.valueOf(100));
        product.setActive(true);
        product.setBusiness(business);
        product = productRepository.save(product);

        TenantContext.setTenantId(business.getId());
        CorrelationContext.setCorrelationId("corr-pay-" + runId);

        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_OWNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        CorrelationContext.clear();

        if (business != null && business.getId() != null) {
            try { stockMovementRepository.deleteByBusiness_Id(business.getId()); } catch (Exception ignored) {}
            try {
                paymentRepository.findByBusiness_IdOrderByPaymentDateDesc(business.getId())
                        .forEach(p -> { try { paymentRepository.delete(p); } catch (Exception ignored) {} });
            } catch (Exception ignored) {}
            try {
                invoiceRepository.findByBusiness_Id(business.getId())
                        .forEach(i -> { try { invoiceRepository.delete(i); } catch (Exception ignored) {} });
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

    private Invoice createTestInvoice(BigDecimal qty) {
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setCustomerId(customer.getId());
        invoice.setInvoiceDate(LocalDate.now());

        InvoiceItem item = new InvoiceItem();
        item.setProductId(product.getId());
        item.setQuantity(qty);
        invoice.setItems(List.of(item));

        return invoiceService.createInvoice(invoice);
    }

    @Test
    @DisplayName("Partial and full payments generate sequential receipt numbers and update balances")
    void testPartialAndFullPaymentsWithReceiptNumbers() {
        // Grand total for 2 items @ 500 = 1000 + 18% GST (180) = 1180.00
        Invoice invoice = createTestInvoice(BigDecimal.valueOf(2));
        BigDecimal grandTotal = invoice.getGrandTotal();
        assertEquals(new BigDecimal("1180.00"), grandTotal);

        // Record Partial Payment of 500
        PaymentRequest p1 = new PaymentRequest(
                invoice.getId(),
                BigDecimal.valueOf(500.00),
                LocalDate.now(),
                "UPI",
                "UPI-TXN-1",
                "First installment"
        );
        PaymentResponse resp1 = paymentService.recordPayment(p1);

        assertNotNull(resp1.id());
        assertNotNull(resp1.receiptNumber());
        assertTrue(resp1.receiptNumber().startsWith("RCP/"));
        assertEquals("PARTIALLY_PAID", resp1.invoiceStatus());
        assertEquals(new BigDecimal("500.00"), resp1.invoicePaidAmount());
        assertEquals(new BigDecimal("680.00"), resp1.invoiceBalanceAmount());

        // Record Final Payment of 680
        PaymentRequest p2 = new PaymentRequest(
                invoice.getId(),
                BigDecimal.valueOf(680.00),
                LocalDate.now(),
                "NEFT",
                "NEFT-TXN-2",
                "Final settlement"
        );
        PaymentResponse resp2 = paymentService.recordPayment(p2);

        assertNotNull(resp2.id());
        assertNotNull(resp2.receiptNumber());
        assertTrue(resp2.receiptNumber().startsWith("RCP/"));
        assertNotEquals(resp1.receiptNumber(), resp2.receiptNumber());
        assertEquals("PAID", resp2.invoiceStatus());
        assertEquals(new BigDecimal("1180.00"), resp2.invoicePaidAmount());
        assertEquals(new BigDecimal("0.00"), resp2.invoiceBalanceAmount());
    }

    @Test
    @DisplayName("Overpayment throws Bad Request exception")
    void testOverpaymentValidation() {
        Invoice invoice = createTestInvoice(BigDecimal.valueOf(1)); // 590.00
        PaymentRequest overpay = new PaymentRequest(
                invoice.getId(),
                BigDecimal.valueOf(1000.00),
                LocalDate.now(),
                "CASH",
                null,
                "Overpaying"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> paymentService.recordPayment(overpay));
        assertTrue(ex.getMessage().contains("exceeds outstanding balance"));
    }

    @Test
    @DisplayName("Receipt PDF generation creates valid PDF bytes")
    void testReceiptPdfGeneration() {
        Invoice invoice = createTestInvoice(BigDecimal.valueOf(1)); // 590.00
        PaymentRequest req = new PaymentRequest(
                invoice.getId(),
                BigDecimal.valueOf(590.00),
                LocalDate.now(),
                "CARD",
                "CARD-1234",
                "Paid in full via card"
        );
        PaymentResponse payment = paymentService.recordPayment(req);

        byte[] pdfBytes = receiptPdfService.generateReceiptPdf(payment.id());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);

        // Verify PDF Magic Bytes (%PDF-)
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("Mock Razorpay provider generates order and verifies signature correctly")
    void testMockRazorpayOrderAndSignature() {
        PaymentOrderRequest orderReq = new PaymentOrderRequest(
                123L,
                BigDecimal.valueOf(1500.00),
                "INR",
                "Acme Corp",
                "acme@corp.com",
                "9876543210",
                "RCP_123",
                Map.of("key", "val")
        );
        PaymentOrderResponse orderResp = mockRazorpayProvider.createPaymentOrder(orderReq);

        assertNotNull(orderResp.orderId());
        assertTrue(orderResp.orderId().startsWith("order_rzp_mock_"));
        assertEquals("RAZORPAY", orderResp.provider());

        // Signature checks
        assertTrue(mockRazorpayProvider.verifyWebhookSignature("{\"test\":\"data\"}", "mock_valid_signature", "secret"));
        assertFalse(mockRazorpayProvider.verifyWebhookSignature("{\"test\":\"data\"}", "invalid_sig", "secret"));
    }

    @Test
    @DisplayName("Payment webhook captures payment and operates idempotently")
    void testPaymentWebhookCapturesPaymentIdempotently() {
        Invoice invoice = createTestInvoice(BigDecimal.valueOf(1)); // 590.00
        assertEquals(new BigDecimal("590.00"), invoice.getBalanceAmount());

        String webhookPayload = String.format("""
                {
                    "event": "payment.captured",
                    "amount": 59000,
                    "order_id": "order_rzp_mock_111",
                    "id": "pay_mock_999888",
                    "notes": {
                        "invoiceId": "%d",
                        "businessId": "%d",
                        "method": "UPI"
                    }
                }
                """, invoice.getId(), business.getId());

        // First webhook delivery
        ResponseEntity<Map<String, Object>> resp1 = webhookController.handlePaymentWebhook(
                "RAZORPAY",
                webhookPayload,
                "mock_valid_signature",
                null
        );

        assertEquals(200, resp1.getStatusCode().value());
        assertEquals("success", resp1.getBody().get("status"));
        assertEquals("PAID", resp1.getBody().get("invoiceStatus"));

        Invoice reloaded = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals("PAID", reloaded.getStatus());
        assertEquals(new BigDecimal("590.00"), reloaded.getPaidAmount());
        assertEquals(new BigDecimal("0.00"), reloaded.getBalanceAmount());

        // Second webhook delivery (idempotency check)
        ResponseEntity<Map<String, Object>> resp2 = webhookController.handlePaymentWebhook(
                "RAZORPAY",
                webhookPayload,
                "mock_valid_signature",
                null
        );

        assertEquals(200, resp2.getStatusCode().value());
        assertEquals("success", resp2.getBody().get("status"));

        // Confirm only 1 payment was recorded
        List<Payment> payments = paymentRepository.findByInvoice_IdAndBusiness_IdOrderByPaymentDateDesc(invoice.getId(), business.getId());
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Payment reconciliation service detects and corrects ledger drift")
    void testPaymentReconciliationService() {
        Invoice invoice = createTestInvoice(BigDecimal.valueOf(2)); // 1180.00

        // Legitimate payment of 600 recorded
        PaymentRequest req = new PaymentRequest(
                invoice.getId(),
                BigDecimal.valueOf(600.00),
                LocalDate.now(),
                "UPI",
                "UPI-DRIFT-1",
                "Normal payment"
        );
        paymentService.recordPayment(req);

        // Manually introduce drift on the invoice row (simulating corrupted state or interrupted legacy update)
        invoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        invoice.setPaidAmount(BigDecimal.valueOf(100.00));
        invoice.setBalanceAmount(BigDecimal.valueOf(1080.00));
        invoice.setStatus("ISSUED");
        invoiceRepository.save(invoice);

        // Run reconciliation
        PaymentReconciliationService.ReconciliationItem item = reconciliationService.reconcileInvoice(invoice.getId());

        assertTrue(item.corrected());
        assertEquals(new BigDecimal("600.00"), item.calculatedPaid());
        assertEquals(new BigDecimal("580.00"), item.calculatedBalance());
        assertEquals("PARTIALLY_PAID", item.newStatus());

        Invoice fixedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(new BigDecimal("600.00"), fixedInvoice.getPaidAmount());
        assertEquals(new BigDecimal("580.00"), fixedInvoice.getBalanceAmount());
        assertEquals("PARTIALLY_PAID", fixedInvoice.getStatus());
    }
}
