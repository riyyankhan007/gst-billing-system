package com.gstbilling.gst_billing.dto.ewaybill;

public record EWayBillGenerateRequest(
        Integer distanceKm,
        String vehicleNumber,
        String vehicleType,
        String transportMode,
        String transporterId,
        String transporterName
) {
    public EWayBillGenerateRequest {
        if (distanceKm == null || distanceKm <= 0) {
            distanceKm = 100; // Default distance
        }
        if (vehicleType == null || vehicleType.isBlank()) {
            vehicleType = "REGULAR";
        }
        if (transportMode == null || transportMode.isBlank()) {
            transportMode = "ROAD";
        }
    }
}
