package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Product;
import com.gstbilling.gst_billing.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    public ProductService(ProductRepository productRepository, CurrentUserService currentUserService) {
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
    }

    public Product createProduct(Product product) {
        product.setBusiness(currentUserService.getCurrentUser().getBusiness());
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findByBusinessId(currentUserService.getCurrentUser().getBusiness().getId());
    }

    public Product getProductById(Long id) {
        return productRepository.findByIdAndBusinessId(id, currentUserService.getCurrentUser().getBusiness().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Product is not available to this business"));
    }
    public Product updateProduct(Long id, Product changes) {
        Product product = getProductById(id); product.setName(changes.getName()); product.setHsnCode(changes.getHsnCode()); product.setPrice(changes.getPrice()); product.setGstRate(changes.getGstRate()); return productRepository.save(product);
    }
}
