package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.ReminderResponse;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.service.InvoicePdfService;
import com.gstbilling.gst_billing.service.InvoiceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    public InvoiceController(InvoiceService invoiceService, InvoicePdfService invoicePdfService) {
        this.invoiceService = invoiceService;
        this.invoicePdfService = invoicePdfService;
    }

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
        Invoice created = invoiceService.createInvoice(invoice);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Invoice> getAllInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId
    ) {
        if (status != null || customerId != null) {
            return invoiceService.searchInvoices(status, customerId);
        }
        return invoiceService.getAllInvoices();
    }

    @GetMapping("/{id}")
    public Invoice getInvoiceById(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id);
    }

    @PutMapping("/{id}")
    public Invoice updateDraftInvoice(@PathVariable Long id, @RequestBody Invoice invoice) {
        return invoiceService.updateDraftInvoice(id, invoice);
    }

    @PutMapping("/{id}/issue")
    public Invoice issueInvoice(@PathVariable Long id) {
        return invoiceService.issueInvoice(id);
    }

    @PutMapping("/{id}/sent")
    public Invoice markAsSent(@PathVariable Long id) {
        return invoiceService.markAsSent(id);
    }

    @PutMapping("/{id}/cancel")
    public Invoice cancelInvoice(@PathVariable Long id) {
        return invoiceService.cancelInvoice(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(id);
        Invoice invoice = invoiceService.getInvoiceById(id);
        String fileName = "Invoice_" + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber().replace("/", "_") : id) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", fileName);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/reminder")
    public ReminderResponse getReminder(@PathVariable Long id) {
        return invoiceService.getReminderDetails(id);
    }

    @PostMapping("/{id}/remind-email")
    public ReminderResponse sendEmailReminder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = (body != null) ? body.get("note") : null;
        return invoiceService.sendInvoiceReminder(id, note);
    }
}