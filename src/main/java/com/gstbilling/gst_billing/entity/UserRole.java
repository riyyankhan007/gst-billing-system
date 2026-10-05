package com.gstbilling.gst_billing.entity;

public enum UserRole {
    OWNER,
    ADMIN,
    ACCOUNTANT,
    SALES,
    VIEWER,
    SUPPORT;

    public static UserRole fromString(String role) {
        if (role == null || role.isBlank()) {
            return VIEWER;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return VIEWER;
        }
    }
}
