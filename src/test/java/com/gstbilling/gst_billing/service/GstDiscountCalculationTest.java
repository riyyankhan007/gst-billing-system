package com.gstbilling.gst_billing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GstDiscountCalculationTest {

    private GstCalculationService service;

    @BeforeEach
    void setUp() {
        service = new GstCalculationService();
    }

    @Test
    @DisplayName("Invoice-level discount is proportionally distributed across items with different GST rates prior to tax calculation")
    void testProportionalDiscountAcrossDifferentGstRates() {
        // Item 1: 10 units @ ₹100.00 = ₹1,000.00 (GST Rate: 5%)
        GstCalculationService.ItemInput item5 = new GstCalculationService.ItemInput(
                1L, "Sugar 5%", "1701",
                BigDecimal.valueOf(10), new BigDecimal("100.00"),
                BigDecimal.valueOf(5), BigDecimal.ZERO, false
        );

        // Item 2: 10 units @ ₹100.00 = ₹1,000.00 (GST Rate: 18%)
        GstCalculationService.ItemInput item18 = new GstCalculationService.ItemInput(
                2L, "Stationery 18%", "4820",
                BigDecimal.valueOf(10), new BigDecimal("100.00"),
                BigDecimal.valueOf(18), BigDecimal.ZERO, false
        );

        // Invoice Level Discount: ₹200.00 (Total pre-discount gross: ₹2,000.00 -> 10% discount)
        BigDecimal invoiceDiscount = new BigDecimal("200.00");

        // Intra-state (Maharashtra -> Maharashtra)
        GstCalculationService.GstResult result = service.calculate(
                "Maharashtra", "Maharashtra", false, invoiceDiscount, List.of(item5, item18)
        );

        // Verification:
        // Item 1 Allocated Discount: 200 * (1000 / 2000) = 100 -> Net Taxable = 900.00
        // Item 1 Tax (5%): 900 * 0.05 = 45.00
        // Item 2 Allocated Discount: 200 * (1000 / 2000) = 100 -> Net Taxable = 900.00
        // Item 2 Tax (18%): 900 * 0.18 = 162.00
        // Total Taxable = 1800.00
        // Total Tax = 45.00 + 162.00 = 207.00
        // CGST = 103.50, SGST = 103.50, IGST = 0.00
        // Grand Total = 1800.00 + 207.00 = 2007.00

        assertEquals(0, new BigDecimal("1800.00").compareTo(result.taxableAmount()), "Taxable amount must reflect invoice discount");
        assertEquals(0, new BigDecimal("207.00").compareTo(result.totalTax()), "Total tax must be computed on discounted taxable amount");
        assertEquals(0, new BigDecimal("103.50").compareTo(result.cgst()), "CGST must equal half of total tax");
        assertEquals(0, new BigDecimal("103.50").compareTo(result.sgst()), "SGST must equal half of total tax");
        assertEquals(0, BigDecimal.ZERO.compareTo(result.igst()), "IGST must be 0 for intra-state");
        assertEquals(0, new BigDecimal("2007.00").compareTo(result.grandTotal()), "Grand total must be 2007.00");
    }

    @Test
    @DisplayName("Combined item-level and invoice-level discounts distribute remaining discount accurately")
    void testCombinedItemAndInvoiceDiscounts() {
        // Item 1: Gross ₹1,000, Item Discount ₹200 -> Net ₹800 (Rate: 18%)
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                1L, "Laptop Sleeve", "4202",
                BigDecimal.ONE, new BigDecimal("1000.00"),
                BigDecimal.valueOf(18), new BigDecimal("200.00"), false
        );

        // Invoice Discount: ₹100
        // Final Taxable: ₹800 - ₹100 = ₹700.00
        // Tax (18%): ₹700 * 0.18 = ₹126.00
        // Grand Total = ₹700 + ₹126 = ₹826.00
        GstCalculationService.GstResult result = service.calculate(
                "Delhi", "Haryana", false, new BigDecimal("100.00"), List.of(item)
        );

        assertTrue(!result.intraState());
        assertEquals(0, new BigDecimal("700.00").compareTo(result.taxableAmount()));
        assertEquals(0, new BigDecimal("126.00").compareTo(result.igst()));
        assertEquals(0, new BigDecimal("126.00").compareTo(result.totalTax()));
        assertEquals(0, new BigDecimal("826.00").compareTo(result.grandTotal()));
    }
}
