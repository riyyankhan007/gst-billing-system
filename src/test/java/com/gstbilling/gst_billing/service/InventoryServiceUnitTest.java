package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Product;
import com.gstbilling.gst_billing.entity.StockMovement;
import com.gstbilling.gst_billing.entity.Warehouse;
import com.gstbilling.gst_billing.entity.WarehouseStock;
import com.gstbilling.gst_billing.repository.ProductRepository;
import com.gstbilling.gst_billing.repository.StockMovementRepository;
import com.gstbilling.gst_billing.repository.WarehouseStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceUnitTest {

    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private WarehouseService warehouseService;
    @Mock private WarehouseStockRepository warehouseStockRepository;

    private InventoryService inventoryService;
    private Business business;
    private Warehouse defaultWarehouse;
    private Product physicalProduct;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                stockMovementRepository,
                productRepository,
                currentUserService,
                warehouseService,
                warehouseStockRepository
        );

        business = new Business();
        business.setId(10L);
        business.setName("Acme Logistics");

        defaultWarehouse = new Warehouse();
        defaultWarehouse.setId(101L);
        defaultWarehouse.setName("Central Depot");
        defaultWarehouse.setCode("DEP-01");
        defaultWarehouse.setBusiness(business);
        defaultWarehouse.setDefault(true);

        physicalProduct = new Product();
        physicalProduct.setId(201L);
        physicalProduct.setName("Steel Rods");
        physicalProduct.setProductType("PRODUCT");
        physicalProduct.setStockQuantity(BigDecimal.valueOf(50));
        physicalProduct.setBusiness(business);
    }

    @Test
    @DisplayName("getAllMovements returns stock movements for current tenant business")
    void testGetAllMovementsSuccess() {
        when(currentUserService.getCurrentTenantId()).thenReturn(10L);

        StockMovement mov1 = new StockMovement(business, defaultWarehouse, physicalProduct, "OPENING_STOCK",
                BigDecimal.valueOf(50), BigDecimal.ZERO, BigDecimal.valueOf(50), "INIT", "Initial count");
        mov1.setId(1L);

        when(stockMovementRepository.findByBusiness_IdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(mov1));

        List<StockMovement> result = inventoryService.getAllMovements();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("OPENING_STOCK", result.get(0).getMovementType());
        verify(stockMovementRepository).findByBusiness_IdOrderByCreatedAtDesc(10L);
    }

    @Test
    @DisplayName("getAllMovements with warehouseId filters by specific warehouse for current tenant")
    void testGetAllMovementsWithWarehouseFilter() {
        when(currentUserService.getCurrentTenantId()).thenReturn(10L);

        StockMovement mov1 = new StockMovement(business, defaultWarehouse, physicalProduct, "PURCHASE",
                BigDecimal.valueOf(20), BigDecimal.valueOf(50), BigDecimal.valueOf(70), "PUR-101", "Inward stock");
        mov1.setId(2L);

        when(stockMovementRepository.findByWarehouse_IdAndBusiness_IdOrderByCreatedAtDesc(101L, 10L))
                .thenReturn(List.of(mov1));

        List<StockMovement> result = inventoryService.getAllMovements(101L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(stockMovementRepository).findByWarehouse_IdAndBusiness_IdOrderByCreatedAtDesc(101L, 10L);
    }

    @Test
    @DisplayName("getAllMovements throws 403 FORBIDDEN when no active business context is present")
    void testGetAllMovementsNoTenantThrowsForbidden() {
        when(currentUserService.getCurrentTenantId()).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> inventoryService.getAllMovements());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    @DisplayName("getMovementsForProduct verifies product belongs to business and returns product movements")
    void testGetMovementsForProductSuccess() {
        when(currentUserService.getCurrentTenantId()).thenReturn(10L);
        when(productRepository.findByIdAndBusinessId(201L, 10L)).thenReturn(Optional.of(physicalProduct));

        StockMovement mov = new StockMovement(business, defaultWarehouse, physicalProduct, "INVOICE",
                BigDecimal.valueOf(-5), BigDecimal.valueOf(50), BigDecimal.valueOf(45), "INV-1001", "Sale");
        when(stockMovementRepository.findByProduct_IdAndBusiness_IdOrderByCreatedAtDesc(201L, 10L))
                .thenReturn(List.of(mov));

        List<StockMovement> result = inventoryService.getMovementsForProduct(201L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("INVOICE", result.get(0).getMovementType());
        verify(productRepository).findByIdAndBusinessId(201L, 10L);
    }

    @Test
    @DisplayName("getMovementsForProduct throws 404 NOT_FOUND when product does not exist in tenant business")
    void testGetMovementsForProductCrossTenantRejected() {
        when(currentUserService.getCurrentTenantId()).thenReturn(10L);
        when(productRepository.findByIdAndBusinessId(999L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> inventoryService.getMovementsForProduct(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(stockMovementRepository, never()).findByProduct_IdAndBusiness_IdOrderByCreatedAtDesc(anyLong(), anyLong());
    }

    @Test
    @DisplayName("adjustStock updates product and warehouse stock synchronously and records audit movement")
    void testAdjustStockSuccess() {
        when(currentUserService.getCurrentBusiness()).thenReturn(business);
        when(productRepository.findByIdAndBusinessId(201L, 10L)).thenReturn(Optional.of(physicalProduct));
        when(warehouseService.getOrCreateDefaultWarehouse(business)).thenReturn(defaultWarehouse);

        WarehouseStock stock = new WarehouseStock(business, defaultWarehouse, physicalProduct, BigDecimal.valueOf(50));
        when(warehouseStockRepository.findForUpdateByWarehouseIdAndProductId(101L, 201L))
                .thenReturn(Optional.of(stock));

        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> {
            StockMovement sm = inv.getArgument(0);
            sm.setId(55L);
            return sm;
        });

        StockMovement result = inventoryService.adjustStock(201L, BigDecimal.valueOf(80), "Recount adjustment");

        assertNotNull(result);
        assertEquals("MANUAL_ADJUSTMENT", result.getMovementType());
        assertEquals(BigDecimal.valueOf(30), result.getQuantity()); // 80 - 50 = +30
        assertEquals(BigDecimal.valueOf(50), result.getStockBefore());
        assertEquals(BigDecimal.valueOf(80), result.getStockAfter());
        assertEquals(defaultWarehouse, result.getWarehouse());

        // Verify product stock synchronized
        assertEquals(BigDecimal.valueOf(80), physicalProduct.getStockQuantity());
        verify(productRepository).save(physicalProduct);

        // Verify warehouse stock synchronized
        assertEquals(BigDecimal.valueOf(80), stock.getQuantityOnHand());
        verify(warehouseStockRepository).save(stock);
    }

    @Test
    @DisplayName("adjustStock rejects negative quantity with 400 BAD_REQUEST")
    void testAdjustStockNegativeQuantityRejected() {
        when(currentUserService.getCurrentBusiness()).thenReturn(business);
        when(productRepository.findByIdAndBusinessId(201L, 10L)).thenReturn(Optional.of(physicalProduct));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> inventoryService.adjustStock(201L, BigDecimal.valueOf(-10), "Negative count"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(productRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("adjustStock rejects service items with 400 BAD_REQUEST")
    void testAdjustStockServiceItemRejected() {
        Product serviceProduct = new Product();
        serviceProduct.setId(301L);
        serviceProduct.setName("Consulting Service");
        serviceProduct.setProductType("SERVICE");
        serviceProduct.setBusiness(business);

        when(currentUserService.getCurrentBusiness()).thenReturn(business);
        when(productRepository.findByIdAndBusinessId(301L, 10L)).thenReturn(Optional.of(serviceProduct));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> inventoryService.adjustStock(301L, BigDecimal.valueOf(10), "Invalid service adjust"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(productRepository, never()).save(any());
    }
}
