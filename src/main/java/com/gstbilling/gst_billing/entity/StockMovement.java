package com.gstbilling.gst_billing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movement", indexes = {
        @Index(name = "idx_stock_mov_business", columnList = "business_id"),
        @Index(name = "idx_stock_mov_product", columnList = "product_id")
})
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "movement_type", nullable = false)
    private String movementType; // "INVOICE", "PURCHASE", "MANUAL_ADJUSTMENT", "CREDIT_NOTE"

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "stock_before")
    private BigDecimal stockBefore;

    @Column(name = "stock_after")
    private BigDecimal stockAfter;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public StockMovement() {
        this.createdAt = LocalDateTime.now();
    }

    public StockMovement(Business business, Product product, String movementType, BigDecimal quantity,
                         BigDecimal stockBefore, BigDecimal stockAfter, String referenceNumber, String notes) {
        this.business = business;
        this.product = product;
        this.movementType = movementType;
        this.quantity = quantity;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.referenceNumber = referenceNumber;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getStockBefore() {
        return stockBefore;
    }

    public void setStockBefore(BigDecimal stockBefore) {
        this.stockBefore = stockBefore;
    }

    public BigDecimal getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(BigDecimal stockAfter) {
        this.stockAfter = stockAfter;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
