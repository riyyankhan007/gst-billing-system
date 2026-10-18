package com.gstbilling.gst_billing.entity;

public enum HsnSacType {
    HSN,
    SAC;

    public static HsnSacType fromString(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        try {
            return HsnSacType.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
