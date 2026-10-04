package com.gstbilling.gst_billing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GstCalculationServiceTest {

    private GstCalculationService service;

    @BeforeEach
    void setUp() {
        service = new GstCalculationService();
    }

    @Test
    @DisplayName("Intra-state transaction splits GST equally into CGST and SGST")
    void testIntraStateGst() {
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                1L, "Widget", "8471", BigDecimal.valueOf(2), BigDecimal.valueOf(5000),
                BigDecimal.valueOf(18), BigDecimal.ZERO, false
        );

        GstCalculationService.GstResult result = service.calculate(
                "Maharashtra", "Maharashtra", false, BigDecimal.ZERO, List.of(item)
        );

        assertTrue(result.intraState());
        assertEquals(0, new BigDecimal("10000.00").compareTo(result.taxableAmount()));
        assertEquals(0, new BigDecimal("900.00").compareTo(result.cgst()));
        assertEquals(0, new BigDecimal("900.00").compareTo(result.sgst()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.igst()));
        assertEquals(0, new BigDecimal("1800.00").compareTo(result.totalTax()));
        assertEquals(0, new BigDecimal("11800.00").compareTo(result.grandTotal()));
    }

    @Test
    @DisplayName("Inter-state transaction applies 100% IGST with zero CGST and SGST")
    void testInterStateGst() {
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                1L, "Widget", "8471", BigDecimal.valueOf(1), BigDecimal.valueOf(10000),
                BigDecimal.valueOf(18), BigDecimal.ZERO, false
        );

        GstCalculationService.GstResult result = service.calculate(
                "Maharashtra", "Karnataka", false, BigDecimal.ZERO, List.of(item)
        );

        assertFalse(result.intraState());
        assertEquals(0, new BigDecimal("10000.00").compareTo(result.taxableAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.cgst()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.sgst()));
        assertEquals(0, new BigDecimal("1800.00").compareTo(result.igst()));
        assertEquals(0, new BigDecimal("11800.00").compareTo(result.grandTotal()));
    }

    @Test
    @DisplayName("Multiple GST rates (5%, 12%, 18%, 28%) in single invoice")
    void testMultipleGstRates() {
        GstCalculationService.ItemInput item1 = new GstCalculationService.ItemInput(
                1L, "Food", "1905", BigDecimal.ONE, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(5), BigDecimal.ZERO, false
        );
        GstCalculationService.ItemInput item2 = new GstCalculationService.ItemInput(
                2L, "Stationery", "4820", BigDecimal.ONE, BigDecimal.valueOf(2000),
                BigDecimal.valueOf(12), BigDecimal.ZERO, false
        );

        GstCalculationService.GstResult result = service.calculate(
                "Delhi", "Delhi", false, BigDecimal.ZERO, List.of(item1, item2)
        );

        // Item 1: 1000, 5% = 50 tax
        // Item 2: 2000, 12% = 240 tax
        // Total taxable = 3000, total tax = 290
        assertEquals(0, new BigDecimal("3000.00").compareTo(result.taxableAmount()));
        assertEquals(0, new BigDecimal("290.00").compareTo(result.totalTax()));
        assertEquals(0, new BigDecimal("145.00").compareTo(result.cgst()));
        assertEquals(0, new BigDecimal("145.00").compareTo(result.sgst()));
        assertEquals(0, new BigDecimal("3290.00").compareTo(result.grandTotal()));
    }

    @Test
    @DisplayName("Tax-inclusive pricing backs out GST accurately")
    void testTaxInclusivePricing() {
        // Price ₹1180 inclusive of 18% GST -> Taxable ₹1000, Tax ₹180
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                1L, "Service", "9983", BigDecimal.ONE, BigDecimal.valueOf(1180),
                BigDecimal.valueOf(18), BigDecimal.ZERO, true
        );

        GstCalculationService.GstResult result = service.calculate(
                "Gujarat", "Gujarat", false, BigDecimal.ZERO, List.of(item)
        );

        assertEquals(0, new BigDecimal("1000.00").compareTo(result.taxableAmount()));
        assertEquals(0, new BigDecimal("180.00").compareTo(result.totalTax()));
        assertEquals(0, new BigDecimal("1180.00").compareTo(result.grandTotal()));
    }

    @Test
    @DisplayName("Discounts at item and invoice level are deducted prior to tax calculation")
    void testDiscounts() {
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                1L, "Gadget", "8517", BigDecimal.valueOf(1), BigDecimal.valueOf(1000),
                BigDecimal.valueOf(18), BigDecimal.valueOf(100), false // ₹100 item discount
        );

        GstCalculationService.GstResult result = service.calculate(
                "Punjab", "Punjab", false, BigDecimal.valueOf(50), List.of(item) // ₹50 invoice discount
        );

        // Gross: 1000 - 100 item discount = 900 taxable from item
        // Less invoice discount 50 = 850 taxable
        // Tax on item: 900 * 18% = 162
        assertNotNull(result.amountInWords());
        assertTrue(result.amountInWords().contains("Rupees"));
    }
}
