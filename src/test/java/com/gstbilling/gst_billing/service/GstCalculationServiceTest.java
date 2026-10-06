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
    @DisplayName("Intra-state transaction using state codes (e.g. 27 and Maharashtra) is recognized correctly")
    void testIntraStateGstWithStateCodes() {
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                1L, "Widget", "8471", BigDecimal.valueOf(1), BigDecimal.valueOf(1000),
                BigDecimal.valueOf(18), BigDecimal.ZERO, false
        );

        // Supplier has code "27", Customer has name "Maharashtra"
        GstCalculationService.GstResult result = service.calculate(
                "27", "Maharashtra", false, BigDecimal.ZERO, List.of(item)
        );

        assertTrue(result.intraState());
        assertEquals(0, new BigDecimal("90.00").compareTo(result.cgst()));
        assertEquals(0, new BigDecimal("90.00").compareTo(result.sgst()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.igst()));
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
    @DisplayName("User iPhone Example: ₹164,999 x 2 @ 18% tax-inclusive reverse calculates accurately with IGST")
    void testTaxInclusiveIPhoneExample() {
        // Product: iPhone, Unit price: ₹164,999, Quantity: 2, GST: 18%, Price is Tax-Inclusive
        // Gross: ₹329,998.00
        // Taxable: ₹329,998 / 1.18 = ₹279,659.32
        // IGST @ 18%: ₹50,338.68
        // Grand Total: ₹329,998.00
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                100L, "iPhone", "8517", BigDecimal.valueOf(2), new BigDecimal("164999.00"),
                BigDecimal.valueOf(18), BigDecimal.ZERO, true
        );

        GstCalculationService.GstResult result = service.calculate(
                "Maharashtra", "Karnataka", false, BigDecimal.ZERO, List.of(item)
        );

        assertFalse(result.intraState());
        assertEquals(0, new BigDecimal("279659.32").compareTo(result.taxableAmount()));
        assertEquals(0, new BigDecimal("50338.68").compareTo(result.igst()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.cgst()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.sgst()));
        assertEquals(0, new BigDecimal("50338.68").compareTo(result.totalTax()));
        assertEquals(0, new BigDecimal("329998.00").compareTo(result.grandTotal()));

        // Also verify line item outputs
        GstCalculationService.ItemOutput lineOut = result.items().get(0);
        assertEquals(0, new BigDecimal("279659.32").compareTo(lineOut.taxableAmount()));
        assertEquals(0, new BigDecimal("50338.68").compareTo(lineOut.taxAmount()));
        assertEquals(0, new BigDecimal("329998.00").compareTo(lineOut.totalAmount()));
    }

    @Test
    @DisplayName("User iPhone Example: ₹164,999 x 2 @ 18% intra-state splits equally into CGST and SGST")
    void testTaxInclusiveIntraStateCgstSgstSplit() {
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                100L, "iPhone", "8517", BigDecimal.valueOf(2), new BigDecimal("164999.00"),
                BigDecimal.valueOf(18), BigDecimal.ZERO, true
        );

        GstCalculationService.GstResult result = service.calculate(
                "Maharashtra", "Maharashtra", false, BigDecimal.ZERO, List.of(item)
        );

        assertTrue(result.intraState());
        assertEquals(0, new BigDecimal("279659.32").compareTo(result.taxableAmount()));
        assertEquals(0, new BigDecimal("25169.34").compareTo(result.cgst()));
        assertEquals(0, new BigDecimal("25169.34").compareTo(result.sgst()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.igst()));
        assertEquals(0, new BigDecimal("50338.68").compareTo(result.totalTax()));
        assertEquals(0, new BigDecimal("329998.00").compareTo(result.grandTotal()));
    }

    @Test
    @DisplayName("Tax-inclusive pricing across standard GST rates: 0%, 5%, 12%, 18%, 28%")
    void testTaxInclusiveDifferentGstRates() {
        // Rate 0%: ₹500 x 2 = ₹1000 -> Taxable ₹1000, Tax ₹0
        GstCalculationService.ItemInput item0 = new GstCalculationService.ItemInput(
                1L, "Grains", "1001", BigDecimal.valueOf(2), new BigDecimal("500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, true
        );
        // Rate 5%: ₹105 x 10 = ₹1050 -> Taxable 1050 / 1.05 = ₹1000, Tax ₹50
        GstCalculationService.ItemInput item5 = new GstCalculationService.ItemInput(
                2L, "Sugar", "1701", BigDecimal.valueOf(10), new BigDecimal("105.00"),
                BigDecimal.valueOf(5), BigDecimal.ZERO, true
        );
        // Rate 12%: ₹1120 x 1 = ₹1120 -> Taxable 1120 / 1.12 = ₹1000, Tax ₹120
        GstCalculationService.ItemInput item12 = new GstCalculationService.ItemInput(
                3L, "Butter", "0405", BigDecimal.ONE, new BigDecimal("1120.00"),
                BigDecimal.valueOf(12), BigDecimal.ZERO, true
        );
        // Rate 28%: ₹1280 x 1 = ₹1280 -> Taxable 1280 / 1.28 = ₹1000, Tax ₹280
        GstCalculationService.ItemInput item28 = new GstCalculationService.ItemInput(
                4L, "Air Conditioner", "8415", BigDecimal.ONE, new BigDecimal("1280.00"),
                BigDecimal.valueOf(28), BigDecimal.ZERO, true
        );

        GstCalculationService.GstResult result = service.calculate(
                "Delhi", "Haryana", false, BigDecimal.ZERO, List.of(item0, item5, item12, item28)
        );

        // Sum taxable = 1000 + 1000 + 1000 + 1000 = 4000
        // Sum tax = 0 + 50 + 120 + 280 = 450
        // Grand total = 4450
        assertEquals(0, new BigDecimal("4000.00").compareTo(result.taxableAmount()));
        assertEquals(0, new BigDecimal("450.00").compareTo(result.igst()));
        assertEquals(0, new BigDecimal("450.00").compareTo(result.totalTax()));
        assertEquals(0, new BigDecimal("4450.00").compareTo(result.grandTotal()));
    }

    @Test
    @DisplayName("Tax-inclusive pricing with item discount reverse calculates from net discounted gross")
    void testTaxInclusiveWithItemDiscount() {
        // Price ₹1180, Qty 2 = Gross ₹2360. Discount = ₹360 -> Net ₹2000
        // Taxable = 2000 / 1.18 = ₹1694.92, Tax = 2000 - 1694.92 = ₹305.08
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                1L, "Tablet", "8471", BigDecimal.valueOf(2), new BigDecimal("1180.00"),
                BigDecimal.valueOf(18), new BigDecimal("360.00"), true
        );

        GstCalculationService.GstResult result = service.calculate(
                "Delhi", "Delhi", false, BigDecimal.ZERO, List.of(item)
        );

        assertEquals(0, new BigDecimal("1694.92").compareTo(result.taxableAmount()));
        assertEquals(0, new BigDecimal("305.08").compareTo(result.totalTax()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(result.grandTotal()));
    }

    @Test
    @DisplayName("Mixed invoice: Combining tax-inclusive and tax-exclusive items in the same invoice")
    void testMixedInvoiceTaxInclusiveAndExclusive() {
        // Item 1 (Inclusive): ₹1180 @ 18% -> Taxable ₹1000.00, Tax ₹180.00, Total ₹1180.00
        GstCalculationService.ItemInput itemIncl = new GstCalculationService.ItemInput(
                1L, "Inclusive Item", "8471", BigDecimal.ONE, new BigDecimal("1180.00"),
                BigDecimal.valueOf(18), BigDecimal.ZERO, true
        );
        // Item 2 (Exclusive): ₹1000 @ 18% -> Taxable ₹1000.00, Tax ₹180.00, Total ₹1180.00
        GstCalculationService.ItemInput itemExcl = new GstCalculationService.ItemInput(
                2L, "Exclusive Item", "8471", BigDecimal.ONE, new BigDecimal("1000.00"),
                BigDecimal.valueOf(18), BigDecimal.ZERO, false
        );

        GstCalculationService.GstResult result = service.calculate(
                "Maharashtra", "Karnataka", false, BigDecimal.ZERO, List.of(itemIncl, itemExcl)
        );

        assertEquals(0, new BigDecimal("2000.00").compareTo(result.taxableAmount()));
        assertEquals(0, new BigDecimal("360.00").compareTo(result.igst()));
        assertEquals(0, new BigDecimal("360.00").compareTo(result.totalTax()));
        assertEquals(0, new BigDecimal("2360.00").compareTo(result.grandTotal()));
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

        assertNotNull(result.amountInWords());
        assertTrue(result.amountInWords().contains("Rupees"));
    }

    @Test
    @DisplayName("Reverse Charge Mechanism (RCM): Tax calculated but grandTotal equals taxableAmount")
    void testReverseChargeMechanism() {
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                1L, "Legal Consulting", "9982", BigDecimal.ONE, BigDecimal.valueOf(50000),
                BigDecimal.valueOf(18), BigDecimal.ZERO, false
        );

        // isExport = false, reverseCharge = true
        GstCalculationService.GstResult result = service.calculate(
                "Maharashtra", "Maharashtra", false, true, BigDecimal.ZERO, List.of(item)
        );

        assertTrue(result.reverseCharge());
        assertEquals(0, new BigDecimal("50000.00").compareTo(result.taxableAmount()));
        assertEquals(0, new BigDecimal("4500.00").compareTo(result.cgst()));
        assertEquals(0, new BigDecimal("4500.00").compareTo(result.sgst()));
        assertEquals(0, new BigDecimal("9000.00").compareTo(result.totalTax()));
        // Payable to supplier is ONLY taxable amount under RCM
        assertEquals(0, new BigDecimal("50000.00").compareTo(result.grandTotal()));
    }

    @Test
    @DisplayName("Round-off adjustment accurately computes nearest integer rupee")
    void testRoundOffAdjustment() {
        // Line total that results in fractional paise
        GstCalculationService.ItemInput item = new GstCalculationService.ItemInput(
                1L, "Odd Item", "8471", BigDecimal.valueOf(1), BigDecimal.valueOf(100.35),
                BigDecimal.valueOf(18), BigDecimal.ZERO, false
        );

        GstCalculationService.GstResult result = service.calculate(
                "Karnataka", "Karnataka", false, false, BigDecimal.ZERO, List.of(item)
        );

        // Taxable 100.35 + 18% tax 18.06 = 118.41
        // Rounded grand total = 118.00 -> round-off = -0.41
        assertEquals(0, new BigDecimal("118.00").compareTo(result.grandTotal()));
        assertEquals(0, new BigDecimal("-0.41").compareTo(result.roundOffAmount()));
        assertEquals(0, result.taxableAmount().add(result.totalTax()).add(result.roundOffAmount()).compareTo(result.grandTotal()),
                "Taxable amount + total tax + round-off must equal grand total exactly");
    }

    @Test
    @DisplayName("Input validation rejects invalid item quantities, prices, or rates")
    void testInputValidationRejections() {
        // Zero or negative quantity
        assertThrows(IllegalArgumentException.class, () ->
                service.calculate("Delhi", "Delhi", false, BigDecimal.ZERO, List.of(
                        new GstCalculationService.ItemInput(1L, "A", "1234", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.valueOf(18), BigDecimal.ZERO, false)
                ))
        );

        // Negative unit price
        assertThrows(IllegalArgumentException.class, () ->
                service.calculate("Delhi", "Delhi", false, BigDecimal.ZERO, List.of(
                        new GstCalculationService.ItemInput(1L, "A", "1234", BigDecimal.ONE, BigDecimal.valueOf(-10), BigDecimal.valueOf(18), BigDecimal.ZERO, false)
                ))
        );

        // Negative GST rate
        assertThrows(IllegalArgumentException.class, () ->
                service.calculate("Delhi", "Delhi", false, BigDecimal.ZERO, List.of(
                        new GstCalculationService.ItemInput(1L, "A", "1234", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(-5), BigDecimal.ZERO, false)
                ))
        );

        // Negative discount
        assertThrows(IllegalArgumentException.class, () ->
                service.calculate("Delhi", "Delhi", false, BigDecimal.ZERO, List.of(
                        new GstCalculationService.ItemInput(1L, "A", "1234", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(18), BigDecimal.valueOf(-20), false)
                ))
        );

        // Empty items list
        assertThrows(IllegalArgumentException.class, () ->
                service.calculate("Delhi", "Delhi", false, BigDecimal.ZERO, List.of())
        );
    }
}
