package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.ewaybill.EWayBillCancelRequest;
import com.gstbilling.gst_billing.dto.ewaybill.EWayBillGenerateRequest;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EWayBillServiceTest {

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
    @Autowired private EWayBillRepository eWayBillRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private InvoiceService invoiceService;
    @Autowired private EWayBillService eWayBillService;
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
        business.setName("E-Way Bill Business " + runId);
        business.setGstin("27AAAAA0000A1Z5");
        business.setInvoicePrefix("EW" + runId.substring(0, 2).toUpperCase());
        business.setState("Maharashtra");
        business = businessRepository.save(business);

        user = new User();
        user.setName("E-Way Bill User");
        user.setEmail("ewaybill_" + runId + "@test.com");
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setBusiness(business);
        user.setUserRole(UserRole.OWNER);
        user = userRepository.save(user);

        customer = new Customer();
        customer.setBusiness(business);
        customer.setName("EWB Customer");
        customer.setGstin("29ABCDE" + String.format("%04d", Math.abs(runId.hashCode() % 9000 + 1000)) + "F1Z5");
        customer.setEmail("cust_ewb_" + runId + "@test.com");
        customer.setState("Karnataka");
        customer = customerRepository.save(customer);

        product = new Product();
        product.setBusiness(business);
        product.setName("Heavy Machinery " + runId);
        product.setStockQuantity(BigDecimal.valueOf(10));
        product.setPrice(BigDecimal.valueOf(75000));
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
            try { eWayBillRepository.deleteByBusiness_Id(business.getId()); } catch (Exception ignored) {}
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
        item.setQuantity(BigDecimal.valueOf(1));
        item.setUnitPrice(BigDecimal.valueOf(75000));
        item.setGstRate(BigDecimal.valueOf(18));
        invoice.setItems(new ArrayList<>(List.of(item)));

        return invoiceService.createInvoice(invoice);
    }

    @Test
    @DisplayName("E-Way Bill generation calculates 12-digit number and distance-based validity period")
    void testEWayBillGenerationRegularCargo() {
        authenticate();

        Invoice issued = createAndSaveInvoice("ISSUED");

        // 350 km regular cargo -> ceil(350/200) = 2 days validity
        EWayBillGenerateRequest request = new EWayBillGenerateRequest(
                350, "MH12AB1234", "REGULAR", "ROAD", "27TRNSP1234A1Z1", "Fast Logistics"
        );

        EWayBill ewb = eWayBillService.generateEWayBill(issued.getId(), request);

        assertNotNull(ewb);
        assertNotNull(ewb.getEwbNumber());
        assertEquals(12, ewb.getEwbNumber().length(), "E-Way Bill must be 12 digits");
        assertEquals("ACTIVE", ewb.getStatus());
        assertEquals("MH12AB1234", ewb.getVehicleNumber());

        // Check validity is approx 2 days from now
        long daysValid = ChronoUnit.DAYS.between(ewb.getEwbDate().toLocalDate(), ewb.getValidUpto().toLocalDate());
        assertEquals(2, daysValid, "350 km regular cargo should provide 2 days validity");

        // Check audit log
        List<AuditLog> logs = auditLogService.getAuditLogsByEntityType(business.getId(), "INVOICE");
        assertTrue(logs.stream().anyMatch(l -> "GENERATE_EWAY_BILL".equals(l.getAction())));
    }

    @Test
    @DisplayName("Over-dimensional cargo gives 1 day per 20 km validity")
    void testEWayBillOverDimensionalCargo() {
        authenticate();

        Invoice issued = createAndSaveInvoice("ISSUED");

        // 100 km over-dimensional -> ceil(100/20) = 5 days validity
        EWayBillGenerateRequest request = new EWayBillGenerateRequest(
                100, "MH04OD9999", "OVER_DIMENSIONAL", "ROAD", null, "Heavy Movers"
        );

        EWayBill ewb = eWayBillService.generateEWayBill(issued.getId(), request);

        long daysValid = ChronoUnit.DAYS.between(ewb.getEwbDate().toLocalDate(), ewb.getValidUpto().toLocalDate());
        assertEquals(5, daysValid, "100 km over-dimensional cargo should provide 5 days validity");
    }

    @Test
    @DisplayName("E-Way Bill cancellation sets status to CANCELLED and records audit log")
    void testEWayBillCancellation() {
        authenticate();

        Invoice issued = createAndSaveInvoice("ISSUED");
        EWayBillGenerateRequest genReq = new EWayBillGenerateRequest(150, "KA01XY1111", "REGULAR", "ROAD", null, null);
        EWayBill ewb = eWayBillService.generateEWayBill(issued.getId(), genReq);

        EWayBillCancelRequest cancelReq = new EWayBillCancelRequest(2, "Vehicle breakdown");
        EWayBill cancelled = eWayBillService.cancelEWayBill(issued.getId(), ewb.getId(), cancelReq);

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("Vehicle breakdown", cancelled.getCancelReason());
        assertNotNull(cancelled.getCancelledAt());

        List<AuditLog> logs = auditLogService.getAuditLogsByEntityType(business.getId(), "INVOICE");
        assertTrue(logs.stream().anyMatch(l -> "CANCEL_EWAY_BILL".equals(l.getAction())));
    }

    @Test
    @DisplayName("E-Way Bill cannot be generated for DRAFT invoice")
    void testEWayBillRejectsDraft() {
        authenticate();

        Invoice draft = createAndSaveInvoice("DRAFT");
        EWayBillGenerateRequest genReq = new EWayBillGenerateRequest(100, "DL01AB1111", "REGULAR", "ROAD", null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                eWayBillService.generateEWayBill(draft.getId(), genReq)
        );
        assertTrue(ex.getReason().contains("E-Way Bill can only be generated for issued invoices"));
    }
}
