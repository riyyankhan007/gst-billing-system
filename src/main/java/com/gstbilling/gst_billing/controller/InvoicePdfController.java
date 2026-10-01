package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.service.InvoicePdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
public class InvoicePdfController {

    private final InvoicePdfService invoicePdfService;

    public InvoicePdfController(
            InvoicePdfService invoicePdfService
    ) {
        this.invoicePdfService = invoicePdfService;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable Long id
    ) {

        byte[] pdf =
                invoicePdfService.generateInvoicePdf(id);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_PDF
        );

        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("invoice-" + id + ".pdf")
                        .build()
        );

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(pdf);
    }
}