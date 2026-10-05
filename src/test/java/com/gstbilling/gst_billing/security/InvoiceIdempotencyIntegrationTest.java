package com.gstbilling.gst_billing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Product;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.entity.UserRole;
import com.gstbilling.gst_billing.repository.AuditLogRepository;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.CustomerRepository;
import com.gstbilling.gst_billing.repository.IdempotencyKeyRepository;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.repository.InvoiceSequenceRepository;
import com.gstbilling.gst_billing.repository.ProductRepository;
import com.gstbilling.gst_billing.repository.StockMovementRepository;
import com.gstbilling.gst_billing.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class InvoiceIdempotencyIntegrationTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private CorrelationIdFilter correlationIdFilter;
    @Autowired private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired private IdempotencyFilter idempotencyFilter;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired private JwtService jwtService;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private IdempotencyKeyRepository idempotencyKeyRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private InvoiceSequenceRepository invoiceSequenceRepository;

    private Business business;
    private User user;
    private Customer customer;
    private Product product;
    private String jwtToken;
    private String runId;

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID().toString().substring(0, 6);

        // 1. Business (do not hardcode fixed GSTIN to prevent unique constraint collision)
        business = new Business();
        business.setName("Idemp Test Corp " + runId);
        business.setState("Maharashtra");
        business.setStateCode("27");
        business.setInvoicePrefix("ID" + runId.substring(0, 2).toUpperCase());
        business = businessRepository.save(business);

        // 2. User
        user = new User();
        user.setName("Owner " + runId);
        user.setEmail("owner." + runId + "@idemptest.com");
        user.setPassword("secretHash");
        user.setUserRole(UserRole.OWNER);
        user.setBusiness(business);
        user = userRepository.save(user);

        // 3. Customer
        customer = new Customer();
        customer.setName("Retail Client " + runId);
        customer.setState("Maharashtra");
        customer.setStateCode("27");
        customer.setBusiness(business);
        customer = customerRepository.save(customer);

        // 4. Product
        product = new Product();
        product.setName("Widget " + runId);
        product.setPrice(new BigDecimal("100.00"));
        product.setGstRate(new BigDecimal("18.00"));
        product.setStockQuantity(new BigDecimal("500.00"));
        product.setBusiness(business);
        product = productRepository.save(product);

        // 5. Auth Token with Tenant ID
        jwtToken = jwtService.generateToken(user.getEmail(), user.getId(), business.getId(), user.getRole());

        // 6. Build MockMvc with full filter chain (Correlation -> JWT Auth -> Idempotency)
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(correlationIdFilter, jwtAuthenticationFilter, idempotencyFilter)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (business != null && business.getId() != null) {
            cleanUpBusinessData(business.getId());
            if (product != null && product.getId() != null) {
                try { productRepository.deleteById(product.getId()); } catch (Exception ignored) {}
            }
            if (customer != null && customer.getId() != null) {
                try { customerRepository.deleteById(customer.getId()); } catch (Exception ignored) {}
            }
            if (user != null && user.getId() != null) {
                try { userRepository.deleteById(user.getId()); } catch (Exception ignored) {}
            }
            try { businessRepository.deleteById(business.getId()); } catch (Exception ignored) {}
        }
    }

    private void cleanUpBusinessData(Long businessId) {
        try { idempotencyKeyRepository.deleteByBusiness_Id(businessId); } catch (Exception ignored) {}
        try { stockMovementRepository.deleteByBusiness_Id(businessId); } catch (Exception ignored) {}
        try {
            var invs = invoiceRepository.findByBusiness_Id(businessId);
            for (var inv : invs) {
                try { invoiceRepository.delete(inv); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        try { invoiceSequenceRepository.deleteByBusiness_Id(businessId); } catch (Exception ignored) {}
        try { auditLogRepository.deleteByBusiness_Id(businessId); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("POST /api/invoices with X-Idempotency-Key succeeds, deserializes body, and replays cached response on retry")
    void testCreateInvoiceWithIdempotencyKey() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        Map<String, Object> invoicePayload = Map.of(
                "customerId", customer.getId(),
                "invoiceDate", "2026-10-05",
                "dueDate", "2026-11-05",
                "discountAmount", 0,
                "notes", "Automated Idempotency Verification",
                "termsAndConditions", "Net 30",
                "status", "ISSUED",
                "items", List.of(
                        Map.of(
                                "productId", product.getId(),
                                "quantity", 25,
                                "discount", 0
                        )
                )
        );
        String jsonBody = objectMapper.writeValueAsString(invoicePayload);

        // --- FIRST REQUEST ---
        MvcResult firstResult = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.invoiceNumber").exists())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.grandTotal").value(2950.0))
                .andReturn();

        String firstResponseBody = firstResult.getResponse().getContentAsString();
        long invoiceCountAfterFirst = invoiceRepository.findByBusiness_Id(business.getId()).size();
        assertEquals(1, invoiceCountAfterFirst, "Exactly one invoice should be created in database");

        // --- SECOND REQUEST (Exact same idempotency key and payload) ---
        MvcResult secondResult = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Cache-Lookup", "HIT-IDEMPOTENT"))
                .andReturn();

        String secondResponseBody = secondResult.getResponse().getContentAsString();
        assertEquals(firstResponseBody, secondResponseBody, "Cached response must exactly match the first response");

        long invoiceCountAfterSecond = invoiceRepository.findByBusiness_Id(business.getId()).size();
        assertEquals(1, invoiceCountAfterSecond, "Replayed idempotent request must NOT create a duplicate invoice");
    }

    @Test
    @DisplayName("Reusing same X-Idempotency-Key with different payload is rejected with 422 Unprocessable Entity")
    void testIdempotencyPayloadMismatch() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        Map<String, Object> payload1 = Map.of(
                "customerId", customer.getId(),
                "invoiceDate", "2026-10-05",
                "status", "ISSUED",
                "items", List.of(Map.of("productId", product.getId(), "quantity", 10, "discount", 0))
        );

        Map<String, Object> payload2 = Map.of(
                "customerId", customer.getId(),
                "invoiceDate", "2026-10-05",
                "status", "ISSUED",
                "items", List.of(Map.of("productId", product.getId(), "quantity", 20, "discount", 0))
        );

        // First request succeeds
        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload1)))
                .andExpect(status().isCreated());

        // Second request with same key but different body -> 422 Unprocessable Entity
        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload2)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Failed request releases idempotency key so client can retry")
    void testFailedRequestReleasesIdempotencyKey() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        // Customer ID that does not exist -> downstream throws EntityNotFoundException or 400/404
        Map<String, Object> invalidPayload = Map.of(
                "customerId", 999999L,
                "invoiceDate", "2026-10-05",
                "status", "ISSUED",
                "items", List.of(Map.of("productId", product.getId(), "quantity", 5, "discount", 0))
        );

        // Attempt creation with invalid customer
        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() >= 400));

        // Verify the key does not leave an IN_PROGRESS lock in DB
        var keyRecord = idempotencyKeyRepository.findByBusiness_IdAndIdempotencyKey(business.getId(), idempotencyKey);
        assertTrue(keyRecord.isEmpty() || !keyRecord.get().isInFlight(),
                "Failed request should not leave an active IN_PROGRESS idempotency lock");
    }

    @Test
    @DisplayName("Tenant isolation: Two different tenants can use identical idempotency keys without conflict")
    void testMultiTenantIdempotencyIsolation() throws Exception {
        String sharedKey = "shared-uuid-" + runId;

        // Tenant B setup
        Business businessB = new Business();
        businessB.setName("Tenant B " + runId);
        businessB.setState("Karnataka");
        businessB.setStateCode("29");
        businessB.setInvoicePrefix("TB" + runId.substring(0, 2).toUpperCase());
        businessB = businessRepository.save(businessB);

        User userB = new User();
        userB.setName("User B " + runId);
        userB.setEmail("userb." + runId + "@tenantb.com");
        userB.setPassword("secret");
        userB.setUserRole(UserRole.OWNER);
        userB.setBusiness(businessB);
        userB = userRepository.save(userB);

        Customer customerB = new Customer();
        customerB.setName("Customer B");
        customerB.setState("Karnataka");
        customerB.setStateCode("29");
        customerB.setBusiness(businessB);
        customerB = customerRepository.save(customerB);

        Product productB = new Product();
        productB.setName("Product B");
        productB.setPrice(new BigDecimal("50.00"));
        productB.setGstRate(new BigDecimal("18.00"));
        productB.setBusiness(businessB);
        productB = productRepository.save(productB);

        String tokenB = jwtService.generateToken(userB.getEmail(), userB.getId(), businessB.getId(), userB.getRole());

        // Tenant A creates invoice with sharedKey
        Map<String, Object> payloadA = Map.of(
                "customerId", customer.getId(),
                "invoiceDate", "2026-10-05",
                "status", "ISSUED",
                "items", List.of(Map.of("productId", product.getId(), "quantity", 1, "discount", 0))
        );

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Idempotency-Key", sharedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadA)))
                .andExpect(status().isCreated());

        // Tenant B creates invoice with same sharedKey without collision
        Map<String, Object> payloadB = Map.of(
                "customerId", customerB.getId(),
                "invoiceDate", "2026-10-05",
                "status", "ISSUED",
                "items", List.of(Map.of("productId", productB.getId(), "quantity", 2, "discount", 0))
        );

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Idempotency-Key", sharedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadB)))
                .andExpect(status().isCreated());

        // Clean up Tenant B
        cleanUpBusinessData(businessB.getId());
        try { productRepository.deleteById(productB.getId()); } catch (Exception ignored) {}
        try { customerRepository.deleteById(customerB.getId()); } catch (Exception ignored) {}
        try { userRepository.deleteById(userB.getId()); } catch (Exception ignored) {}
        try { businessRepository.deleteById(businessB.getId()); } catch (Exception ignored) {}
    }
}
