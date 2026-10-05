package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.repository.PaymentRepository;
import com.gstbilling.gst_billing.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentReconciliationService {

    public record ReconciliationItem(
            Long invoiceId,
            String invoiceNumber,
            BigDecimal grandTotal,
            BigDecimal recordedPaid,
            BigDecimal calculatedPaid,
            BigDecimal calculatedBalance,
            String previousStatus,
            String newStatus,
            boolean corrected
    ) {}

    public record ReconciliationSummary(
            int totalInvoices,
            int reconciledCount,
            int discrepancyCount,
            List<ReconciliationItem> items
    ) {}

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final StorageService storageService;

    public PaymentReconciliationService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            StorageService storageService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.storageService = storageService;
    }

    @Transactional
    public ReconciliationItem reconcileInvoice(Long invoiceId) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();

        Invoice invoice = invoiceRepository.findByIdAndBusiness_Id(invoiceId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found with ID: " + invoiceId));

        return doReconcile(invoice, businessId);
    }

    @Transactional
    public ReconciliationSummary reconcileAllInvoices() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        List<Invoice> invoices = invoiceRepository.findByBusiness_IdOrderByInvoiceDateDescIdDesc(businessId);

        List<ReconciliationItem> items = new ArrayList<>();
        int discrepancyCount = 0;

        for (Invoice invoice : invoices) {
            ReconciliationItem item = doReconcile(invoice, businessId);
            items.add(item);
            if (item.corrected()) {
                discrepancyCount++;
            }
        }

        auditLogService.logAction("RECONCILE_PAYMENTS", "BUSINESS", businessId,
                "Payment reconciliation completed. Total: " + invoices.size() + ", Discrepancies fixed: " + discrepancyCount);

        return new ReconciliationSummary(invoices.size(), items.size(), discrepancyCount, items);
    }

    private ReconciliationItem doReconcile(Invoice invoice, Long businessId) {
        if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            return new ReconciliationItem(
                    invoice.getId(),
                    invoice.getInvoiceNumber(),
                    invoice.getGrandTotal(),
                    invoice.getPaidAmount(),
                    BigDecimal.ZERO,
                    invoice.getGrandTotal(),
                    invoice.getStatus(),
                    invoice.getStatus(),
                    false
            );
        }

        BigDecimal recordedPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal grandTotal = invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO;
        String prevStatus = invoice.getStatus();

        // Sum up all successful payments
        BigDecimal calculatedPaid = paymentRepository.sumAmountByInvoiceIdAndBusinessId(invoice.getId(), businessId);
        if (calculatedPaid == null) {
            calculatedPaid = BigDecimal.ZERO;
        }
        calculatedPaid = calculatedPaid.setScale(2, RoundingMode.HALF_UP);

        BigDecimal calculatedBalance = grandTotal.subtract(calculatedPaid).setScale(2, RoundingMode.HALF_UP);
        if (calculatedBalance.compareTo(BigDecimal.ZERO) < 0) {
            calculatedBalance = BigDecimal.ZERO;
        }

        String targetStatus = prevStatus;
        if (calculatedBalance.compareTo(BigDecimal.ZERO) <= 0 && grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            targetStatus = "PAID";
        } else if (calculatedPaid.compareTo(BigDecimal.ZERO) > 0) {
            targetStatus = "PARTIALLY_PAID";
        } else if ("PAID".equalsIgnoreCase(prevStatus) || "PARTIALLY_PAID".equalsIgnoreCase(prevStatus)) {
            targetStatus = "ISSUED";
        }

        boolean paidDiffers = recordedPaid.compareTo(calculatedPaid) != 0;
        BigDecimal curBalance = invoice.getBalanceAmount() != null ? invoice.getBalanceAmount() : BigDecimal.ZERO;
        boolean balanceDiffers = curBalance.compareTo(calculatedBalance) != 0;
        boolean statusDiffers = !targetStatus.equalsIgnoreCase(prevStatus);

        boolean corrected = paidDiffers || balanceDiffers || statusDiffers;

        if (corrected) {
            invoice.setPaidAmount(calculatedPaid);
            invoice.setBalanceAmount(calculatedBalance);
            invoice.setStatus(targetStatus);
            invoiceRepository.save(invoice);
            storageService.delete("invoices/" + businessId + "/" + invoice.getId() + ".pdf");

            auditLogService.logAction("RECONCILE_INVOICE", "INVOICE", invoice.getId(),
                    "Reconciled invoice " + invoice.getInvoiceNumber() + ". Paid: " + calculatedPaid + ", Balance: " + calculatedBalance + ", Status: " + targetStatus);
        }

        return new ReconciliationItem(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                grandTotal,
                recordedPaid,
                calculatedPaid,
                calculatedBalance,
                prevStatus,
                targetStatus,
                corrected
        );
    }
}
