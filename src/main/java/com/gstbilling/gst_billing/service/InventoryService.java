package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Product;
import com.gstbilling.gst_billing.entity.StockMovement;
import com.gstbilling.gst_billing.repository.ProductRepository;
import com.gstbilling.gst_billing.repository.StockMovementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventoryService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    public InventoryService(StockMovementRepository stockMovementRepository,
                            ProductRepository productRepository,
                            CurrentUserService currentUserService) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getAllMovements() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return stockMovementRepository.findByBusiness_IdOrderByCreatedAtDesc(businessId);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getMovementsForProduct(Long productId) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return stockMovementRepository.findByProduct_IdAndBusiness_IdOrderByCreatedAtDesc(productId, businessId);
    }

    @Transactional
    public StockMovement adjustStock(Long productId, BigDecimal newQuantity, String reason) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        Business business = currentUserService.getCurrentUser().getBusiness();

        Product product = productRepository.findByIdAndBusinessId(productId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if ("SERVICE".equalsIgnoreCase(product.getProductType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot track stock for services");
        }

        BigDecimal before = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
        BigDecimal target = newQuantity != null ? newQuantity : BigDecimal.ZERO;
        BigDecimal diff = target.subtract(before);

        product.setStockQuantity(target);
        productRepository.save(product);

        StockMovement movement = new StockMovement(
                business,
                product,
                "MANUAL_ADJUSTMENT",
                diff,
                before,
                target,
                "ADJUSTMENT",
                reason != null ? reason : "Manual inventory adjustment"
        );

        return stockMovementRepository.save(movement);
    }
}
