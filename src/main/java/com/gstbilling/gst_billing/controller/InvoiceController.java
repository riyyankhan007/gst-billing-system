package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.ReminderResponse;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.service.InvoicePdfService;
import com.gstbilling.gst_billing.service.InvoiceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES')")
    public ResponseEntity<Invoice> createInvoice(@jakarta.validation.Valid @RequestBody com.gstbilling.gst_billing.dto.CreateInvoiceRequest request) {
        Invoice invoice = mapToEntity(request);
        Invoice created = invoiceService.createInvoice(invoice);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
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
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public Invoice getInvoiceById(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES')")
    public Invoice updateDraftInvoice(@PathVariable Long id, @jakarta.validation.Valid @RequestBody com.gstbilling.gst_billing.dto.CreateInvoiceRequest request) {
        Invoice invoice = mapToEntity(request);
        return invoiceService.updateDraftInvoice(id, invoice);
    }

    private Invoice mapToEntity(com.gstbilling.gst_billing.dto.CreateInvoiceRequest request) {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(request.customerId());
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDueDate(request.dueDate());
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setDiscountAmount(request.discountAmount() != null ? request.discountAmount() : java.math.BigDecimal.ZERO);
        invoice.setReverseCharge(request.reverseCharge());
        invoice.setExportType(request.exportType());
        invoice.setNotes(request.notes());
        invoice.setTermsAndConditions(request.termsAndConditions());
        invoice.setStatus(request.status());

        if (request.items() != null) {
            List<com.gstbilling.gst_billing.entity.InvoiceItem> items = request.items().stream().map(i -> {
                com.gstbilling.gst_billing.entity.InvoiceItem item = new com.gstbilling.gst_billing.entity.InvoiceItem();
                item.setProductId(i.productId());
                item.setProductName(i.productName());
                item.setHsnCode(i.hsnCode());
                item.setQuantity(i.quantity());
                item.setUnitPrice(i.unitPrice());
                item.setGstRate(i.gstRate());
                item.setDiscount(i.discount() != null ? i.discount() : java.math.BigDecimal.ZERO);
                item.setTaxInclusive(i.isTaxInclusive());
                return item;
            }).toList();
            invoice.setItems(items);
        }
        return invoice;
    }

    @PutMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES')")
    public Invoice issueInvoice(@PathVariable Long id) {
        return invoiceService.issueInvoice(id);
    }

    @PutMapping("/{id}/sent")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES')")
    public Invoice markAsSent(@PathVariable Long id) {
        return invoiceService.markAsSent(id);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public Invoice cancelInvoice(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        return invoiceService.cancelInvoice(id, reason);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public Invoice cancelInvoice(Long id) {
        return cancelInvoice(id, (String) null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
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
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public ReminderResponse getReminder(@PathVariable Long id) {
        return invoiceService.getReminderDetails(id);
    }

    @PostMapping("/{id}/remind-email")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES')")
    public ReminderResponse sendEmailReminder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = (body != null) ? body.get("note") : null;
        return invoiceService.sendInvoiceReminder(id, note);
    }
}