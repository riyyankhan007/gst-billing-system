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
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class InvoicePdfService {

    private final InvoiceService invoiceService;

    public InvoicePdfService(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public byte[] generateInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                float left = 45;
                float right = 550;

                // Header
                write(content, "TAX INVOICE", 20, 45, 800);

                Business business = invoice.getBusiness();

                write(content,
                        business != null ? business.getName() : "",
                        16, left, 765);

                write(content,
                        business != null ? business.getAddress() : "",
                        10, left, 748);

                write(content,
                        "GSTIN: " +
                                (business != null ? business.getGstin() : ""),
                        10, left, 733);

                write(content,
                        "State: " +
                                (business != null ? business.getState() : ""),
                        10, left, 718);

                write(content,
                        "Invoice No: " + invoice.getInvoiceNumber(),
                        10, 400, 765);

                write(content,
                        "Date: " + invoice.getInvoiceDate(),
                        10, 400, 748);

                write(content,
                        "Status: " + invoice.getStatus(),
                        10, 400, 733);

                line(content, left, 700, right, 700);

                // Customer
                Customer customer = invoice.getCustomer();

                write(content, "BILL TO", 12, left, 675);

                write(content,
                        customer != null ? customer.getName() : "",
                        11, left, 657);

                write(content,
                        customer != null ? customer.getAddress() : "",
                        10, left, 642);

                write(content,
                        "GSTIN: " +
                                (customer != null ? customer.getGstin() : ""),
                        10, left, 627);

                write(content,
                        "State: " +
                                (customer != null ? customer.getState() : ""),
                        10, left, 612);

                // Table
                float y = 570;

                line(content, left, y + 10, right, y + 10);

                write(content, "Product", 10, 50, y);
                write(content, "HSN", 10, 210, y);
                write(content, "Qty", 10, 270, y);
                write(content, "Price", 10, 320, y);
                write(content, "GST", 10, 400, y);
                write(content, "Total", 10, 470, y);

                line(content, left, y - 10, right, y - 10);

                y -= 30;

                for (InvoiceItem item : invoice.getItems()) {

                    write(content, item.getProductName(), 9, 50, y);
                    write(content, item.getHsnCode(), 9, 210, y);
                    write(content, item.getQuantity().toString(), 9, 270, y);
                    write(content, format(item.getUnitPrice()), 9, 320, y);
                    write(content, item.getGstRate() + "%", 9, 400, y);
                    write(content, format(item.getTotalAmount()), 9, 470, y);

                    y -= 25;
                }

                // Totals
                y -= 10;

                line(content, 300, y + 10, right, y + 10);

                write(content, "Taxable Amount", 10, 320, y);
                write(content, format(invoice.getTaxableAmount()), 10, 470, y);

                y -= 20;

                write(content, "CGST", 10, 320, y);
                write(content, format(invoice.getCgst()), 10, 470, y);

                y -= 20;

                write(content, "SGST", 10, 320, y);
                write(content, format(invoice.getSgst()), 10, 470, y);

                y -= 20;

                write(content, "IGST", 10, 320, y);
                write(content, format(invoice.getIgst()), 10, 470, y);

                y -= 25;

                line(content, 300, y + 10, right, y + 10);

                write(content, "GRAND TOTAL", 13, 320, y - 8);
                write(content, format(invoice.getGrandTotal()), 13, 470, y - 8);

                // Footer
                write(content,
                        "Thank you for your business",
                        9, 220, 55);
            }

            document.save(output);
            return output.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private void write(
            PDPageContentStream content,
            String text,
            int size,
            float x,
            float y
    ) throws IOException {

        content.beginText();
        content.setFont(
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                size
        );
        content.newLineAtOffset(x, y);
        content.showText(text != null ? text : "");
        content.endText();
    }

    private void line(
            PDPageContentStream content,
            float x1,
            float y1,
            float x2,
            float y2
    ) throws IOException {

        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private String format(BigDecimal amount) {

        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        return "Rs. " +
                amount.setScale(2, RoundingMode.HALF_UP);
    }
}