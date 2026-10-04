package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.PaymentRequest;
import com.gstbilling.gst_billing.dto.PaymentResponse;
import com.gstbilling.gst_billing.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> recordPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.recordPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/invoice/{invoiceId}")
    public List<PaymentResponse> getPaymentsForInvoice(@PathVariable Long invoiceId) {
        return paymentService.getPaymentsForInvoice(invoiceId);
    }

    @GetMapping("/customer/{customerId}")
    public List<PaymentResponse> getPaymentsForCustomer(@PathVariable Long customerId) {
        return paymentService.getPaymentsForCustomer(customerId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}
