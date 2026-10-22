package com.gstbilling.gst_billing.dto;

import java.time.LocalDateTime;

public record BusinessMembershipDto(
        Long membershipId,
        Long businessId,
        String businessName,
        String gstin,
        String role,
        String status,
        LocalDateTime createdAt
) {}
