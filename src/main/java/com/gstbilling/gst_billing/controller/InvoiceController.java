package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.service.InvoiceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public Invoice createInvoice(@RequestBody Invoice invoice) {
        return invoiceService.createInvoice(invoice);
    }

    @GetMapping
    public List<Invoice> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }

    @GetMapping("/{id}")
    public Invoice getInvoiceById(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id);
    }
    @PutMapping("/{id}/paid")
    public Invoice markAsPaid(@PathVariable Long id) {
        return invoiceService.markAsPaid(id);
    }

    @PutMapping("/{id}/cancel")
    public Invoice cancelInvoice(@PathVariable Long id) {
        return invoiceService.cancelInvoice(id);
    }
}