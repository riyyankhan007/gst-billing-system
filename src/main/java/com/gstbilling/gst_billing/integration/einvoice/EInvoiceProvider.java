package com.gstbilling.gst_billing.integration.einvoice;

import com.gstbilling.gst_billing.dto.einvoice.EInvoiceResult;
import com.gstbilling.gst_billing.entity.Invoice;

/**
 * Provider-agnostic interface for Indian E-Invoice generation (IRP / NIC).
 */
public interface EInvoiceProvider {

    default String getProviderName() {
        return getClass().getSimpleName();
    }

    /**
     * Generates Invoice Reference Number (IRN) and signed QR code for an issued invoice.
     */
    EInvoiceResult generateIrn(Invoice invoice);

    /**
     * Cancels an existing e-invoice with the IRP portal.
     *
     * @param irn 64-character Invoice Reference Number
     * @param reasonCode 1: Duplicate, 2: Data entry mistake, 3: Order cancelled, 4: Others
     * @param remarks Cancellation remarks
     * @return true if cancelled successfully
     */
    boolean cancelIrn(String irn, int reasonCode, String remarks);

    /**
     * Fetches details of an existing e-invoice from the IRP portal.
     */
    EInvoiceResult getEInvoiceDetails(String irn);
}
