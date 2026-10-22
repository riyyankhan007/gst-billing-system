package com.gstbilling.gst_billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_stock", uniqueConstraints = {
        @UniqueConstraint(name = "uk_warehouse_product", columnNames = {"warehouse_id", "product_id"})
}, indexes = {
        @Index(name = "idx_wh_stock_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_wh_stock_product", columnList = "product_id"),
        @Index(name = "idx_wh_stock_business", columnList = "business_id")
})
public class WarehouseStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    @JsonIgnore
    private Business business;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_on_hand", nullable = false)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "quantity_reserved", nullable = false)
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WarehouseStock() {
    }

    public WarehouseStock(Business business, Warehouse warehouse, Product product, BigDecimal quantityOnHand) {
        this.business = business;
        this.warehouse = warehouse;
        this.product = product;
        this.quantityOnHand = quantityOnHand != null ? quantityOnHand : BigDecimal.ZERO;
        this.quantityReserved = BigDecimal.ZERO;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand != null ? quantityOnHand : BigDecimal.ZERO;
    }

    public void setQuantityOnHand(BigDecimal quantityOnHand) {
        this.quantityOnHand = quantityOnHand != null ? quantityOnHand : BigDecimal.ZERO;
    }

    public BigDecimal getQuantityReserved() {
        return quantityReserved != null ? quantityReserved : BigDecimal.ZERO;
    }

    public void setQuantityReserved(BigDecimal quantityReserved) {
        this.quantityReserved = quantityReserved != null ? quantityReserved : BigDecimal.ZERO;
    }

    public BigDecimal getAvailableQuantity() {
        BigDecimal onHand = getQuantityOnHand();
        BigDecimal reserved = getQuantityReserved();
        return onHand.subtract(reserved);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
