package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.config.GstRateConfig;
import com.gstbilling.gst_billing.entity.Product;
import com.gstbilling.gst_billing.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    public ProductService(ProductRepository productRepository, CurrentUserService currentUserService) {
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
    }

    public Product createProduct(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required.");
        }
        product.setName(product.getName().trim());

        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product price must be non-negative.");
        }

        if (product.getGstRate() != null && !GstRateConfig.isValidRate(product.getGstRate())) {
            // Soft-clamp or validate to nearest standard rate
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GST Rate. Standard rates are 0%, 5%, 12%, 18%, 28%.");
        }

        if (product.getProductType() == null || product.getProductType().isBlank()) {
            product.setProductType("PRODUCT");
        } else {
            product.setProductType(product.getProductType().trim().toUpperCase());
        }

        if (product.getTaxInclusive() == null) {
            product.setTaxInclusive(false);
        }

        product.setBusiness(currentUserService.getCurrentUser().getBusiness());
        return productRepository.save(product);
    }

    public List<Product> getAllProducts(String search, String type, Boolean lowStockOnly) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();

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

    public Product getProductById(Long id) {
        return productRepository.findByIdAndBusinessId(id, currentUserService.getCurrentUser().getBusiness().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or not accessible"));
    }

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

        if (changes.getStockQuantity() != null) {
            product.setStockQuantity(changes.getStockQuantity());
        }
        if (changes.getLowStockThreshold() != null) {
            product.setLowStockThreshold(changes.getLowStockThreshold());
        }
        if (changes.getProductType() != null) {
            product.setProductType(changes.getProductType().trim().toUpperCase());
        }
        product.setActive(changes.isActive());

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        try {
            productRepository.delete(product);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete product because it is referenced in existing invoices or purchase records.");
        }
    }
}
