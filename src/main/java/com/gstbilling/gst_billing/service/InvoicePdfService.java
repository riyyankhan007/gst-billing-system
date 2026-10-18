package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.InvoiceItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.gstbilling.gst_billing.storage.StorageException;
import com.gstbilling.gst_billing.storage.StorageService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class InvoicePdfService {

    private static final Logger log = LoggerFactory.getLogger(InvoicePdfService.class);

    private final InvoiceService invoiceService;
    private final StorageService storageService;

    public InvoicePdfService(
            InvoiceService invoiceService,
            StorageService storageService
    ) {
        this.invoiceService = invoiceService;
        this.storageService = storageService;
    }

    public void invalidateInvoicePdfCache(Long businessId, Long invoiceId) {
        String cacheKey = "invoices/" + businessId + "/" + invoiceId + ".pdf";
        storageService.delete(cacheKey);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);
        Long businessId = (invoice.getBusiness() != null) ? invoice.getBusiness().getId() : 0L;
        String cacheKey = "invoices/" + businessId + "/" + invoice.getId() + ".pdf";

        // If finalized and cached, return cached PDF bytes immediately
        if (!"DRAFT".equalsIgnoreCase(invoice.getStatus()) && storageService.exists(cacheKey)) {
            byte[] cached = storageService.retrieve(cacheKey);
            if (cached != null && cached.length > 0) {
                return cached;
            }
        }

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float left = 40;
                float right = 555;
                float y = 800;

                // Header Title
                write(content, "TAX INVOICE", 18, true, left, y);

                // If IRN is present, display e-invoice header
                if (invoice.getIrn() != null && !invoice.getIrn().isBlank()) {
                    write(content, "IRN: " + truncate(invoice.getIrn(), 64), 7, false, left + 130, y + 4);
                    String ackInfo = "Ack No: " + (invoice.getAckNo() != null ? invoice.getAckNo() : "-") +
                            (invoice.getAckDate() != null ? " | Dt: " + invoice.getAckDate().toString() : "");
                    write(content, ackInfo, 7, false, left + 130, y - 5);
                }

                Business business = invoice.getBusiness();

                // Logo if present
                byte[] logoBytes = (business != null) ? loadAssetBytes(business.getLogo()) : null;
                if (logoBytes != null && logoBytes.length > 0) {
                    try {
                        PDImageXObject logo = PDImageXObject.createFromByteArray(document, logoBytes, "logo");
                        content.drawImage(logo, 470, y - 35, 80, 40);
                    } catch (Exception e) {
                        log.debug("Could not render logo in invoice PDF: {}", e.getMessage());
                    }
                }

                y -= 30;

                // Business Details (Left Column)
                write(content, business != null && business.getName() != null ? business.getName() : "Business Name", 14, true, left, y);
                y -= 15;
                write(content, business != null && business.getAddress() != null ? business.getAddress() : "", 9, false, left, y);
                y -= 13;
                write(content, "GSTIN: " + (business != null && business.getGstin() != null ? business.getGstin() : "N/A"), 9, true, left, y);
                y -= 12;
                if (business != null && business.getPan() != null && !business.getPan().isBlank()) {
                    write(content, "PAN: " + business.getPan(), 9, false, left, y);
                    y -= 12;
                }
                write(content, "State: " + (business != null && business.getState() != null ? business.getState() : "") +
                        (business != null && business.getStateCode() != null ? " (" + business.getStateCode() + ")" : ""), 9, false, left, y);
                y -= 12;
                if (business != null && business.getPhone() != null) {
                    write(content, "Phone: " + business.getPhone() + (business.getEmail() != null ? " | Email: " + business.getEmail() : ""), 9, false, left, y);
                    y -= 12;
                }

                // Invoice Meta (Right Column)
                float rightX = 380;
                float rightY = 770;
                write(content, "Invoice No: " + invoice.getInvoiceNumber(), 10, true, rightX, rightY);
                rightY -= 14;
                write(content, "Invoice Date: " + invoice.getInvoiceDate(), 9, false, rightX, rightY);
                rightY -= 13;
                write(content, "Due Date: " + (invoice.getDueDate() != null ? invoice.getDueDate() : "Due on receipt"), 9, false, rightX, rightY);
                rightY -= 13;
                write(content, "Status: " + (invoice.getStatus() != null ? invoice.getStatus() : "DRAFT"), 9, true, rightX, rightY);
                rightY -= 13;
                if (Boolean.TRUE.equals(invoice.getReverseCharge())) {
                    write(content, "Reverse Charge: YES", 9, false, rightX, rightY);
                    rightY -= 13;
                }

                y = Math.min(y, rightY) - 10;
                line(content, left, y, right, y);
                y -= 15;

                // Customer Details (Bill To)
                Customer customer = invoice.getCustomer();
                write(content, "BILL TO (CUSTOMER):", 10, true, left, y);
                y -= 14;
                write(content, customer != null ? customer.getName() : "Customer", 11, true, left, y);
                y -= 13;
                write(content, "Address: " + (customer != null && customer.getBillingAddress() != null ? customer.getBillingAddress() : (customer != null && customer.getAddress() != null ? customer.getAddress() : "")), 9, false, left, y);
                y -= 12;
                write(content, "GSTIN: " + (customer != null && customer.getGstin() != null ? customer.getGstin() : "Unregistered/B2C"), 9, false, left, y);
                y -= 12;
                if (customer != null && customer.getPan() != null && !customer.getPan().isBlank()) {
                    write(content, "PAN: " + customer.getPan(), 9, false, left, y);
                    y -= 12;
                }
                write(content, "State: " + (customer != null && customer.getState() != null ? customer.getState() : "") +
                        (customer != null && customer.getStateCode() != null ? " (" + customer.getStateCode() + ")" : ""), 9, false, left, y);
                y -= 15;

                // Table Header
                line(content, left, y, right, y);
                y -= 12;
                write(content, "Item / Description", 9, true, left + 5, y);
                write(content, "HSN/SAC", 9, true, 200, y);
                write(content, "Qty", 9, true, 255, y);
                write(content, "Rate", 9, true, 295, y);
                write(content, "Disc", 9, true, 345, y);
                write(content, "Taxable", 9, true, 390, y);
                write(content, "GST", 9, true, 455, y);
                write(content, "Total (Rs)", 9, true, 495, y);
                y -= 6;
                line(content, left, y, right, y);
                y -= 15;

                // Table Rows
                if (invoice.getItems() != null) {
                    for (InvoiceItem item : invoice.getItems()) {
                        write(content, truncate(item.getProductName(), 28), 8, false, left + 5, y);
                        write(content, item.getHsnCode() != null ? item.getHsnCode() : "-", 8, false, 200, y);
                        write(content, (item.getQuantity() != null ? item.getQuantity().toString() : "1") + (item.getUnit() != null ? " " + item.getUnit() : ""), 8, false, 255, y);
                        write(content, formatNum(item.getUnitPrice()), 8, false, 295, y);
                        write(content, formatNum(item.getDiscount()), 8, false, 345, y);
                        write(content, formatNum(item.getTaxableAmount()), 8, false, 390, y);
                        write(content, (item.getGstRate() != null ? item.getGstRate() : BigDecimal.ZERO) + "%", 8, false, 455, y);
                        write(content, formatNum(item.getTotalAmount()), 8, false, 495, y);
                        y -= 15;
                    }
                }

                line(content, left, y, right, y);
                y -= 15;

                // Summary / Totals & HSN/SAC Tax Breakdown
                float totalX1 = 360;
                float totalX2 = 490;

                // Left side: Statutory HSN/SAC Summary
                float hsnX = left + 5;
                float hsnY = y;
                write(content, "HSN/SAC TAX SUMMARY:", 8, true, hsnX, hsnY);
                hsnY -= 11;
                write(content, "HSN/SAC", 7, true, hsnX, hsnY);
                write(content, "Taxable", 7, true, hsnX + 55, hsnY);
                write(content, "Rate", 7, true, hsnX + 115, hsnY);
                write(content, "Tax Amt", 7, true, hsnX + 155, hsnY);
                hsnY -= 10;

                java.util.Map<String, java.math.BigDecimal[]> hsnMap = new java.util.LinkedHashMap<>();
                if (invoice.getItems() != null) {
                    for (InvoiceItem item : invoice.getItems()) {
                        String code = (item.getHsnCode() != null && !item.getHsnCode().isBlank()) ? item.getHsnCode() : "N/A";
                        java.math.BigDecimal taxVal = item.getTaxableAmount() != null ? item.getTaxableAmount() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal taxAmt = item.getTaxAmount() != null ? item.getTaxAmount() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal rate = item.getGstRate() != null ? item.getGstRate() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal[] acc = hsnMap.computeIfAbsent(code, k -> new java.math.BigDecimal[]{java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, rate});
                        acc[0] = acc[0].add(taxVal);
                        acc[1] = acc[1].add(taxAmt);
                    }
                }
                for (java.util.Map.Entry<String, java.math.BigDecimal[]> entry : hsnMap.entrySet()) {
                    write(content, entry.getKey(), 7, false, hsnX, hsnY);
                    write(content, formatNum(entry.getValue()[0]), 7, false, hsnX + 55, hsnY);
                    write(content, entry.getValue()[2] + "%", 7, false, hsnX + 115, hsnY);
                    write(content, formatNum(entry.getValue()[1]), 7, false, hsnX + 155, hsnY);
                    hsnY -= 10;
                }

                write(content, "Taxable Amount:", 9, false, totalX1, y);
                write(content, formatCurrency(invoice.getTaxableAmount()), 9, false, totalX2, y);
                y -= 13;

                if (invoice.getCgst() != null && invoice.getCgst().compareTo(BigDecimal.ZERO) > 0) {
                    write(content, "CGST:", 9, false, totalX1, y);
                    write(content, formatCurrency(invoice.getCgst()), 9, false, totalX2, y);
                    y -= 13;
                }
                if (invoice.getSgst() != null && invoice.getSgst().compareTo(BigDecimal.ZERO) > 0) {
                    write(content, "SGST:", 9, false, totalX1, y);
                    write(content, formatCurrency(invoice.getSgst()), 9, false, totalX2, y);
                    y -= 13;
                }
                if (invoice.getIgst() != null && invoice.getIgst().compareTo(BigDecimal.ZERO) > 0) {
                    write(content, "IGST:", 9, false, totalX1, y);
                    write(content, formatCurrency(invoice.getIgst()), 9, false, totalX2, y);
                    y -= 13;
                }

                write(content, "Total Tax:", 9, false, totalX1, y);
                write(content, formatCurrency(invoice.getTotalTax()), 9, false, totalX2, y);
                y -= 14;

                line(content, totalX1 - 10, y, right, y);
                y -= 14;
                write(content, "Grand Total:", 11, true, totalX1, y);
                write(content, formatCurrency(invoice.getGrandTotal()), 11, true, totalX2, y);
                y -= 14;

                if (invoice.getPaidAmount() != null && invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                    write(content, "Paid Amount:", 9, false, totalX1, y);
                    write(content, formatCurrency(invoice.getPaidAmount()), 9, false, totalX2, y);
                    y -= 12;
                    write(content, "Balance Due:", 10, true, totalX1, y);
                    write(content, formatCurrency(invoice.getBalanceAmount()), 10, true, totalX2, y);
                    y -= 14;
                }

                line(content, left, y, right, y);
                y -= 15;

                // Amount in words
                if (invoice.getAmountInWords() != null && !invoice.getAmountInWords().isBlank()) {
                    write(content, "Amount in Words: " + invoice.getAmountInWords(), 9, true, left, y);
                    y -= 18;
                }

                // Bank Details & UPI (Left) + Authorized Signatory (Right)
                float bottomY = y;
                if (business != null && business.getBankName() != null && !business.getBankName().isBlank()) {
                    write(content, "BANK DETAILS FOR PAYMENT:", 9, true, left, bottomY);
                    bottomY -= 12;
                    write(content, "Bank: " + business.getBankName(), 8, false, left, bottomY);
                    bottomY -= 11;
                    write(content, "A/C No: " + (business.getBankAccountNumber() != null ? business.getBankAccountNumber() : ""), 8, false, left, bottomY);
                    bottomY -= 11;
                    write(content, "IFSC: " + (business.getBankIfsc() != null ? business.getBankIfsc() : ""), 8, false, left, bottomY);
                    bottomY -= 11;
                    if (business.getUpiId() != null && !business.getUpiId().isBlank()) {
                        write(content, "UPI ID: " + business.getUpiId(), 8, false, left, bottomY);
                        bottomY -= 11;
                    }
                }

                // Terms and Conditions
                if (invoice.getTermsAndConditions() != null && !invoice.getTermsAndConditions().isBlank()) {
                    write(content, "Terms & Conditions:", 8, true, left, bottomY - 5);
                    bottomY -= 16;
                    String terms = truncate(invoice.getTermsAndConditions().replace("\n", " "), 90);
                    write(content, terms, 7, false, left, bottomY);
                }

                // Signatory (Right side)
                float sigY = y;
                write(content, "For " + (business != null ? business.getName() : "Business"), 9, true, 400, sigY);
                sigY -= 35;
                // Stamp/Signature image if available
                byte[] sigBytes = (business != null) ? loadAssetBytes(business.getSignature()) : null;
                if (sigBytes != null && sigBytes.length > 0) {
                    try {
                        PDImageXObject sigImg = PDImageXObject.createFromByteArray(document, sigBytes, "sig");
                        content.drawImage(sigImg, 420, sigY - 10, 70, 30);
                        sigY -= 15;
                    } catch (Exception e) {
                        log.debug("Could not render signature in invoice PDF: {}", e.getMessage());
                    }
                }
                sigY -= 10;
                write(content, "Authorized Signatory", 8, false, 410, sigY);

                // Footer
                write(content, "This is a computer generated invoice. Thank you for your business!", 8, false, 160, 25);
            }

            document.save(output);
            byte[] pdfBytes = output.toByteArray();
            if (!"DRAFT".equalsIgnoreCase(invoice.getStatus())) {
                try {
                    storageService.store(cacheKey, pdfBytes, "application/pdf");
                } catch (Exception e) {
                    log.error("Failed to store finalized invoice PDF (key={}): {}", cacheKey, e.getMessage());
                    throw new StorageException("Failed to persist invoice PDF to storage: " + cacheKey, e);
                }
            }
            return pdfBytes;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private byte[] loadAssetBytes(String assetPathOrUrl) {
        if (assetPathOrUrl == null || assetPathOrUrl.isBlank()) {
            return null;
        }
        String clean = assetPathOrUrl.trim().replace("\\", "/");
        while (clean.startsWith("/")) clean = clean.substring(1);

        try {
            if (storageService.exists(clean)) {
                byte[] bytes = storageService.retrieve(clean);
                if (bytes != null && bytes.length > 0) return bytes;
            }
        } catch (Exception ignored) {}

        String filename = clean.contains("/") ? clean.substring(clean.lastIndexOf('/') + 1) : clean;
        for (String prefix : new String[]{"logos/", "signatures/", "uploads/"}) {
            try {
                String candidate = prefix + filename;
                if (storageService.exists(candidate)) {
                    byte[] bytes = storageService.retrieve(candidate);
                    if (bytes != null && bytes.length > 0) return bytes;
                }
            } catch (Exception ignored) {}
        }

        try {
            Path localPath = Path.of("uploads", filename);
            if (Files.exists(localPath) && Files.isRegularFile(localPath)) {
                return Files.readAllBytes(localPath);
            }
        } catch (Exception ignored) {}

        return null;
    }

    private void write(PDPageContentStream content, String text, int size, boolean bold, float x, float y) throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA), size);
        content.newLineAtOffset(x, y);
        content.showText(text != null ? text : "");
        content.endText();
    }

    private void line(PDPageContentStream content, float x1, float y1, float x2, float y2) throws IOException {
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private String formatNum(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "Rs. 0.00";
        return "Rs. " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
