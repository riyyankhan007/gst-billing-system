package com.gstbilling.gst_billing.dto;

public record ReminderResponse(
    String customerName,
    String customerPhone,
    String customerEmail,
    String invoiceNumber,
    String amount,
    String messageText,
    String whatsappUrl,
    boolean emailSent,
    String status
) {}
