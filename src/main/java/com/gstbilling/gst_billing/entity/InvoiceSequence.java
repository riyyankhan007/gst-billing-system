package com.gstbilling.gst_billing.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "invoice_sequence",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_business_fy_doc_type",
                columnNames = {"business_id", "financial_year", "doc_type"}
        )
)
public class InvoiceSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "financial_year", nullable = false, length = 10)
    private String financialYear;

    @Column(name = "doc_type", nullable = false, length = 30)
    private String docType = "INVOICE";

    @Column(name = "prefix", length = 20)
    private String prefix = "INV";

    @Column(name = "current_sequence", nullable = false)
    private Long currentSequence = 0L;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public InvoiceSequence() {}

    public InvoiceSequence(Business business, String financialYear, String docType, String prefix, Long currentSequence) {
        this.business = business;
        this.financialYear = financialYear;
        this.docType = docType;
        this.prefix = prefix;
        this.currentSequence = currentSequence != null ? currentSequence : 0L;
        this.updatedAt = LocalDateTime.now();
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

    public String getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Long getCurrentSequence() {
        return currentSequence;
    }

    public void setCurrentSequence(Long currentSequence) {
        this.currentSequence = currentSequence;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
