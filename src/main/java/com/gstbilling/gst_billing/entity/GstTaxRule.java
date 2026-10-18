package com.gstbilling.gst_billing.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gst_tax_rule")
public class GstTaxRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hsn_sac_code_id", nullable = false)
    @JsonBackReference
    private HsnSacCode hsnSacCode;

    @Column(name = "igst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal igstRate;

    @Column(name = "cgst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal cgstRate;

    @Column(name = "sgst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal sgstRate;

    @Column(name = "cess_rate", precision = 5, scale = 2)
    private BigDecimal cessRate = BigDecimal.ZERO;

    @Column(name = "condition_description", columnDefinition = "TEXT")
    private String conditionDescription;

    @Column(name = "notification_number", length = 100)
    private String notificationNumber;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom = LocalDate.of(2017, 7, 1);

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_conditional", nullable = false)
    private Boolean isConditional = false;

    @Column(name = "requires_review", nullable = false)
    private Boolean requiresReview = false;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public GstTaxRule() {}

    public GstTaxRule(HsnSacCode hsnSacCode, BigDecimal igstRate, BigDecimal cgstRate, BigDecimal sgstRate, String notificationNumber) {
        this.hsnSacCode = hsnSacCode;
        this.igstRate = igstRate;
        this.cgstRate = cgstRate;
        this.sgstRate = sgstRate;
        this.notificationNumber = notificationNumber;
        this.effectiveFrom = LocalDate.of(2017, 7, 1);
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (isConditional == null) {
            isConditional = false;
        }
        if (requiresReview == null) {
            requiresReview = false;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public HsnSacCode getHsnSacCode() {
        return hsnSacCode;
    }

    public void setHsnSacCode(HsnSacCode hsnSacCode) {
        this.hsnSacCode = hsnSacCode;
    }

    public BigDecimal getIgstRate() {
        return igstRate;
    }

    public void setIgstRate(BigDecimal igstRate) {
        this.igstRate = igstRate;
    }

    public BigDecimal getCgstRate() {
        return cgstRate;
    }

    public void setCgstRate(BigDecimal cgstRate) {
        this.cgstRate = cgstRate;
    }

    public BigDecimal getSgstRate() {
        return sgstRate;
    }

    public void setSgstRate(BigDecimal sgstRate) {
        this.sgstRate = sgstRate;
    }

    public BigDecimal getCessRate() {
        return cessRate;
    }

    public void setCessRate(BigDecimal cessRate) {
        this.cessRate = cessRate;
    }

    public String getConditionDescription() {
        return conditionDescription;
    }

    public void setConditionDescription(String conditionDescription) {
        this.conditionDescription = conditionDescription;
    }

    public String getNotificationNumber() {
        return notificationNumber;
    }

    public void setNotificationNumber(String notificationNumber) {
        this.notificationNumber = notificationNumber;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Boolean getIsConditional() {
        return isConditional;
    }

    public void setIsConditional(Boolean conditional) {
        isConditional = conditional;
    }

    public Boolean getRequiresReview() {
        return requiresReview;
    }

    public void setRequiresReview(Boolean requiresReview) {
        this.requiresReview = requiresReview;
    }

    public Boolean getActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
