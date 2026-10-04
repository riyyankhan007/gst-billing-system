package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.PaymentRequest;
import com.gstbilling.gst_billing.dto.PaymentResponse;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.Payment;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.repository.PaymentRepository;
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

    public PaymentService(PaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          CurrentUserService currentUserService) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.currentUserService = currentUserService;
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

        Payment payment = new Payment(
                business,
                invoice,
                customer,
                paymentAmount,
                request.paymentDate() != null ? request.paymentDate() : LocalDate.now(),
                request.paymentMethod(),
                request.referenceNumber(),
                request.notes(),
                currentUserEmail
        );

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

        return toResponse(savedPayment, invoice);
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
            // Revert back to ISSUED or DRAFT
            invoice.setStatus("ISSUED");
        }

        invoiceRepository.save(invoice);
        paymentRepository.delete(payment);
    }

    private PaymentResponse toResponse(Payment p, Invoice inv) {
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
                inv != null ? inv.getStatus() : null
        );
    }
}
