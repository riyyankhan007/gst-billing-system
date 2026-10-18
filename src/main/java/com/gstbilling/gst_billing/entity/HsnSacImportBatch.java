package com.gstbilling.gst_billing.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hsn_sac_import_batch")
public class HsnSacImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true, length = 64)
    private String batchId;

    @Column(nullable = false, length = 150)
    private String source;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "imported_by", nullable = false, length = 150)
    private String importedBy;

    @Column(name = "total_records", nullable = false)
    private int totalRecords = 0;

    @Column(name = "inserted_records", nullable = false)
    private int insertedRecords = 0;

    @Column(name = "updated_records", nullable = false)
    private int updatedRecords = 0;

    @Column(name = "skipped_records", nullable = false)
    private int skippedRecords = 0;

    @Column(name = "rejected_records", nullable = false)
    private int rejectedRecords = 0;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(nullable = false, length = 30)
    private String status = "COMPLETED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public HsnSacImportBatch() {}

    public HsnSacImportBatch(String batchId, String source, String version, String importedBy) {
        this.batchId = batchId;
        this.source = source;
        this.version = version;
        this.importedBy = importedBy;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getImportedBy() {
        return importedBy;
    }

    public void setImportedBy(String importedBy) {
        this.importedBy = importedBy;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getInsertedRecords() {
        return insertedRecords;
    }

    public void setInsertedRecords(int insertedRecords) {
        this.insertedRecords = insertedRecords;
    }

    public int getUpdatedRecords() {
        return updatedRecords;
    }

    public void setUpdatedRecords(int updatedRecords) {
        this.updatedRecords = updatedRecords;
    }

    public int getSkippedRecords() {
        return skippedRecords;
    }

    public void setSkippedRecords(int skippedRecords) {
        this.skippedRecords = skippedRecords;
    }

    public int getRejectedRecords() {
        return rejectedRecords;
    }

    public void setRejectedRecords(int rejectedRecords) {
        this.rejectedRecords = rejectedRecords;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
