package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.PaymentRequest;
import com.gstbilling.gst_billing.dto.PaymentResponse;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.integration.payment.PaymentGatewayProvider;
import com.gstbilling.gst_billing.integration.payment.PaymentOrderRequest;
import com.gstbilling.gst_billing.integration.payment.PaymentOrderResponse;
import com.gstbilling.gst_billing.service.CurrentUserService;
import com.gstbilling.gst_billing.service.InvoiceService;
import com.gstbilling.gst_billing.service.PaymentReceiptPdfService;
import com.gstbilling.gst_billing.service.PaymentReconciliationService;
import com.gstbilling.gst_billing.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentReceiptPdfService receiptPdfService;
    private final PaymentReconciliationService reconciliationService;
    private final List<PaymentGatewayProvider> gatewayProviders;
    private final InvoiceService invoiceService;
    private final CurrentUserService currentUserService;

    public PaymentController(
            PaymentService paymentService,
            PaymentReceiptPdfService receiptPdfService,
            PaymentReconciliationService reconciliationService,
            List<PaymentGatewayProvider> gatewayProviders,
            InvoiceService invoiceService,
            CurrentUserService currentUserService
    ) {
        this.paymentService = paymentService;
        this.receiptPdfService = receiptPdfService;
        this.reconciliationService = reconciliationService;
        this.gatewayProviders = gatewayProviders;
        this.invoiceService = invoiceService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<PaymentResponse> recordPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.recordPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public List<PaymentResponse> getPaymentsForInvoice(@PathVariable Long invoiceId) {
        return paymentService.getPaymentsForInvoice(invoiceId);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public List<PaymentResponse> getPaymentsForCustomer(@PathVariable Long customerId) {
        return paymentService.getPaymentsForCustomer(customerId);
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public ResponseEntity<byte[]> getPaymentReceiptPdf(@PathVariable Long id) {
        byte[] pdf = receiptPdfService.generateReceiptPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"receipt_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/order")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES')")
    public ResponseEntity<PaymentOrderResponse> createPaymentOrder(@RequestBody PaymentOrderRequest request) {
        PaymentGatewayProvider provider = gatewayProviders.stream()
                .filter(p -> "RAZORPAY".equalsIgnoreCase(p.getProviderName()))
                .findFirst()
                .orElse(gatewayProviders.isEmpty() ? null : gatewayProviders.get(0));

        if (provider == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        // Fill invoice details if not provided
        PaymentOrderRequest finalRequest = request;
        if (request.invoiceId() != null) {
            Invoice inv = invoiceService.getInvoiceById(request.invoiceId());
            Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
            finalRequest = new PaymentOrderRequest(
                    inv.getId(),
                    request.amount() != null ? request.amount() : inv.getBalanceAmount(),
                    "INR",
                    inv.getCustomer() != null ? inv.getCustomer().getName() : "Customer",
                    inv.getCustomer() != null ? inv.getCustomer().getEmail() : "",
                    inv.getCustomer() != null ? inv.getCustomer().getPhone() : "",
                    "RCP_ORD_" + inv.getInvoiceNumber(),
                    Map.of(
                            "invoiceId", String.valueOf(inv.getId()),
                            "businessId", String.valueOf(businessId)
                    )
            );
        }

        PaymentOrderResponse order = provider.createPaymentOrder(finalRequest);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/reconcile")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<PaymentReconciliationService.ReconciliationSummary> reconcileAllPayments() {
        var summary = reconciliationService.reconcileAllInvoices();
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/reconcile/invoice/{invoiceId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<PaymentReconciliationService.ReconciliationItem> reconcileInvoice(@PathVariable Long invoiceId) {
        var item = reconciliationService.reconcileInvoice(invoiceId);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}
