package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.WarehouseAdjustmentRequest;
import com.gstbilling.gst_billing.dto.WarehouseOpeningStockRequest;
import com.gstbilling.gst_billing.dto.WarehouseTransferRequest;
import com.gstbilling.gst_billing.entity.*;
import com.gstbilling.gst_billing.repository.*;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WarehouseStockTransferTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BusinessMembershipRepository membershipRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private WarehouseStockRepository warehouseStockRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private WarehouseService warehouseService;
    @Autowired private InvoiceService invoiceService;

    private Business business;
    private User user;
    private Warehouse warehouseA;
    private Warehouse warehouseB;
    private Product product;
    private Customer customer;

    @BeforeEach
    void setUp() {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        business = new Business();
        business.setName("Logistics HQ " + runId);
        business.setInvoicePrefix("W" + runId.substring(0, 4).toUpperCase());
        business.setState("Maharashtra");
        business = businessRepository.save(business);

        user = new User();
        user.setName("Ops Manager");
        user.setEmail("ops_" + runId + "@lhq.com");
        user.setPassword("Password@123");
        user.setBusiness(business);
        user.setRole("ADMIN");
        user = userRepository.save(user);

        membershipRepository.save(new BusinessMembership(user, business, UserRole.ADMIN, "ACTIVE"));

        TenantContext.setTenantId(business.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        // Warehouse A (Main)
        warehouseA = new Warehouse();
        warehouseA.setBusiness(business);
        warehouseA.setName("Depot A " + runId);
        warehouseA.setCode("DEP-A-" + runId);
        warehouseA.setIsDefault(true);
        warehouseA = warehouseRepository.save(warehouseA);

        // Warehouse B (Secondary)
        warehouseB = new Warehouse();
        warehouseB.setBusiness(business);
        warehouseB.setName("Depot B " + runId);
        warehouseB.setCode("DEP-B-" + runId);
        warehouseB.setIsDefault(false);
        warehouseB = warehouseRepository.save(warehouseB);

        // Product
        product = new Product();
        product.setBusiness(business);
        product.setName("Industrial Valve " + runId);
        product.setSku("VALVE-" + runId);
        product.setPrice(BigDecimal.valueOf(500));
        product.setGstRate(BigDecimal.valueOf(18));
        product.setStockQuantity(BigDecimal.ZERO);
        product = productRepository.save(product);

        // Customer
        customer = new Customer();
        customer.setBusiness(business);
        customer.setName("Acme Client " + runId);
        customer.setState("Maharashtra");
        customer = customerRepository.save(customer);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        if (business != null && business.getId() != null) {
            try { stockMovementRepository.deleteByBusiness_Id(business.getId()); } catch (Exception ignored) {}
            try { warehouseStockRepository.deleteAll(warehouseStockRepository.findByBusiness_Id(business.getId())); } catch (Exception ignored) {}
        }
        if (customer != null && customer.getId() != null) {
            try {
                invoiceRepository.findAll().stream()
                        .filter(i -> i.getCustomer() != null && customer.getId().equals(i.getCustomer().getId()))
                        .forEach(inv -> {
                            try { invoiceRepository.delete(inv); } catch (Exception ignored) {}
                        });
            } catch (Exception ignored) {}
        }
        if (product != null && product.getId() != null) {
            try { productRepository.deleteById(product.getId()); } catch (Exception ignored) {}
        }
        if (customer != null && customer.getId() != null) {
            try { customerRepository.deleteById(customer.getId()); } catch (Exception ignored) {}
        }
        if (warehouseA != null && warehouseA.getId() != null) {
            try { warehouseRepository.deleteById(warehouseA.getId()); } catch (Exception ignored) {}
        }
        if (warehouseB != null && warehouseB.getId() != null) {
            try { warehouseRepository.deleteById(warehouseB.getId()); } catch (Exception ignored) {}
        }
        if (user != null && user.getId() != null) {
            try { membershipRepository.deleteAll(membershipRepository.findActiveMembershipsByUserId(user.getId())); } catch (Exception ignored) {}
            try { userRepository.deleteById(user.getId()); } catch (Exception ignored) {}
        }
        if (business != null && business.getId() != null) {
            try { businessRepository.deleteById(business.getId()); } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("Record opening stock initializes warehouse stock and movement correctly")
    void testRecordOpeningStock() {
        WarehouseOpeningStockRequest req = new WarehouseOpeningStockRequest(
                warehouseA.getId(),
                product.getId(),
                new BigDecimal("100.00"),
                "OPENING-BATCH-1",
                "Initial inventory onboarding"
        );

        WarehouseStock stock = warehouseService.recordOpeningStock(req);

        assertNotNull(stock);
        assertEquals(0, new BigDecimal("100.00").compareTo(stock.getQuantityOnHand()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stock.getQuantityReserved()));
        assertEquals(0, new BigDecimal("100.00").compareTo(stock.getAvailableQuantity()));

        // Verify product aggregate stock is updated
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(updatedProduct.getStockQuantity()));
    }

    @Test
    @DisplayName("Inter-warehouse transfer moves stock atomically and rejects overdrafts")
    void testInterWarehouseTransfer() {
        // Initialize Warehouse A with 100 units
        warehouseService.recordOpeningStock(new WarehouseOpeningStockRequest(
                warehouseA.getId(), product.getId(), new BigDecimal("100.00"), "INIT", "Setup"
        ));

        // Transfer 35 units from Warehouse A to Warehouse B
        WarehouseTransferRequest transferReq = new WarehouseTransferRequest(
                warehouseA.getId(),
                warehouseB.getId(),
                product.getId(),
                new BigDecimal("35.00"),
                "Branch transfer"
        );
        warehouseService.transferStock(transferReq);

        // Verify balances
        List<WarehouseStock> stocksA = warehouseService.getStockByWarehouse(warehouseA.getId());
        WarehouseStock stockA = stocksA.stream().filter(s -> s.getProduct().getId().equals(product.getId())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("65.00").compareTo(stockA.getQuantityOnHand()));

        List<WarehouseStock> stocksB = warehouseService.getStockByWarehouse(warehouseB.getId());
        WarehouseStock stockB = stocksB.stream().filter(s -> s.getProduct().getId().equals(product.getId())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("35.00").compareTo(stockB.getQuantityOnHand()));

        // Attempt overdraft transfer of 70 units from Warehouse A (which only has 65)
        WarehouseTransferRequest overdraftReq = new WarehouseTransferRequest(
                warehouseA.getId(),
                warehouseB.getId(),
                product.getId(),
                new BigDecimal("70.00"),
                "Overdraft attempt"
        );
        assertThrows(ResponseStatusException.class, () -> warehouseService.transferStock(overdraftReq));
    }

    @Test
    @DisplayName("Invoice creation deducts stock from chosen warehouse and invoice cancellation reverses it")
    void testInvoiceStockDeductionAndReversal() {
        // Initialize Warehouse A with 50 units
        warehouseService.recordOpeningStock(new WarehouseOpeningStockRequest(
                warehouseA.getId(), product.getId(), new BigDecimal("50.00"), "INIT", "Setup"
        ));

        // Create Invoice for 15 units specifying Warehouse A
        Invoice invoice = new Invoice();
        invoice.setCustomerId(customer.getId());
        invoice.setWarehouseId(warehouseA.getId());
        invoice.setStatus("ISSUED");
        invoice.setInvoiceDate(LocalDate.now());

        InvoiceItem item = new InvoiceItem();
        item.setProductId(product.getId());
        item.setQuantity(new BigDecimal("15.00"));
        invoice.setItems(List.of(item));

        Invoice created = invoiceService.createInvoice(invoice);

        // Verify Warehouse A stock reduced from 50 to 35
        WarehouseStock stockAfterInvoice = warehouseStockRepository.findByWarehouseIdAndProductId(warehouseA.getId(), product.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("35.00").compareTo(stockAfterInvoice.getQuantityOnHand()));

        // Cancel Invoice
        invoiceService.cancelInvoice(created.getId(), "Customer changed mind");

        // Verify Warehouse A stock restored from 35 back to 50
        WarehouseStock stockAfterCancel = warehouseStockRepository.findByWarehouseIdAndProductId(warehouseA.getId(), product.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("50.00").compareTo(stockAfterCancel.getQuantityOnHand()));
    }
}
