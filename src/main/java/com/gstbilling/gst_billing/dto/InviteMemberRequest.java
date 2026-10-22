package com.gstbilling.gst_billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteMemberRequest(
        @NotBlank(message = "Email is required") @Email(message = "Valid email is required") String email,
        @NotBlank(message = "Role is required") String role
) {}
