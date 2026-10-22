package com.gstbilling.gst_billing.dto;

public record SwitchBusinessResponse(
        String token,
        Long businessId,
        String businessName,
        String gstin,
        String role,
        String userName,
        String userEmail
) {}
