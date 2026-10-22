package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Product;
import com.gstbilling.gst_billing.entity.StockMovement;
import com.gstbilling.gst_billing.entity.Warehouse;
import com.gstbilling.gst_billing.entity.WarehouseStock;
import com.gstbilling.gst_billing.repository.ProductRepository;
import com.gstbilling.gst_billing.repository.StockMovementRepository;
import com.gstbilling.gst_billing.repository.WarehouseStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;
    private final WarehouseService warehouseService;
    private final WarehouseStockRepository warehouseStockRepository;

    @Autowired
    public InventoryService(StockMovementRepository stockMovementRepository,
                            ProductRepository productRepository,
                            CurrentUserService currentUserService,
                            WarehouseService warehouseService,
                            WarehouseStockRepository warehouseStockRepository) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
        this.warehouseService = warehouseService;
        this.warehouseStockRepository = warehouseStockRepository;
    }

    private Business resolveBusiness() {
        Business business = currentUserService.getCurrentBusiness();
        if (business == null && currentUserService.getCurrentUser() != null) {
            business = currentUserService.getCurrentUser().getBusiness();
        }
        if (business == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active business context");
        }
        return business;
    }

    private Long resolveBusinessId() {
        Long businessId = currentUserService.getCurrentTenantId();
        if (businessId == null && currentUserService.getCurrentUser() != null && currentUserService.getCurrentUser().getBusiness() != null) {
            businessId = currentUserService.getCurrentUser().getBusiness().getId();
        }
        if (businessId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active business context");
        }
        return businessId;
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getAllMovements() {
        return getAllMovements(null);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getAllMovements(Long warehouseId) {
        Long businessId = resolveBusinessId();

        if (warehouseId != null) {
            return stockMovementRepository.findByWarehouse_IdAndBusiness_IdOrderByCreatedAtDesc(warehouseId, businessId);
        }
        return stockMovementRepository.findByBusiness_IdOrderByCreatedAtDesc(businessId);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getMovementsForProduct(Long productId) {
        Long businessId = resolveBusinessId();

        productRepository.findByIdAndBusinessId(productId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or not accessible"));

        return stockMovementRepository.findByProduct_IdAndBusiness_IdOrderByCreatedAtDesc(productId, businessId);
    }

    @Transactional
    public StockMovement adjustStock(Long productId, BigDecimal newQuantity, String reason) {
        Business business = resolveBusiness();
        Long businessId = business.getId();

        Product product = productRepository.findByIdAndBusinessId(productId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or not accessible"));

        if ("SERVICE".equalsIgnoreCase(product.getProductType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot track stock for services");
        }

        BigDecimal target = newQuantity != null ? newQuantity : BigDecimal.ZERO;
        if (target.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity cannot be negative");
        }

        BigDecimal before = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
        BigDecimal diff = target.subtract(before);

        product.setStockQuantity(target);
        productRepository.save(product);

        Warehouse defaultWh = warehouseService.getOrCreateDefaultWarehouse(business);
        WarehouseStock stock = warehouseStockRepository.findForUpdateByWarehouseIdAndProductId(defaultWh.getId(), product.getId())
                .orElseGet(() -> {
                    WarehouseStock ws = new WarehouseStock(business, defaultWh, product, before);
                    return warehouseStockRepository.saveAndFlush(ws);
                });

        stock.setQuantityOnHand(target);
        stock.setUpdatedAt(LocalDateTime.now());
        warehouseStockRepository.save(stock);

        StockMovement movement = new StockMovement(
                business,
                defaultWh,
                product,
                "MANUAL_ADJUSTMENT",
                diff,
                before,
                target,
                "ADJUSTMENT",
                (reason != null && !reason.isBlank()) ? reason : "Manual inventory adjustment"
        );

        return stockMovementRepository.save(movement);
    }
}
