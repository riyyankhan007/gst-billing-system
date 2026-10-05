package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.einvoice.EInvoiceCancelRequest;
import com.gstbilling.gst_billing.dto.einvoice.EInvoiceResult;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.integration.einvoice.EInvoiceProvider;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class EInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final EInvoiceProvider eInvoiceProvider;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public EInvoiceService(
            InvoiceRepository invoiceRepository,
            EInvoiceProvider eInvoiceProvider,
            CurrentUserService currentUserService,
            AuditLogService auditLogService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.eInvoiceProvider = eInvoiceProvider;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public EInvoiceResult generateEInvoice(Long invoiceId) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        Invoice invoice = invoiceRepository.findByIdAndBusiness_Id(invoiceId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if ("DRAFT".equalsIgnoreCase(invoice.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-Invoice can only be generated for issued invoices. Please issue the invoice first.");
        }

        if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot generate e-invoice for a cancelled invoice");
        }

        // Idempotent: return existing if already generated
        if ("GENERATED".equalsIgnoreCase(invoice.getEinvoiceStatus()) && invoice.getIrn() != null) {
            return EInvoiceResult.success(
                    invoice.getIrn(),
                    invoice.getAckNo(),
                    invoice.getAckDate(),
                    null,
                    invoice.getSignedQrCode()
            );
        }

        EInvoiceResult result = eInvoiceProvider.generateIrn(invoice);

        if (!result.success()) {
            invoice.setEinvoiceStatus("FAILED");
            invoice.setEinvoiceErrorMessage(result.errorMessage());
            invoice.setUpdatedAt(LocalDateTime.now());
            invoiceRepository.save(invoice);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "E-Invoice generation failed: " + result.errorMessage());
        }

        invoice.setIrn(result.irn());
        invoice.setAckNo(result.ackNo());
        invoice.setAckDate(result.ackDate());
        invoice.setSignedQrCode(result.signedQrCode());
        invoice.setEinvoiceStatus("GENERATED");
        invoice.setEinvoiceErrorMessage(null);
        invoice.setUpdatedAt(LocalDateTime.now());

        invoiceRepository.save(invoice);
        auditLogService.logAction("GENERATE_EINVOICE", "INVOICE", invoice.getId(),
                "E-Invoice IRN generated: " + result.irn() + ", AckNo: " + result.ackNo());

        return result;
    }

    @Transactional
    public EInvoiceResult cancelEInvoice(Long invoiceId, EInvoiceCancelRequest request) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        Invoice invoice = invoiceRepository.findByIdAndBusiness_Id(invoiceId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (!"GENERATED".equalsIgnoreCase(invoice.getEinvoiceStatus()) || invoice.getIrn() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice does not have an active generated E-Invoice");
        }

        int code = (request != null) ? request.reasonCode() : 1;
        String remarks = (request != null && request.remarks() != null) ? request.remarks() : "Cancelled by user";

        boolean cancelled = eInvoiceProvider.cancelIrn(invoice.getIrn(), code, remarks);
        if (!cancelled) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to cancel E-Invoice on IRP portal");
        }

        invoice.setEinvoiceStatus("CANCELLED");
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        auditLogService.logAction("CANCEL_EINVOICE", "INVOICE", invoice.getId(),
                "E-Invoice IRN cancelled: " + invoice.getIrn() + ". Remarks: " + remarks);

        return EInvoiceResult.cancelled(invoice.getIrn());
    }

    @Transactional(readOnly = true)
    public EInvoiceResult getEInvoice(Long invoiceId) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        Invoice invoice = invoiceRepository.findByIdAndBusiness_Id(invoiceId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getIrn() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "E-Invoice not generated for this invoice");
        }

        return new EInvoiceResult(
                "GENERATED".equalsIgnoreCase(invoice.getEinvoiceStatus()),
                invoice.getIrn(),
                invoice.getAckNo(),
                invoice.getAckDate(),
                null,
                invoice.getSignedQrCode(),
                invoice.getEinvoiceStatus(),
                invoice.getEinvoiceErrorMessage()
        );
    }
}
