package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.ReportDto.*;
import com.gstbilling.gst_billing.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    public SalesReport getSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return reportService.getSalesReport(startDate, endDate);
    }

    @GetMapping("/gst")
    public GstSummaryReport getGstReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return reportService.getGstReport(startDate, endDate);
    }

    @GetMapping("/customers")
    public List<CustomerReportItem> getCustomerReport() {
        return reportService.getCustomerReport();
    }

    @GetMapping("/products")
    public List<ProductReportItem> getProductReport() {
        return reportService.getProductReport();
    }

    @GetMapping("/export/invoices")
    public ResponseEntity<String> exportInvoicesCsv() {
        String csv = reportService.exportInvoicesCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoices_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/export/customers")
    public ResponseEntity<String> exportCustomersCsv() {
        String csv = reportService.exportCustomersCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customers_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/export/products")
    public ResponseEntity<String> exportProductsCsv() {
        String csv = reportService.exportProductsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/export/payments")
    public ResponseEntity<String> exportPaymentsCsv() {
        String csv = reportService.exportPaymentsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payments_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
