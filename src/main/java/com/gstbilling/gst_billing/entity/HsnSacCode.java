package com.gstbilling.gst_billing.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hsn_sac_code", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hsn_sac_code_type", columnNames = {"code", "type"})
})
public class HsnSacCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HsnSacType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "classification_level", length = 20)
    private String classificationLevel;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom = LocalDate.of(2017, 7, 1);

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, length = 100)
    private String source = "CBIC_GST_TARIFF";

    @Column(name = "verification_date", nullable = false)
    private LocalDate verificationDate = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "hsnSacCode", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<GstTaxRule> taxRules = new ArrayList<>();

    public HsnSacCode() {}

    public HsnSacCode(String code, HsnSacType type, String description, String classificationLevel, BigDecimal gstRate, String source) {
        this.code = code;
        this.type = type;
        this.description = description;
        this.classificationLevel = classificationLevel;
        this.gstRate = gstRate;
        this.source = source != null ? source : "CBIC_GST_TARIFF";
        this.effectiveFrom = LocalDate.of(2017, 7, 1);
        this.verificationDate = LocalDate.now();
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
        if (verificationDate == null) {
            verificationDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public HsnSacType getType() {
        return type;
    }

    public void setType(HsnSacType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClassificationLevel() {
        return classificationLevel;
    }

    public void setClassificationLevel(String classificationLevel) {
        this.classificationLevel = classificationLevel;
    }

    public BigDecimal getGstRate() {
        return gstRate;
    }

    public void setGstRate(BigDecimal gstRate) {
        this.gstRate = gstRate;
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

    public Boolean getActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDate getVerificationDate() {
        return verificationDate;
    }

    public void setVerificationDate(LocalDate verificationDate) {
        this.verificationDate = verificationDate;
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

    public List<GstTaxRule> getTaxRules() {
        return taxRules;
    }

    public void setTaxRules(List<GstTaxRule> taxRules) {
        this.taxRules = taxRules;
    }
}
