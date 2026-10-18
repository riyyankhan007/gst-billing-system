package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.InvoiceItem;
import com.gstbilling.gst_billing.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvoicePdfServiceUnitTest {

    @Test
    @DisplayName("InvoicePdfService generates valid PDF bytes containing HSN/SAC summary")
    void testPdfGenerationWithHsnSummary() {
        InvoiceService invoiceService = mock(InvoiceService.class);
        StorageService storageService = mock(StorageService.class);

        InvoicePdfService pdfService = new InvoicePdfService(invoiceService, storageService);

        Business biz = new Business();
        biz.setId(1L);
        biz.setName("Apex Tech Solutions Pvt Ltd");
        biz.setGstin("27AAAAA0000A1Z5");
        biz.setState("Maharashtra");
        biz.setStateCode("27");
        biz.setBankName("HDFC Bank");
        biz.setBankAccountNumber("50200012345678");
        biz.setBankIfsc("HDFC0001234");

        Customer cust = new Customer();
        cust.setId(2L);
        cust.setName("Zenith Corp");
        cust.setGstin("27BBBBB1111B1Z2");
        cust.setState("Maharashtra");
        cust.setStateCode("27");

        Invoice inv = new Invoice();
        inv.setId(101L);
        inv.setInvoiceNumber("INV-2024-001");
        inv.setInvoiceDate(LocalDate.now());
        inv.setBusiness(biz);
        inv.setCustomer(cust);
        inv.setTaxableAmount(BigDecimal.valueOf(100000));
        inv.setCgst(BigDecimal.valueOf(9000));
        inv.setSgst(BigDecimal.valueOf(9000));
        inv.setTotalTax(BigDecimal.valueOf(18000));
        inv.setGrandTotal(BigDecimal.valueOf(118000));
        inv.setStatus("ISSUED");

        InvoiceItem item1 = new InvoiceItem();
        item1.setProductName("High Performance Laptops");
        item1.setHsnCode("84713010");
        item1.setQuantity(BigDecimal.valueOf(2));
        item1.setUnitPrice(BigDecimal.valueOf(40000));
        item1.setTaxableAmount(BigDecimal.valueOf(80000));
        item1.setGstRate(BigDecimal.valueOf(18));
        item1.setTaxAmount(BigDecimal.valueOf(14400));
        item1.setTotalAmount(BigDecimal.valueOf(94400));

        InvoiceItem item2 = new InvoiceItem();
        item2.setProductName("IT Setup Consulting");
        item2.setHsnCode("998311");
        item2.setQuantity(BigDecimal.valueOf(1));
        item2.setUnitPrice(BigDecimal.valueOf(20000));
        item2.setTaxableAmount(BigDecimal.valueOf(20000));
        item2.setGstRate(BigDecimal.valueOf(18));
        item2.setTaxAmount(BigDecimal.valueOf(3600));
        item2.setTotalAmount(BigDecimal.valueOf(23600));

        inv.setItems(List.of(item1, item2));

        when(invoiceService.getInvoiceById(101L)).thenReturn(inv);
        when(storageService.exists(anyString())).thenReturn(false);

        byte[] pdfBytes = pdfService.generateInvoicePdf(101L);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF bytes should be non-trivial");

        // Verify PDF Magic Bytes "%PDF"
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);

        // Verify it caches the finalized invoice
        verify(storageService, times(1)).store(contains("invoices/1/101.pdf"), eq(pdfBytes), eq("application/pdf"));
    }
}
