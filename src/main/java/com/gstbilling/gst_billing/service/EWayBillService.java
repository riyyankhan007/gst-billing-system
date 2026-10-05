package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.ewaybill.EWayBillCancelRequest;
import com.gstbilling.gst_billing.dto.ewaybill.EWayBillGenerateRequest;
import com.gstbilling.gst_billing.dto.ewaybill.EWayBillResult;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.EWayBill;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.integration.ewaybill.EWayBillProvider;
import com.gstbilling.gst_billing.repository.EWayBillRepository;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EWayBillService {

    private final EWayBillRepository eWayBillRepository;
    private final InvoiceRepository invoiceRepository;
    private final EWayBillProvider eWayBillProvider;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public EWayBillService(
            EWayBillRepository eWayBillRepository,
            InvoiceRepository invoiceRepository,
            EWayBillProvider eWayBillProvider,
            CurrentUserService currentUserService,
            AuditLogService auditLogService
    ) {
        this.eWayBillRepository = eWayBillRepository;
        this.invoiceRepository = invoiceRepository;
        this.eWayBillProvider = eWayBillProvider;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public EWayBill generateEWayBill(Long invoiceId, EWayBillGenerateRequest request) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        Invoice invoice = invoiceRepository.findByIdAndBusiness_Id(invoiceId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if ("DRAFT".equalsIgnoreCase(invoice.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-Way Bill can only be generated for issued invoices.");
        }

        if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot generate E-Way Bill for a cancelled invoice.");
        }

        EWayBillResult result = eWayBillProvider.generateEWayBill(invoice, request);
        if (!result.success()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to generate E-Way Bill: " + result.errorMessage());
        }

        Business business = invoice.getBusiness();
        EWayBill ewb = new EWayBill();
        ewb.setBusiness(business);
        ewb.setInvoice(invoice);
        ewb.setEwbNumber(result.ewbNumber());
        ewb.setEwbDate(result.ewbDate());
        ewb.setValidUpto(result.validUpto());
        ewb.setDistanceKm(request.distanceKm());
        ewb.setVehicleNumber(request.vehicleNumber());
        ewb.setVehicleType(request.vehicleType());
        ewb.setTransportMode(request.transportMode());
        ewb.setTransporterId(request.transporterId());
        ewb.setTransporterName(request.transporterName());
        ewb.setStatus("ACTIVE");
        ewb.setCreatedAt(LocalDateTime.now());
        ewb.setUpdatedAt(LocalDateTime.now());

        EWayBill saved = eWayBillRepository.save(ewb);
        auditLogService.logAction("GENERATE_EWAY_BILL", "INVOICE", invoice.getId(),
                "E-Way Bill generated #" + saved.getEwbNumber() + " for invoice #" + invoice.getInvoiceNumber());

        return saved;
    }

    @Transactional
    public EWayBill cancelEWayBill(Long invoiceId, Long ewbId, EWayBillCancelRequest request) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        EWayBill ewb = eWayBillRepository.findByIdAndBusiness_Id(ewbId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "E-Way Bill not found"));

        if (!ewb.getInvoice().getId().equals(invoiceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-Way Bill does not belong to this invoice");
        }

        if ("CANCELLED".equalsIgnoreCase(ewb.getStatus())) {
            return ewb;
        }

        int reasonCode = (request != null) ? request.reasonCode() : 1;
        String remarks = (request != null && request.remarks() != null) ? request.remarks() : "Cancelled by user";

        boolean cancelled = eWayBillProvider.cancelEWayBill(ewb.getEwbNumber(), reasonCode, remarks);
        if (!cancelled) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to cancel E-Way Bill with portal");
        }

        ewb.setStatus("CANCELLED");
        ewb.setCancelReason(remarks);
        ewb.setCancelledAt(LocalDateTime.now());
        ewb.setUpdatedAt(LocalDateTime.now());

        EWayBill saved = eWayBillRepository.save(ewb);
        auditLogService.logAction("CANCEL_EWAY_BILL", "INVOICE", invoiceId,
                "E-Way Bill #" + saved.getEwbNumber() + " cancelled. Reason: " + remarks);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<EWayBill> getEWayBillsForInvoice(Long invoiceId) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return eWayBillRepository.findByInvoice_IdAndBusiness_IdOrderByCreatedAtDesc(invoiceId, businessId);
    }
}
