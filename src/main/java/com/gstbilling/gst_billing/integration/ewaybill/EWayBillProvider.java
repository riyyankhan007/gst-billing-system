package com.gstbilling.gst_billing.integration.ewaybill;

import com.gstbilling.gst_billing.dto.ewaybill.EWayBillGenerateRequest;
import com.gstbilling.gst_billing.dto.ewaybill.EWayBillResult;
import com.gstbilling.gst_billing.dto.ewaybill.VehicleUpdateRequest;
import com.gstbilling.gst_billing.entity.Invoice;

/**
 * Provider-agnostic interface for Indian E-Way Bill operations.
 */
public interface EWayBillProvider {

    default String getProviderName() {
        return getClass().getSimpleName();
    }

    /**
     * Generates a 12-digit E-Way Bill for an invoice.
     */
    EWayBillResult generateEWayBill(Invoice invoice, EWayBillGenerateRequest request);

    /**
     * Cancels an existing E-Way Bill within 24 hours of generation.
     */
    boolean cancelEWayBill(String ewbNo, int reasonCode, String remarks);

    /**
     * Updates vehicle number / transport details on an active E-Way Bill.
     */
    boolean updateVehicleNumber(String ewbNo, VehicleUpdateRequest request);
}
