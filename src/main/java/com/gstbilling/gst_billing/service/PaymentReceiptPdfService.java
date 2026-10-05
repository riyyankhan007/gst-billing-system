package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.Payment;
import com.gstbilling.gst_billing.storage.StorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PaymentReceiptPdfService {

    private final PaymentService paymentService;
    private final StorageService storageService;

    public PaymentReceiptPdfService(PaymentService paymentService, StorageService storageService) {
        this.paymentService = paymentService;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public byte[] generateReceiptPdf(Long paymentId) {
        Payment payment = paymentService.getPaymentById(paymentId);
        Long businessId = payment.getBusiness() != null ? payment.getBusiness().getId() : 0L;
        String cacheKey = "receipts/" + businessId + "/" + payment.getId() + ".pdf";

        if (storageService.exists(cacheKey)) {
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
                float left = 45;
                float right = 550;
                float y = 790;

                // Header Title
                write(content, "PAYMENT RECEIPT", 18, true, left, y);
                y -= 25;

                Business business = payment.getBusiness();
                Invoice invoice = payment.getInvoice();
                Customer customer = payment.getCustomer();

                // Business Details (Left Column)
                write(content, business != null && business.getName() != null ? business.getName() : "Business", 13, true, left, y);
                y -= 14;
                if (business != null && business.getAddress() != null) {
                    write(content, business.getAddress(), 9, false, left, y);
                    y -= 12;
                }
                write(content, "GSTIN: " + (business != null && business.getGstin() != null ? business.getGstin() : "N/A"), 9, true, left, y);
                y -= 12;
                if (business != null && business.getState() != null) {
                    write(content, "State: " + business.getState() + (business.getStateCode() != null ? " (" + business.getStateCode() + ")" : ""), 9, false, left, y);
                    y -= 12;
                }
                if (business != null && business.getEmail() != null) {
                    write(content, "Email: " + business.getEmail() + (business.getPhone() != null ? " | Phone: " + business.getPhone() : ""), 9, false, left, y);
                    y -= 12;
                }

                // Receipt Meta (Right Column)
                float rightX = 360;
                float rightY = 790;
                write(content, "Receipt No: " + (payment.getReceiptNumber() != null ? payment.getReceiptNumber() : "RCP-" + payment.getId()), 11, true, rightX, rightY);
                rightY -= 15;
                write(content, "Receipt Date: " + payment.getPaymentDate(), 9, false, rightX, rightY);
                rightY -= 13;
                write(content, "Payment Mode: " + payment.getPaymentMethod(), 9, true, rightX, rightY);
                rightY -= 13;
                if (payment.getReferenceNumber() != null && !payment.getReferenceNumber().isBlank()) {
                    write(content, "Ref / Txn ID: " + payment.getReferenceNumber(), 9, false, rightX, rightY);
                    rightY -= 13;
                }
                if (payment.getGatewayPaymentId() != null && !payment.getGatewayPaymentId().isBlank()) {
                    write(content, "Gateway ID: " + payment.getGatewayPaymentId(), 8, false, rightX, rightY);
                    rightY -= 13;
                }
                write(content, "Status: " + payment.getStatus(), 9, true, rightX, rightY);

                y = Math.min(y, rightY) - 20;

                // Horizontal separator
                drawLine(content, left, y, right, y);
                y -= 25;

                // Received From Box
                write(content, "RECEIVED FROM:", 10, true, left, y);
                y -= 14;
                write(content, customer != null && customer.getName() != null ? customer.getName() : "Customer", 11, true, left, y);
                y -= 13;
                if (customer != null && customer.getBillingAddress() != null) {
                    write(content, customer.getBillingAddress(), 9, false, left, y);
                    y -= 12;
                }
                if (customer != null && customer.getGstin() != null && !customer.getGstin().isBlank()) {
                    write(content, "GSTIN: " + customer.getGstin(), 9, false, left, y);
                    y -= 12;
                }
                if (customer != null && customer.getState() != null) {
                    write(content, "State: " + customer.getState() + (customer.getStateCode() != null ? " (" + customer.getStateCode() + ")" : ""), 9, false, left, y);
                    y -= 12;
                }

                y -= 20;
                drawLine(content, left, y, right, y);
                y -= 30;

                // Payment Details Summary Box
                write(content, "PAYMENT SUMMARY", 11, true, left, y);
                y -= 20;

                // Table-like row layout
                content.setNonStrokingColor(0.95f, 0.95f, 0.95f);
                content.addRect(left, y - 5, right - left, 22);
                content.fill();
                content.setNonStrokingColor(0f, 0f, 0f);

                write(content, "Description", 10, true, left + 10, y + 2);
                write(content, "Amount (INR)", 10, true, right - 100, y + 2);
                y -= 20;

                // Rows
                write(content, "Amount Received Against Invoice " + (invoice != null ? invoice.getInvoiceNumber() : "-"), 10, false, left + 10, y);
                write(content, "Rs. " + formatCurrency(payment.getAmount()), 10, true, right - 100, y);
                y -= 18;

                if (invoice != null) {
                    BigDecimal grandTotal = invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO;
                    BigDecimal paid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
                    BigDecimal balance = invoice.getBalanceAmount() != null ? invoice.getBalanceAmount() : BigDecimal.ZERO;

                    write(content, "Invoice Total", 9, false, left + 10, y);
                    write(content, "Rs. " + formatCurrency(grandTotal), 9, false, right - 100, y);
                    y -= 15;

                    write(content, "Total Paid to Date", 9, false, left + 10, y);
                    write(content, "Rs. " + formatCurrency(paid), 9, false, right - 100, y);
                    y -= 15;

                    write(content, "Remaining Balance Due", 10, true, left + 10, y);
                    write(content, "Rs. " + formatCurrency(balance), 10, true, right - 100, y);
                    y -= 25;
                }

                drawLine(content, left, y, right, y);
                y -= 25;

                // Notes
                if (payment.getNotes() != null && !payment.getNotes().isBlank()) {
                    write(content, "Notes / Remarks: " + payment.getNotes(), 9, false, left, y);
                    y -= 20;
                }

                // Footer & Signatory
                float footY = 120;
                drawLine(content, right - 180, footY + 40, right, footY + 40);
                write(content, "Authorised Signatory", 9, true, right - 150, footY + 25);
                write(content, business != null && business.getName() != null ? business.getName() : "", 8, false, right - 150, footY + 12);

                write(content, "This is a computer generated payment receipt. Thank you for your business!", 8, false, left, footY);
            }

            document.save(output);
            byte[] pdfBytes = output.toByteArray();
            storageService.store(cacheKey, pdfBytes, "application/pdf");
            return pdfBytes;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate payment receipt PDF: " + e.getMessage(), e);
        }
    }

    private void write(PDPageContentStream content, String text, float size, boolean bold, float x, float y) throws IOException {
        if (text == null) return;
        content.beginText();
        content.setFont(new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA), size);
        content.newLineAtOffset(x, y);
        content.showText(cleanText(text));
        content.endText();
    }

    private void drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2) throws IOException {
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private String cleanText(String input) {
        if (input == null) return "";
        return input.replace("₹", "Rs. ")
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("[^\\x20-\\x7E]", "");
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
