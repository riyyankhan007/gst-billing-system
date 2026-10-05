package com.gstbilling.gst_billing.entity;

public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    SENT,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED;

    public boolean isFinalized() {
        return this != DRAFT;
    }

    public boolean canBeModified() {
        return this == DRAFT;
    }

    public boolean canBeDeleted() {
        return this == DRAFT;
    }

    public boolean canBeCancelled() {
        return this == ISSUED || this == SENT || this == PARTIALLY_PAID || this == OVERDUE;
    }
}
