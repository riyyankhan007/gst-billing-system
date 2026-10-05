package com.gstbilling.gst_billing.dto.einvoice;

import java.time.LocalDateTime;

public record EInvoiceResult(
        boolean success,
        String irn,
        String ackNo,
        LocalDateTime ackDate,
        String signedInvoice,
        String signedQrCode,
        String status,
        String errorMessage
) {
    public static EInvoiceResult success(String irn, String ackNo, LocalDateTime ackDate, String signedInvoice, String signedQrCode) {
        return new EInvoiceResult(true, irn, ackNo, ackDate, signedInvoice, signedQrCode, "GENERATED", null);
    }

    public static EInvoiceResult failure(String errorMessage) {
        return new EInvoiceResult(false, null, null, null, null, null, "FAILED", errorMessage);
    }

    public static EInvoiceResult cancelled(String irn) {
        return new EInvoiceResult(true, irn, null, LocalDateTime.now(), null, null, "CANCELLED", null);
    }
}
