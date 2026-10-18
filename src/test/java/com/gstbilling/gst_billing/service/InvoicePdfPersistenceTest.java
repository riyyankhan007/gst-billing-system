package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.InvoiceItem;
import com.gstbilling.gst_billing.storage.StorageException;
import com.gstbilling.gst_billing.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InvoicePdfPersistenceTest {

    private InvoiceService invoiceService;
    private StorageService storageService;
    private InvoicePdfService pdfService;

    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        invoiceService = mock(InvoiceService.class);
        storageService = mock(StorageService.class);
        pdfService = new InvoicePdfService(invoiceService, storageService);

        Business business = new Business();
        business.setId(10L);
        business.setName("Test Enterprise");
        business.setGstin("27AABCT1234F1Z5");
        business.setAddress("123 Commercial Blvd, Mumbai");
        business.setState("Maharashtra");
        business.setStateCode("27");

        Customer customer = new Customer();
        customer.setId(20L);
        customer.setName("Acme Corp");
        customer.setGstin("27XYZAB1234F1Z9");
        customer.setBillingAddress("456 Market St, Pune");
        customer.setState("Maharashtra");

        InvoiceItem item = new InvoiceItem();
        item.setProductName("Cloud Architecture Consulting");
        item.setHsnCode("998313");
        item.setQuantity(new BigDecimal("10"));
        item.setUnitPrice(new BigDecimal("2000.00"));
        item.setGstRate(new BigDecimal("18.00"));
        item.setTaxableAmount(new BigDecimal("20000.00"));
        item.setTaxAmount(new BigDecimal("3600.00"));
        item.setTotalAmount(new BigDecimal("23600.00"));

        testInvoice = new Invoice();
        testInvoice.setId(101L);
        testInvoice.setInvoiceNumber("INV/2026-27/101");
        testInvoice.setInvoiceDate(LocalDate.now());
        testInvoice.setStatus("ISSUED");
        testInvoice.setBusiness(business);
        testInvoice.setCustomer(customer);
        testInvoice.setItems(List.of(item));
        testInvoice.setTaxableAmount(new BigDecimal("20000.00"));
        testInvoice.setCgst(new BigDecimal("1800.00"));
        testInvoice.setSgst(new BigDecimal("1800.00"));
        testInvoice.setIgst(BigDecimal.ZERO);
        testInvoice.setTotalTax(new BigDecimal("3600.00"));
        testInvoice.setGrandTotal(new BigDecimal("23600.00"));
    }

    @Test
    @DisplayName("Finalized invoice PDF is persisted to persistent storage with tenant-isolated key")
    void testFinalizedInvoicePdfIsStoredInStorage() {
        when(invoiceService.getInvoiceById(101L)).thenReturn(testInvoice);
        String expectedKey = "invoices/10/101.pdf";
        when(storageService.exists(expectedKey)).thenReturn(false);

        byte[] pdfBytes = pdfService.generateInvoicePdf(101L);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // Verify persistent store was called with expected key and application/pdf
        verify(storageService, times(1)).store(eq(expectedKey), eq(pdfBytes), eq("application/pdf"));
    }

    @Test
    @DisplayName("Cached finalized PDF is retrieved from storage directly without regenerating")
    void testCachedInvoicePdfIsRetrievedDirectly() {
        when(invoiceService.getInvoiceById(101L)).thenReturn(testInvoice);
        String expectedKey = "invoices/10/101.pdf";
        byte[] preCachedBytes = "%PDF-1.4 pre-cached invoice data from R2".getBytes();

        when(storageService.exists(expectedKey)).thenReturn(true);
        when(storageService.retrieve(expectedKey)).thenReturn(preCachedBytes);

        byte[] resultBytes = pdfService.generateInvoicePdf(101L);

        assertArrayEquals(preCachedBytes, resultBytes);
        // Ensure no new store() was triggered because cache hit was returned
        verify(storageService, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("Failed R2 storage operation is not swallowed and throws StorageException")
    void testFailedStorageOperationThrowsStorageException() {
        when(invoiceService.getInvoiceById(101L)).thenReturn(testInvoice);
        String expectedKey = "invoices/10/101.pdf";
        when(storageService.exists(expectedKey)).thenReturn(false);
        when(storageService.store(eq(expectedKey), any(), eq("application/pdf")))
                .thenThrow(new StorageException("Cloudflare R2 PutObject timeout"));

        StorageException ex = assertThrows(StorageException.class, () ->
                pdfService.generateInvoicePdf(101L)
        );
        assertTrue(ex.getMessage().contains("Failed to persist invoice PDF"));
    }

    @Test
    @DisplayName("Draft invoice is never stored in persistent cache")
    void testDraftInvoiceIsNotStoredInCache() {
        testInvoice.setStatus("DRAFT");
        when(invoiceService.getInvoiceById(101L)).thenReturn(testInvoice);

        byte[] pdfBytes = pdfService.generateInvoicePdf(101L);

        assertNotNull(pdfBytes);
        verify(storageService, never()).store(any(), any(), any());
    }
}
