package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.WarehouseAdjustmentRequest;
import com.gstbilling.gst_billing.dto.WarehouseOpeningStockRequest;
import com.gstbilling.gst_billing.dto.WarehouseTransferRequest;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Product;
import com.gstbilling.gst_billing.entity.StockMovement;
import com.gstbilling.gst_billing.entity.Warehouse;
import com.gstbilling.gst_billing.entity.WarehouseStock;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.ProductRepository;
import com.gstbilling.gst_billing.repository.StockMovementRepository;
import com.gstbilling.gst_billing.repository.WarehouseRepository;
import com.gstbilling.gst_billing.repository.WarehouseStockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final ProductRepository productRepository;
    private final BusinessRepository businessRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public WarehouseService(
            WarehouseRepository warehouseRepository,
            WarehouseStockRepository warehouseStockRepository,
            ProductRepository productRepository,
            BusinessRepository businessRepository,
            StockMovementRepository stockMovementRepository,
            CurrentUserService currentUserService,
            AuditLogService auditLogService
    ) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseStockRepository = warehouseStockRepository;
        this.productRepository = productRepository;
        this.businessRepository = businessRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getWarehouses() {
        Long businessId = currentUserService.getCurrentTenantId();
        return warehouseRepository.findByBusiness_IdOrderByIdAsc(businessId);
    }

    @Transactional(readOnly = true)
    public Warehouse getWarehouseById(Long id) {
        Long businessId = currentUserService.getCurrentTenantId();
        return warehouseRepository.findByIdAndBusiness_Id(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found with ID: " + id));
    }

    @Transactional
    public Warehouse getOrCreateDefaultWarehouse(Business business) {
        return warehouseRepository.findByBusiness_IdAndIsDefaultTrue(business.getId())
                .orElseGet(() -> {
                    Warehouse wh = new Warehouse(
                            business,
                            "Main Warehouse",
                            "MAIN",
                            business.getAddress(),
                            business.getState(),
                            business.getStateCode(),
                            true
                    );
                    return warehouseRepository.save(wh);
                });
    }

    @Transactional
    public Warehouse createWarehouse(Warehouse warehouse) {
        Long businessId = currentUserService.getCurrentTenantId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));

        if (warehouse.getName() == null || warehouse.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse name is required");
        }

        String code = warehouse.getCode() != null ? warehouse.getCode().trim().toUpperCase() : "WH-" + System.currentTimeMillis() % 10000;
        if (warehouseRepository.existsByBusiness_IdAndCodeIgnoreCase(businessId, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A warehouse with code '" + code + "' already exists in this business");
        }

        warehouse.setBusiness(business);
        warehouse.setCode(code);
        warehouse.setCreatedAt(LocalDateTime.now());

        if (warehouse.isDefault()) {
            // Unset previous default warehouse
            warehouseRepository.findByBusiness_IdAndIsDefaultTrue(businessId).ifPresent(prev -> {
                prev.setDefault(false);
                warehouseRepository.save(prev);
            });
        }

        Warehouse saved = warehouseRepository.save(warehouse);
        auditLogService.logAction("CREATE_WAREHOUSE", "WAREHOUSE", saved.getId(), "Created warehouse: " + saved.getName() + " (" + saved.getCode() + ")");
        return saved;
    }

    @Transactional
    public Warehouse updateWarehouse(Long id, Warehouse updates) {
        Warehouse warehouse = getWarehouseById(id);

        if (updates.getName() != null && !updates.getName().isBlank()) {
            warehouse.setName(updates.getName().trim());
        }

        if (updates.getCode() != null && !updates.getCode().isBlank()) {
            String newCode = updates.getCode().trim().toUpperCase();
            if (warehouseRepository.existsByBusiness_IdAndCodeIgnoreCaseAndIdNot(warehouse.getBusiness().getId(), newCode, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another warehouse with code '" + newCode + "' already exists");
            }
            warehouse.setCode(newCode);
        }

        if (updates.getAddress() != null) warehouse.setAddress(updates.getAddress());
        if (updates.getState() != null) warehouse.setState(updates.getState());
        if (updates.getStateCode() != null) warehouse.setStateCode(updates.getStateCode());
        if (updates.getContactPerson() != null) warehouse.setContactPerson(updates.getContactPerson());
        if (updates.getContactPhone() != null) warehouse.setContactPhone(updates.getContactPhone());
        warehouse.setActive(updates.isActive());

        if (updates.isDefault() && !warehouse.isDefault()) {
            warehouseRepository.findByBusiness_IdAndIsDefaultTrue(warehouse.getBusiness().getId()).ifPresent(prev -> {
                prev.setDefault(false);
                warehouseRepository.save(prev);
            });
            warehouse.setDefault(true);
        }

        warehouse.setUpdatedAt(LocalDateTime.now());
        return warehouseRepository.save(warehouse);
    }

    @Transactional(readOnly = true)
    public List<WarehouseStock> getStockByWarehouse(Long warehouseId) {
        Warehouse warehouse = getWarehouseById(warehouseId);
        return warehouseStockRepository.findByWarehouse_Id(warehouse.getId());
    }

    @Transactional
    public WarehouseStock recordOpeningStock(WarehouseOpeningStockRequest request) {
        Long businessId = currentUserService.getCurrentTenantId();
        Warehouse warehouse = warehouseRepository.findByIdAndBusiness_Id(request.warehouseId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));

        Product product = productRepository.findByIdAndBusinessId(request.productId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if ("SERVICE".equalsIgnoreCase(product.getProductType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot track stock for service items");
        }

        BigDecimal qty = request.quantity() != null ? request.quantity() : BigDecimal.ZERO;
        if (qty.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opening stock cannot be negative");
        }

        WarehouseStock stock = warehouseStockRepository.findForUpdateByWarehouseIdAndProductId(warehouse.getId(), product.getId())
                .orElseGet(() -> {
                    WarehouseStock ws = new WarehouseStock(warehouse.getBusiness(), warehouse, product, BigDecimal.ZERO);
                    return warehouseStockRepository.saveAndFlush(ws);
                });

        BigDecimal before = stock.getQuantityOnHand();
        BigDecimal diff = qty.subtract(before);
        stock.setQuantityOnHand(qty);
        stock.setUpdatedAt(LocalDateTime.now());
        warehouseStockRepository.save(stock);

        // Synchronize consolidated product stock
        BigDecimal currentProdStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
        product.setStockQuantity(currentProdStock.add(diff));
        productRepository.save(product);

        StockMovement movement = new StockMovement(
                warehouse.getBusiness(),
                warehouse,
                product,
                "OPENING_STOCK",
                diff,
                before,
                qty,
                request.reference(),
                (request.notes() != null && !request.notes().isBlank()) ? request.notes() : "Initial opening stock recorded"
        );
        stockMovementRepository.save(movement);

        auditLogService.logAction("OPENING_STOCK", "WAREHOUSE_STOCK", stock.getId(),
                "Opening stock set to " + qty + " for " + product.getName() + " in warehouse " + warehouse.getName());

        return stock;
    }

    @Transactional
    public void transferStock(WarehouseTransferRequest request) {
        Long businessId = currentUserService.getCurrentTenantId();

        if (request.sourceWarehouseId().equals(request.destinationWarehouseId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination warehouses cannot be the same");
        }

        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer quantity must be greater than zero");
        }

        Warehouse source = warehouseRepository.findByIdAndBusiness_Id(request.sourceWarehouseId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source warehouse not found in this business"));

        Warehouse dest = warehouseRepository.findByIdAndBusiness_Id(request.destinationWarehouseId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination warehouse not found in this business"));

        Product product = productRepository.findByIdAndBusinessId(request.productId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found in this business"));

        if ("SERVICE".equalsIgnoreCase(product.getProductType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer service items");
        }

        // Lock in consistent order by ID to prevent deadlocks
        Long firstId = source.getId() < dest.getId() ? source.getId() : dest.getId();
        Long secondId = source.getId() < dest.getId() ? dest.getId() : source.getId();

        WarehouseStock stockFirst = getOrCreateStockForUpdate(firstId, product);
        WarehouseStock stockSecond = getOrCreateStockForUpdate(secondId, product);

        WarehouseStock sourceStock = source.getId().equals(firstId) ? stockFirst : stockSecond;
        WarehouseStock destStock = dest.getId().equals(firstId) ? stockFirst : stockSecond;

        if (sourceStock.getAvailableQuantity().compareTo(request.quantity()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Insufficient stock in source warehouse '%s'. Available: %s, Requested: %s",
                            source.getName(), sourceStock.getAvailableQuantity(), request.quantity()));
        }

        BigDecimal srcBefore = sourceStock.getQuantityOnHand();
        BigDecimal srcAfter = srcBefore.subtract(request.quantity());
        sourceStock.setQuantityOnHand(srcAfter);
        sourceStock.setUpdatedAt(LocalDateTime.now());
        warehouseStockRepository.save(sourceStock);

        BigDecimal destBefore = destStock.getQuantityOnHand();
        BigDecimal destAfter = destBefore.add(request.quantity());
        destStock.setQuantityOnHand(destAfter);
        destStock.setUpdatedAt(LocalDateTime.now());
        warehouseStockRepository.save(destStock);

        String ref = "TRF-" + System.currentTimeMillis();

        StockMovement outMovement = new StockMovement(
                source.getBusiness(),
                source,
                product,
                "TRANSFER_OUT",
                request.quantity().negate(),
                srcBefore,
                srcAfter,
                ref,
                "Transferred to " + dest.getName() + (request.notes() != null ? " - " + request.notes() : "")
        );
        outMovement.setTargetWarehouse(dest);
        stockMovementRepository.save(outMovement);

        StockMovement inMovement = new StockMovement(
                dest.getBusiness(),
                dest,
                product,
                "TRANSFER_IN",
                request.quantity(),
                destBefore,
                destAfter,
                ref,
                "Transferred from " + source.getName() + (request.notes() != null ? " - " + request.notes() : "")
        );
        inMovement.setTargetWarehouse(source);
        stockMovementRepository.save(inMovement);

        auditLogService.logAction("STOCK_TRANSFER", "WAREHOUSE", source.getId(),
                String.format("Transferred %s units of %s from %s to %s", request.quantity(), product.getName(), source.getName(), dest.getName()));
    }

    @Transactional
    public WarehouseStock adjustStock(WarehouseAdjustmentRequest request) {
        Long businessId = currentUserService.getCurrentTenantId();

        Warehouse warehouse = warehouseRepository.findByIdAndBusiness_Id(request.warehouseId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));

        Product product = productRepository.findByIdAndBusinessId(request.productId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if ("SERVICE".equalsIgnoreCase(product.getProductType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot adjust stock for service items");
        }

        WarehouseStock stock = getOrCreateStockForUpdate(warehouse.getId(), product);
        BigDecimal before = stock.getQuantityOnHand();
        BigDecimal newQty = request.newQuantity();

        if (newQty == null || newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity cannot be negative");
        }

        BigDecimal diff = newQty.subtract(before);
        stock.setQuantityOnHand(newQty);
        stock.setUpdatedAt(LocalDateTime.now());
        warehouseStockRepository.save(stock);

        // Update product consolidated stock
        BigDecimal prodStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
        product.setStockQuantity(prodStock.add(diff));
        productRepository.save(product);

        StockMovement movement = new StockMovement(
                warehouse.getBusiness(),
                warehouse,
                product,
                "MANUAL_ADJUSTMENT",
                diff,
                before,
                newQty,
                request.reference(),
                (request.reason() != null && !request.reason().isBlank()) ? request.reason() : "Manual inventory adjustment"
        );
        stockMovementRepository.save(movement);

        auditLogService.logAction("STOCK_ADJUSTMENT", "WAREHOUSE_STOCK", stock.getId(),
                String.format("Adjusted stock for %s in %s from %s to %s. Reason: %s",
                        product.getName(), warehouse.getName(), before, newQty, request.reason()));

        return stock;
    }

    @Transactional
    public void deductInvoiceStock(Warehouse warehouse, Product product, BigDecimal quantity, String invoiceNumber) {
        if ("SERVICE".equalsIgnoreCase(product.getProductType())) return;

        WarehouseStock stock = getOrCreateStockForUpdate(warehouse.getId(), product);
        BigDecimal before = stock.getQuantityOnHand();
        BigDecimal after = before.subtract(quantity);

        stock.setQuantityOnHand(after);
        stock.setUpdatedAt(LocalDateTime.now());
        warehouseStockRepository.save(stock);

        // Synchronize consolidated product stock
        BigDecimal prodBefore = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
        product.setStockQuantity(prodBefore.subtract(quantity));
        productRepository.save(product);

        StockMovement movement = new StockMovement(
                warehouse.getBusiness(),
                warehouse,
                product,
                "INVOICE",
                quantity.negate(),
                before,
                after,
                invoiceNumber,
                "Deducted for issued invoice #" + invoiceNumber
        );
        stockMovementRepository.save(movement);
    }

    @Transactional
    public void reverseInvoiceStock(Warehouse warehouse, Product product, BigDecimal quantity, String invoiceNumber) {
        if ("SERVICE".equalsIgnoreCase(product.getProductType())) return;

        WarehouseStock stock = getOrCreateStockForUpdate(warehouse.getId(), product);
        BigDecimal before = stock.getQuantityOnHand();
        BigDecimal after = before.add(quantity);

        stock.setQuantityOnHand(after);
        stock.setUpdatedAt(LocalDateTime.now());
        warehouseStockRepository.save(stock);

        // Synchronize consolidated product stock
        BigDecimal prodBefore = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
        product.setStockQuantity(prodBefore.add(quantity));
        productRepository.save(product);

        StockMovement movement = new StockMovement(
                warehouse.getBusiness(),
                warehouse,
                product,
                "INVOICE_CANCELLED",
                quantity,
                before,
                after,
                invoiceNumber,
                "Stock restored on cancellation of invoice #" + invoiceNumber
        );
        stockMovementRepository.save(movement);
    }

    private WarehouseStock getOrCreateStockForUpdate(Long warehouseId, Product product) {
        return warehouseStockRepository.findForUpdateByWarehouseIdAndProductId(warehouseId, product.getId())
                .orElseGet(() -> {
                    Warehouse wh = warehouseRepository.findById(warehouseId).orElseThrow();
                    WarehouseStock ws = new WarehouseStock(wh.getBusiness(), wh, product, BigDecimal.ZERO);
                    return warehouseStockRepository.saveAndFlush(ws);
                });
    }
}
