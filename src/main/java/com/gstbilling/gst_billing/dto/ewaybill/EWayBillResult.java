package com.gstbilling.gst_billing.dto.ewaybill;

import java.time.LocalDateTime;

public record EWayBillResult(
        boolean success,
        String ewbNumber,
        LocalDateTime ewbDate,
        LocalDateTime validUpto,
        String status,
        String errorMessage
) {
    public static EWayBillResult success(String ewbNumber, LocalDateTime ewbDate, LocalDateTime validUpto) {
        return new EWayBillResult(true, ewbNumber, ewbDate, validUpto, "ACTIVE", null);
    }

    public static EWayBillResult failure(String errorMessage) {
        return new EWayBillResult(false, null, null, null, "FAILED", errorMessage);
    }

    public static EWayBillResult cancelled(String ewbNumber) {
        return new EWayBillResult(true, ewbNumber, LocalDateTime.now(), null, "CANCELLED", null);
    }
}
