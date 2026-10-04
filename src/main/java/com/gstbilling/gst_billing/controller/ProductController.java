package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.config.GstRateConfig;
import com.gstbilling.gst_billing.entity.Product;
import com.gstbilling.gst_billing.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @GetMapping
    public List<Product> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean lowStock
    ) {
        return productService.getAllProducts(search, type, lowStock);
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }

    @GetMapping("/config")
    public Map<String, Object> getGstConfig() {
        return Map.of(
                "standardGstRates", GstRateConfig.STANDARD_GST_RATES,
                "standardUnits", GstRateConfig.STANDARD_UNITS
        );
    }
}
