package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.einvoice.EInvoiceCancelRequest;
import com.gstbilling.gst_billing.dto.einvoice.EInvoiceResult;
import com.gstbilling.gst_billing.service.EInvoiceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices/{id}/einvoice")
public class EInvoiceController {

    private final EInvoiceService eInvoiceService;

    public EInvoiceController(EInvoiceService eInvoiceService) {
        this.eInvoiceService = eInvoiceService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public EInvoiceResult generateEInvoice(@PathVariable Long id) {
        return eInvoiceService.generateEInvoice(id);
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public EInvoiceResult cancelEInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) EInvoiceCancelRequest request
    ) {
        return eInvoiceService.cancelEInvoice(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public EInvoiceResult getEInvoice(@PathVariable Long id) {
        return eInvoiceService.getEInvoice(id);
    }
}
