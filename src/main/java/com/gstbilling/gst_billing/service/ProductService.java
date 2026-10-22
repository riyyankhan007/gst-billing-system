package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.config.GstRateConfig;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;
    private final com.gstbilling.gst_billing.repository.HsnSacCodeRepository hsnSacCodeRepository;
    private final WarehouseService warehouseService;
    private final WarehouseStockRepository warehouseStockRepository;
    private final StockMovementRepository stockMovementRepository;

    public ProductService(
            ProductRepository productRepository,
            CurrentUserService currentUserService,
            com.gstbilling.gst_billing.repository.HsnSacCodeRepository hsnSacCodeRepository
    ) {
        this(productRepository, currentUserService, hsnSacCodeRepository, null, null, null);
    }

    @Autowired
    public ProductService(
            ProductRepository productRepository,
            CurrentUserService currentUserService,
            com.gstbilling.gst_billing.repository.HsnSacCodeRepository hsnSacCodeRepository,
            WarehouseService warehouseService,
            WarehouseStockRepository warehouseStockRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
        this.hsnSacCodeRepository = hsnSacCodeRepository;
        this.warehouseService = warehouseService;
        this.warehouseStockRepository = warehouseStockRepository;
        this.stockMovementRepository = stockMovementRepository;
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

    @Transactional
    public Product createProduct(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required.");
        }
        product.setName(product.getName().trim());

        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product price must be non-negative.");
        }

        if (product.getProductType() == null || product.getProductType().isBlank()) {
            product.setProductType("PRODUCT");
        } else {
            String pType = product.getProductType().trim().toUpperCase();
            product.setProductType("GOODS".equals(pType) ? "PRODUCT" : pType);
        }

        validateAndEnrichHsnSac(product);

        if (product.getGstRate() != null && !GstRateConfig.isValidRate(product.getGstRate())) {
            // Soft-clamp or validate to nearest standard rate
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GST Rate. Standard rates are 0%, 5%, 12%, 18%, 28%.");
        }

        if (product.getTaxInclusive() == null) {
            product.setTaxInclusive(false);
        }

        Business business = resolveBusiness();
        product.setBusiness(business);
        Product saved = productRepository.save(product);

        // If physical product, initialize default warehouse stock and record opening movement if stock > 0
        if (!"SERVICE".equalsIgnoreCase(saved.getProductType()) && warehouseService != null && warehouseStockRepository != null) {
            Warehouse defaultWh = warehouseService.getOrCreateDefaultWarehouse(business);
            BigDecimal initQty = saved.getStockQuantity() != null ? saved.getStockQuantity() : BigDecimal.ZERO;

            WarehouseStock ws = new WarehouseStock(business, defaultWh, saved, initQty);
            warehouseStockRepository.save(ws);

            if (initQty.compareTo(BigDecimal.ZERO) > 0 && stockMovementRepository != null) {
                StockMovement movement = new StockMovement(
                        business,
                        defaultWh,
                        saved,
                        "OPENING_STOCK",
                        initQty,
                        BigDecimal.ZERO,
                        initQty,
                        "CATALOG_INIT",
                        "Initial stock balance recorded on product creation"
                );
                stockMovementRepository.save(movement);
            }
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts(String search, String type, Boolean lowStockOnly) {
        Long businessId = resolveBusinessId();

        List<Product> products;
        if (Boolean.TRUE.equals(lowStockOnly)) {
            products = productRepository.findLowStockProducts(businessId);
        } else {
            products = productRepository.findByBusinessId(businessId);
        }

        return products.stream()
                .filter(p -> {
                    if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
                        return type.equalsIgnoreCase(p.getProductType());
                    }
                    return true;
                })
                .filter(p -> {
                    if (search != null && !search.isBlank()) {
                        String q = search.trim().toLowerCase();
                        boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(q);
                        boolean matchSku = p.getSku() != null && p.getSku().toLowerCase().contains(q);
                        boolean matchHsn = p.getHsnCode() != null && p.getHsnCode().toLowerCase().contains(q);
                        return matchName || matchSku || matchHsn;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        Long businessId = resolveBusinessId();

        return productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or not accessible"));
    }

    @Transactional
    public Product updateProduct(Long id, Product changes) {
        Product product = getProductById(id);

        if (changes.getName() != null && !changes.getName().isBlank()) {
            product.setName(changes.getName().trim());
        }
        if (changes.getPrice() != null) {
            if (changes.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price cannot be negative.");
            }
            product.setPrice(changes.getPrice());
        }
        if (changes.getGstRate() != null) {
            if (!GstRateConfig.isValidRate(changes.getGstRate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GST Rate. Standard rates are 0%, 5%, 12%, 18%, 28%.");
            }
            product.setGstRate(changes.getGstRate());
        }

        product.setSku(changes.getSku());
        product.setHsnCode(changes.getHsnCode());
        if (changes.getUnit() != null) {
            product.setUnit(changes.getUnit());
        }
        if (changes.getDiscount() != null) {
            product.setDiscount(changes.getDiscount());
        }
        if (changes.getTaxInclusive() != null) {
            product.setTaxInclusive(changes.isTaxInclusive());
        }

        if (changes.getLowStockThreshold() != null) {
            product.setLowStockThreshold(changes.getLowStockThreshold());
        }
        if (changes.getProductType() != null) {
            String pType = changes.getProductType().trim().toUpperCase();
            product.setProductType("GOODS".equals(pType) ? "PRODUCT" : pType);
        }
        if (changes.getHsnCode() != null) {
            product.setHsnCode(changes.getHsnCode());
            validateAndEnrichHsnSac(product);
        }
        product.setActive(changes.isActive());

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        try {
            productRepository.delete(product);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete product because it is referenced in existing invoices or purchase records.");
        }
    }

    private void validateAndEnrichHsnSac(Product product) {
        String hsn = product.getHsnCode();
        if (hsn == null || hsn.isBlank()) {
            product.setHsnCode(null);
            product.setHsnSacId(null);
            product.setClassificationDescription(null);
            return;
        }

        String cleanCode = hsn.trim();
        product.setHsnCode(cleanCode);

        boolean isService = "SERVICE".equalsIgnoreCase(product.getProductType());
        com.gstbilling.gst_billing.entity.HsnSacType expectedType = isService
                ? com.gstbilling.gst_billing.entity.HsnSacType.SAC
                : com.gstbilling.gst_billing.entity.HsnSacType.HSN;

        if (!com.gstbilling.gst_billing.util.HsnSacValidator.isValidFormat(cleanCode, expectedType)) {
            String help = isService
                    ? "Services require a valid SAC code (starts with 99, 4 to 6 digits)."
                    : "Goods require a valid HSN code (2, 4, 6, or 8 digits).";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + expectedType + " code '" + cleanCode + "': " + help);
        }

        var entryOpt = hsnSacCodeRepository.findByCodeAndTypeAndActiveTrue(cleanCode, expectedType);
        if (entryOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The code '" + cleanCode + "' is not found in the official active " + expectedType + " catalogue.");
        }

        var entry = entryOpt.get();
        product.setHsnSacId(entry.getId());
        product.setClassificationDescription(entry.getDescription());

        // If product has no GST rate assigned, adopt official verified rate
        if (product.getGstRate() == null && entry.getGstRate() != null) {
            product.setGstRate(entry.getGstRate());
        }
    }
}
