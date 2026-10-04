package com.gstbilling.gst_billing.dto;

public record UserProfileResponse(
    Long id,
    String name,
    String email,
    String role,
    Long businessId,
    String businessName,
    String businessGstin
) {}
