package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.PaymentRequest;
import com.gstbilling.gst_billing.dto.PaymentResponse;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.Payment;
import com.gstbilling.gst_billing.entity.PaymentMethod;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.repository.PaymentRepository;
import com.gstbilling.gst_billing.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final InvoiceSequenceService invoiceSequenceService;
    private final StorageService storageService;

    public PaymentService(PaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          CurrentUserService currentUserService,
                          AuditLogService auditLogService,
                          InvoiceSequenceService invoiceSequenceService,
                          StorageService storageService) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.invoiceSequenceService = invoiceSequenceService;
        this.storageService = storageService;
    }

    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();

        Invoice invoice = invoiceRepository.findByIdAndBusiness_Id(request.invoiceId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found with ID: " + request.invoiceId()));

        if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot record payment against a cancelled invoice");
        }

        BigDecimal currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal grandTotal = invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal currentBalance = grandTotal.subtract(currentPaid).setScale(2, RoundingMode.HALF_UP);

        BigDecimal paymentAmount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (paymentAmount.compareTo(currentBalance) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Payment amount (₹%s) exceeds outstanding balance (₹%s)", paymentAmount, currentBalance));
        }

        Business business = invoice.getBusiness();
        Customer customer = invoice.getCustomer();
        String currentUserEmail = currentUserService.getCurrentUser().getEmail();
        LocalDate payDate = request.paymentDate() != null ? request.paymentDate() : LocalDate.now();

        // Standardize payment method
        PaymentMethod pm = PaymentMethod.fromString(request.paymentMethod());

        // Generate sequential receipt number
        String receiptNumber = invoiceSequenceService.generateNextReceiptNumber(business, payDate);

        Payment payment = new Payment(
                business,
                invoice,
                customer,
                paymentAmount,
                payDate,
                pm.name(),
                request.referenceNumber(),
                request.notes(),
                currentUserEmail
        );
        payment.setReceiptNumber(receiptNumber);
        payment.setGatewayProvider(request.gatewayProvider() != null ? request.gatewayProvider() : "MANUAL");
        payment.setGatewayPaymentId(request.gatewayPaymentId());
        payment.setGatewayOrderId(request.gatewayOrderId());
        payment.setStatus("SUCCESS");

        Payment savedPayment = paymentRepository.save(payment);

        // Update invoice balances and status
        BigDecimal newPaid = currentPaid.add(paymentAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = grandTotal.subtract(newPaid).setScale(2, RoundingMode.HALF_UP);

        invoice.setPaidAmount(newPaid);
        invoice.setBalanceAmount(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus("PAID");
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus("PARTIALLY_PAID");
        }

        invoiceRepository.save(invoice);

        // Invalidate invoice PDF cache so balance updates in PDF
        storageService.delete("invoices/" + business.getId() + "/" + invoice.getId() + ".pdf");

        auditLogService.logAction("RECORD_PAYMENT", "PAYMENT", savedPayment.getId(),
                "Payment of Rs. " + savedPayment.getAmount() + " (Receipt: " + receiptNumber + ") recorded for invoice " + invoice.getInvoiceNumber());

        return toResponse(savedPayment, invoice);
    }

    /**
     * Webhook/system payment recording (e.g. from Razorpay/Cashfree webhook callback)
     */
    @Transactional
    public PaymentResponse recordGatewayPayment(
            Long invoiceId,
            Long businessId,
            BigDecimal amount,
            LocalDate paymentDate,
            String paymentMethod,
            String referenceNumber,
            String gatewayProvider,
            String gatewayPaymentId,
            String gatewayOrderId,
            String gatewaySignature,
            String notes
    ) {
        // Check for idempotency: if gatewayPaymentId already processed, return existing
        if (gatewayPaymentId != null && !gatewayPaymentId.isBlank()) {
            var existing = paymentRepository.findByGatewayPaymentId(gatewayPaymentId);
            if (existing.isPresent()) {
                Payment p = existing.get();
                return toResponse(p, p.getInvoice());
            }
        }

        Invoice invoice = invoiceRepository.findByIdAndBusiness_Id(invoiceId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found with ID: " + invoiceId));

        if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot record payment against a cancelled invoice");
        }

        BigDecimal currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal grandTotal = invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal paymentAmount = amount.setScale(2, RoundingMode.HALF_UP);

        Business business = invoice.getBusiness();
        Customer customer = invoice.getCustomer();
        LocalDate payDate = paymentDate != null ? paymentDate : LocalDate.now();

        PaymentMethod pm = PaymentMethod.fromString(paymentMethod);
        String receiptNumber = invoiceSequenceService.generateNextReceiptNumber(business, payDate);

        Payment payment = new Payment(
                business,
                invoice,
                customer,
                paymentAmount,
                payDate,
                pm.name(),
                referenceNumber,
                notes,
                "SYSTEM_WEBHOOK"
        );
        payment.setReceiptNumber(receiptNumber);
        payment.setGatewayProvider(gatewayProvider != null ? gatewayProvider : "GATEWAY");
        payment.setGatewayPaymentId(gatewayPaymentId);
        payment.setGatewayOrderId(gatewayOrderId);
        payment.setGatewaySignature(gatewaySignature);
        payment.setStatus("SUCCESS");

        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal newPaid = currentPaid.add(paymentAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = grandTotal.subtract(newPaid).setScale(2, RoundingMode.HALF_UP);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }

        invoice.setPaidAmount(newPaid);
        invoice.setBalanceAmount(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus("PAID");
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus("PARTIALLY_PAID");
        }

        invoiceRepository.save(invoice);
        storageService.delete("invoices/" + business.getId() + "/" + invoice.getId() + ".pdf");

        auditLogService.logAction("WEBHOOK_PAYMENT_CAPTURED", "PAYMENT", savedPayment.getId(),
                "Gateway payment " + gatewayPaymentId + " of Rs. " + paymentAmount + " captured for invoice " + invoice.getInvoiceNumber());

        return toResponse(savedPayment, invoice);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return paymentRepository.findByIdAndBusiness_Id(paymentId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment record not found with ID: " + paymentId));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return paymentRepository.findByBusiness_IdOrderByPaymentDateDesc(businessId)
                .stream()
                .map(p -> toResponse(p, p.getInvoice()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForInvoice(Long invoiceId) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return paymentRepository.findByInvoice_IdAndBusiness_IdOrderByPaymentDateDesc(invoiceId, businessId)
                .stream()
                .map(p -> toResponse(p, p.getInvoice()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForCustomer(Long customerId) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return paymentRepository.findByCustomer_IdAndBusiness_IdOrderByPaymentDateDesc(customerId, businessId)
                .stream()
                .map(p -> toResponse(p, p.getInvoice()))
                .toList();
    }

    @Transactional
    public void deletePayment(Long paymentId) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();

        Payment payment = paymentRepository.findByIdAndBusiness_Id(paymentId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment record not found"));

        Invoice invoice = payment.getInvoice();
        BigDecimal newPaid = (invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO)
                .subtract(payment.getAmount()).setScale(2, RoundingMode.HALF_UP);
        if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
            newPaid = BigDecimal.ZERO;
        }

        BigDecimal grandTotal = invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal newBalance = grandTotal.subtract(newPaid).setScale(2, RoundingMode.HALF_UP);

        invoice.setPaidAmount(newPaid);
        invoice.setBalanceAmount(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0 && grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus("PAID");
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus("PARTIALLY_PAID");
        } else {
            // Revert back to ISSUED
            invoice.setStatus("ISSUED");
        }

        invoiceRepository.save(invoice);
        paymentRepository.delete(payment);
        storageService.delete("invoices/" + businessId + "/" + invoice.getId() + ".pdf");
        storageService.delete("receipts/" + businessId + "/" + paymentId + ".pdf");

        auditLogService.logAction("DELETE_PAYMENT", "PAYMENT", paymentId,
                "Payment of Rs. " + payment.getAmount() + " deleted for invoice " + invoice.getInvoiceNumber());
    }

    public PaymentResponse toResponse(Payment p, Invoice inv) {
        return new PaymentResponse(
                p.getId(),
                inv != null ? inv.getId() : null,
                inv != null ? inv.getInvoiceNumber() : null,
                p.getCustomer() != null ? p.getCustomer().getId() : null,
                p.getCustomer() != null ? p.getCustomer().getName() : null,
                p.getAmount(),
                p.getPaymentDate(),
                p.getPaymentMethod(),
                p.getReferenceNumber(),
                p.getNotes(),
                p.getCreatedBy(),
                p.getCreatedAt(),
                inv != null ? inv.getGrandTotal() : null,
                inv != null ? inv.getPaidAmount() : null,
                inv != null ? inv.getBalanceAmount() : null,
                inv != null ? inv.getStatus() : null,
                p.getReceiptNumber(),
                p.getGatewayProvider(),
                p.getGatewayPaymentId(),
                p.getGatewayOrderId(),
                p.getStatus()
        );
    }
}
