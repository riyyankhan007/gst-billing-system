package com.gstbilling.gst_billing.dto.ewaybill;

public record VehicleUpdateRequest(
        String vehicleNumber,
        String fromPlace,
        String fromState,
        int reasonCode,
        String remarks
) {}
