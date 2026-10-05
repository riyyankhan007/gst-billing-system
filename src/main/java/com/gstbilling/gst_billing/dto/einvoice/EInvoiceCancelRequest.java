package com.gstbilling.gst_billing.dto.einvoice;

public record EInvoiceCancelRequest(
        int reasonCode,
        String remarks
) {
    public EInvoiceCancelRequest {
        if (reasonCode < 1 || reasonCode > 4) {
            reasonCode = 1; // Default: 1 = Duplicate, 2 = Data entry mistake, 3 = Order cancelled, 4 = Others
        }
        if (remarks == null || remarks.isBlank()) {
            remarks = "Cancelled by user";
        }
    }
}
