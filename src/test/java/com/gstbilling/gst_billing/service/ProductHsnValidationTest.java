package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.*;
import com.gstbilling.gst_billing.repository.HsnSacCodeRepository;
import com.gstbilling.gst_billing.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductHsnValidationTest {

    private ProductRepository productRepository;
    private CurrentUserService currentUserService;
    private HsnSacCodeRepository hsnSacCodeRepository;
    private ProductService productService;
    private User mockUser;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        currentUserService = mock(CurrentUserService.class);
        hsnSacCodeRepository = mock(HsnSacCodeRepository.class);

        productService = new ProductService(productRepository, currentUserService, hsnSacCodeRepository);

        Business biz = new Business();
        biz.setId(1L);
        biz.setName("Test Business");

        mockUser = new User();
        mockUser.setId(10L);
        mockUser.setEmail("owner@test.com");
        mockUser.setBusiness(biz);

        when(currentUserService.getCurrentUser()).thenReturn(mockUser);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Goods product successfully validates and snapshots official HSN description")
    void testGoodsWithValidHsn() {
        HsnSacCode hsn8471 = new HsnSacCode("8471", HsnSacType.HSN, "Automatic data processing machines", "4_DIGIT", BigDecimal.valueOf(18), "CBIC");
        hsn8471.setId(10L);
        when(hsnSacCodeRepository.findByCodeAndTypeAndActiveTrue("8471", HsnSacType.HSN))
                .thenReturn(Optional.of(hsn8471));

        Product p = new Product();
        p.setName("Desktop PC");
        p.setProductType("PRODUCT");
        p.setHsnCode("8471");
        p.setPrice(BigDecimal.valueOf(50000));

        Product saved = productService.createProduct(p);
        assertNotNull(saved);
        assertEquals(10L, saved.getHsnSacId());
        assertEquals("Automatic data processing machines", saved.getClassificationDescription());
        assertEquals(BigDecimal.valueOf(18), saved.getGstRate());
    }

    @Test
    @DisplayName("Service product successfully validates and snapshots official SAC description")
    void testServiceWithValidSac() {
        HsnSacCode sac9983 = new HsnSacCode("998311", HsnSacType.SAC, "IT consulting services", "TARIFF", BigDecimal.valueOf(18), "CBIC");
        sac9983.setId(20L);
        when(hsnSacCodeRepository.findByCodeAndTypeAndActiveTrue("998311", HsnSacType.SAC))
                .thenReturn(Optional.of(sac9983));

        Product s = new Product();
        s.setName("Cloud Infrastructure Consulting");
        s.setProductType("SERVICE");
        s.setHsnCode("998311");
        s.setPrice(BigDecimal.valueOf(25000));

        Product saved = productService.createProduct(s);
        assertNotNull(saved);
        assertEquals(20L, saved.getHsnSacId());
        assertEquals("IT consulting services", saved.getClassificationDescription());
    }

    @Test
    @DisplayName("Goods product rejects SAC code with 400 Bad Request")
    void testGoodsRejectSacCode() {
        Product p = new Product();
        p.setName("Laptop");
        p.setProductType("PRODUCT");
        p.setHsnCode("998311"); // SAC assigned to Goods
        p.setPrice(BigDecimal.valueOf(60000));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> productService.createProduct(p));
        assertTrue(ex.getReason().contains("Invalid HSN code") || ex.getReason().contains("Goods require a valid HSN code"));
    }

    @Test
    @DisplayName("Service product rejects HSN code with 400 Bad Request")
    void testServiceRejectHsnCode() {
        Product s = new Product();
        s.setName("Legal Advisory");
        s.setProductType("SERVICE");
        s.setHsnCode("8471"); // HSN assigned to Service
        s.setPrice(BigDecimal.valueOf(15000));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> productService.createProduct(s));
        assertTrue(ex.getReason().contains("Invalid SAC code") || ex.getReason().contains("Services require a valid SAC code"));
    }

    @Test
    @DisplayName("Product with code not in active catalogue throws 400 Bad Request")
    void testCodeNotFoundInCatalogue() {
        when(hsnSacCodeRepository.findByCodeAndTypeAndActiveTrue("84713099", HsnSacType.HSN))
                .thenReturn(Optional.empty());

        Product p = new Product();
        p.setName("Special Gear");
        p.setProductType("PRODUCT");
        p.setHsnCode("84713099");
        p.setPrice(BigDecimal.valueOf(1200));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> productService.createProduct(p));
        assertTrue(ex.getReason().contains("not found in the official active HSN catalogue"));
    }
}
