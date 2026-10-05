package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.PaymentResponse;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.integration.payment.PaymentGatewayProvider;
import com.gstbilling.gst_billing.integration.payment.PaymentWebhookEvent;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/payments")
public class PaymentWebhookController {

    private final List<PaymentGatewayProvider> providers;
    private final PaymentService paymentService;
    private final InvoiceRepository invoiceRepository;

    @Value("${app.payment.razorpay.webhook-secret:mock_secret}")
    private String razorpayWebhookSecret;

    public PaymentWebhookController(
            List<PaymentGatewayProvider> providers,
            PaymentService paymentService,
            InvoiceRepository invoiceRepository
    ) {
        this.providers = providers;
        this.paymentService = paymentService;
        this.invoiceRepository = invoiceRepository;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<Map<String, Object>> handlePaymentWebhook(
            @PathVariable String provider,
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String rzpSignature,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String genericSignature
    ) {
        PaymentGatewayProvider gatewayProvider = providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(provider))
                .findFirst()
                .orElse(null);

        if (gatewayProvider == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Unknown payment gateway provider: " + provider));
        }

        String signature = rzpSignature != null ? rzpSignature : genericSignature;
        boolean valid = gatewayProvider.verifyWebhookSignature(payload, signature, razorpayWebhookSecret);
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid webhook signature"));
        }

        PaymentWebhookEvent event = gatewayProvider.parseWebhookEvent(payload);

        // Process captured payment
        if ("payment.captured".equalsIgnoreCase(event.eventType())
                || "order.paid".equalsIgnoreCase(event.eventType())
                || "SUCCESS".equalsIgnoreCase(event.status())) {

            String invoiceIdStr = event.notes().get("invoiceId");
            if (invoiceIdStr == null || invoiceIdStr.isBlank()) {
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "No invoiceId in webhook event notes"));
            }

            Long invoiceId = Long.parseLong(invoiceIdStr);
            Long businessId;

            String businessIdStr = event.notes().get("businessId");
            if (businessIdStr != null && !businessIdStr.isBlank()) {
                businessId = Long.parseLong(businessIdStr);
            } else {
                Invoice inv = invoiceRepository.findById(invoiceId).orElse(null);
                if (inv == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Invoice not found for ID: " + invoiceId));
                }
                businessId = inv.getBusiness().getId();
            }

            BigDecimal amount = event.amount();
            String method = event.notes().getOrDefault("method", "UPI");
            String paymentId = event.paymentId();
            String orderId = event.orderId();

            PaymentResponse recorded = paymentService.recordGatewayPayment(
                    invoiceId,
                    businessId,
                    amount,
                    LocalDate.now(),
                    method,
                    paymentId,
                    gatewayProvider.getProviderName(),
                    paymentId,
                    orderId,
                    signature,
                    "Webhook payment: " + event.eventType()
            );

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "paymentId", recorded.id(),
                    "receiptNumber", recorded.receiptNumber() != null ? recorded.receiptNumber() : "",
                    "invoiceStatus", recorded.invoiceStatus()
            ));
        }

        return ResponseEntity.ok(Map.of("status", "ignored", "eventType", event.eventType()));
    }
}
