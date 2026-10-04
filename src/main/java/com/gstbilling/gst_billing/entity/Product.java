package com.gstbilling.gst_billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String sku;

    @Column(name = "hsn_code")
    private String hsnCode;

    private String unit = "PCS";

    @Column(nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "gst_rate", nullable = false)
    private BigDecimal gstRate = BigDecimal.valueOf(18);

    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "tax_inclusive")
    private boolean taxInclusive = false;

    @Column(name = "stock_quantity")
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Column(name = "low_stock_threshold")
    private BigDecimal lowStockThreshold = BigDecimal.valueOf(5);

    @Column(name = "product_type")
    private String productType = "PRODUCT"; // PRODUCT or SERVICE

    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    @JsonIgnore
    private Business business;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getHsnCode() {
        return hsnCode;
    }

    public void setHsnCode(String hsnCode) {
        this.hsnCode = hsnCode;
    }

    public String getUnit() {
        return unit != null ? unit : "PCS";
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getPrice() {
        return price != null ? price : BigDecimal.ZERO;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getGstRate() {
        return gstRate != null ? gstRate : BigDecimal.ZERO;
    }

    public void setGstRate(BigDecimal gstRate) {
        this.gstRate = gstRate;
    }

    public BigDecimal getDiscount() {
        return discount != null ? discount : BigDecimal.ZERO;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public boolean isTaxInclusive() {
        return taxInclusive;
    }

    public void setTaxInclusive(boolean taxInclusive) {
        this.taxInclusive = taxInclusive;
    }

    public BigDecimal getStockQuantity() {
        return stockQuantity != null ? stockQuantity : BigDecimal.ZERO;
    }

    public void setStockQuantity(BigDecimal stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public BigDecimal getLowStockThreshold() {
        return lowStockThreshold != null ? lowStockThreshold : BigDecimal.valueOf(5);
    }

    public void setLowStockThreshold(BigDecimal lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getProductType() {
        return productType != null ? productType : "PRODUCT";
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public Boolean isActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }
}
