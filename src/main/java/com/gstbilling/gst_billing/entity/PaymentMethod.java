package com.gstbilling.gst_billing.entity;

public enum PaymentMethod {
    CASH,
    UPI,
    NEFT,
    RTGS,
    IMPS,
    CARD,
    CHEQUE,
    NETBANKING,
    OTHER;

    public static PaymentMethod fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OTHER;
        }
        String clean = value.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        if (clean.contains("BANK") || clean.contains("TRANSFER")) {
            return NEFT;
        }
        for (PaymentMethod pm : values()) {
            if (pm.name().equalsIgnoreCase(clean)) {
                return pm;
            }
        }
        return OTHER;
    }
}
