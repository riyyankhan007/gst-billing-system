package com.gstbilling.gst_billing.dto.ewaybill;

public record EWayBillCancelRequest(
        int reasonCode,
        String remarks
) {
    public EWayBillCancelRequest {
        if (reasonCode < 1 || reasonCode > 4) {
            reasonCode = 1; // 1 = Duplicate, 2 = Order Cancelled, 3 = Data Entry Error, 4 = Others
        }
        if (remarks == null || remarks.isBlank()) {
            remarks = "Cancelled by user";
        }
    }
}
