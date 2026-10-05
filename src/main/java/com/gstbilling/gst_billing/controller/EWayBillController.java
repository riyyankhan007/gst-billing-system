package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.ewaybill.EWayBillCancelRequest;
import com.gstbilling.gst_billing.dto.ewaybill.EWayBillGenerateRequest;
import com.gstbilling.gst_billing.entity.EWayBill;
import com.gstbilling.gst_billing.service.EWayBillService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices/{id}/ewaybill")
public class EWayBillController {

    private final EWayBillService eWayBillService;

    public EWayBillController(EWayBillService eWayBillService) {
        this.eWayBillService = eWayBillService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public EWayBill generateEWayBill(
            @PathVariable Long id,
            @RequestBody(required = false) EWayBillGenerateRequest request
    ) {
        if (request == null) {
            request = new EWayBillGenerateRequest(100, null, "REGULAR", "ROAD", null, null);
        }
        return eWayBillService.generateEWayBill(id, request);
    }

    @PostMapping("/{ewbId}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public EWayBill cancelEWayBill(
            @PathVariable Long id,
            @PathVariable Long ewbId,
            @RequestBody(required = false) EWayBillCancelRequest request
    ) {
        return eWayBillService.cancelEWayBill(id, ewbId, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public List<EWayBill> getEWayBills(@PathVariable Long id) {
        return eWayBillService.getEWayBillsForInvoice(id);
    }
}
