package com.gstbilling.gst_billing.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaxValidatorTest {

    @Test
    @DisplayName("Valid GSTIN validation passes")
    void testValidGstin() {
        assertTrue(IndianTaxValidator.isValidGstin("27AAAAA0000A1Z5"));
        assertTrue(IndianTaxValidator.isValidGstin("29ABCDE1234F1Z5"));
        assertTrue(IndianTaxValidator.isValidGstin("07ABCDE1234F2ZA"));
    }

    @Test
    @DisplayName("Invalid GSTIN fails validation")
    void testInvalidGstin() {
        assertFalse(IndianTaxValidator.isValidGstin(null));
        assertFalse(IndianTaxValidator.isValidGstin(""));
        assertFalse(IndianTaxValidator.isValidGstin("SHORT"));
        assertFalse(IndianTaxValidator.isValidGstin("27AAAAA0000A1Z!")); // Special char
        assertFalse(IndianTaxValidator.isValidGstin("27AAAAA0000A195")); // Missing 'Z' as 14th character
        assertFalse(IndianTaxValidator.isValidGstin("27AAAAA0000A1Z"));  // 14 chars instead of 15
        assertFalse(IndianTaxValidator.isValidGstin("98AAAAA0000A1Z5")); // Invalid state code 98
    }

    @Test
    @DisplayName("Mod-36 Luhn-variant GSTIN checksum calculation and validation")
    void testGstinChecksumCalculation() {
        String base14 = "27AAPFU0939F1Z";
        char checkChar = IndianTaxValidator.calculateGstinChecksum(base14);
        assertNotNull(Character.valueOf(checkChar));

        String completeGstin = base14 + checkChar;
        assertTrue(IndianTaxValidator.isValidGstinChecksum(completeGstin));

        // Corrupted checksum must fail
        char wrongChar = (checkChar == 'A') ? 'B' : 'A';
        assertFalse(IndianTaxValidator.isValidGstinChecksum(base14 + wrongChar));
    }

    @Test
    @DisplayName("State codes and intra-state detection")
    void testStateCodesAndIntraState() {
        assertTrue(IndianTaxValidator.isValidStateCode("27"));
        assertTrue(IndianTaxValidator.isValidStateCode("07"));
        assertFalse(IndianTaxValidator.isValidStateCode("98"));

        assertEquals("Maharashtra", IndianTaxValidator.getStateNameByCode("27"));
        assertEquals("Delhi", IndianTaxValidator.getStateNameByCode("07"));
        assertEquals("27", IndianTaxValidator.getStateCodeByName("Maharashtra"));

        // Comparing same code / names
        assertTrue(IndianTaxValidator.isIntraState("Maharashtra", "Maharashtra"));
        assertTrue(IndianTaxValidator.isIntraState("27", "27"));
        assertTrue(IndianTaxValidator.isIntraState("27", "Maharashtra"));
        assertTrue(IndianTaxValidator.isIntraState("Maharashtra", "27"));

        // Inter-state
        assertFalse(IndianTaxValidator.isIntraState("27", "29"));
        assertFalse(IndianTaxValidator.isIntraState("Maharashtra", "Karnataka"));
    }

    @Test
    @DisplayName("HSN / SAC Code validation (4, 6, 8 digits)")
    void testHsnValidation() {
        assertTrue(IndianTaxValidator.isValidHsn("8471"));       // 4-digit
        assertTrue(IndianTaxValidator.isValidHsn("847130"));     // 6-digit
        assertTrue(IndianTaxValidator.isValidHsn("84713010"));   // 8-digit
        assertTrue(IndianTaxValidator.isValidHsn("998311"));     // 6-digit SAC

        assertFalse(IndianTaxValidator.isValidHsn("847"));       // 3-digit invalid
        assertFalse(IndianTaxValidator.isValidHsn("84713"));     // 5-digit invalid
        assertFalse(IndianTaxValidator.isValidHsn("8471301"));   // 7-digit invalid
        assertFalse(IndianTaxValidator.isValidHsn("847130109")); // 9-digit invalid
        assertFalse(IndianTaxValidator.isValidHsn("ABCD"));      // Non-numeric invalid
        assertFalse(IndianTaxValidator.isValidHsn(""));
        assertFalse(IndianTaxValidator.isValidHsn(null));
    }

    @Test
    @DisplayName("Valid PAN validation and PAN extraction from GSTIN")
    void testPanValidation() {
        assertTrue(IndianTaxValidator.isValidPan("AAAAA0000A"));
        assertTrue(IndianTaxValidator.isValidPan("ABCDE1234F"));
        assertFalse(IndianTaxValidator.isValidPan("12345ABCDE"));
        assertFalse(IndianTaxValidator.isValidPan("ABCDE12345"));

        assertEquals("AAAAA0000A", IndianTaxValidator.extractPanFromGstin("27AAAAA0000A1Z5"));
        assertEquals("ABCDE1234F", IndianTaxValidator.extractPanFromGstin("29ABCDE1234F1Z5"));
    }

    @Test
    @DisplayName("IFSC Code validation")
    void testIfscValidation() {
        assertTrue(IndianTaxValidator.isValidIfsc("SBIN0001234"));
        assertTrue(IndianTaxValidator.isValidIfsc("HDFC0000123"));
        assertFalse(IndianTaxValidator.isValidIfsc("SBIN1001234")); // 5th char must be 0
        assertFalse(IndianTaxValidator.isValidIfsc("SHORT"));
    }

    @Test
    @DisplayName("Number to Indian currency words conversion")
    void testNumberToWords() {
        assertEquals("Rupees Ten Thousand Only", NumberToWordsConverter.convertToIndianCurrencyWords(new java.math.BigDecimal("10000")));
        assertEquals("Rupees One Lakh Twenty Five Thousand Four Hundred Fifty and Fifty Paise Only",
                NumberToWordsConverter.convertToIndianCurrencyWords(new java.math.BigDecimal("125450.50")));
        assertEquals("Rupees Zero Only", NumberToWordsConverter.convertToIndianCurrencyWords(java.math.BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Financial Year calculation utility (April 1 to March 31)")
    void testFinancialYearUtil() {
        assertEquals("2026-27", FinancialYearUtil.getFinancialYear(java.time.LocalDate.of(2026, 4, 1)));
        assertEquals("2026-27", FinancialYearUtil.getFinancialYear(java.time.LocalDate.of(2026, 10, 5)));
        assertEquals("2026-27", FinancialYearUtil.getFinancialYear(java.time.LocalDate.of(2027, 3, 31)));
        assertEquals("2025-26", FinancialYearUtil.getFinancialYear(java.time.LocalDate.of(2026, 3, 31)));
        assertEquals("2025-26", FinancialYearUtil.getFinancialYear(java.time.LocalDate.of(2026, 1, 15)));
        assertEquals("2027-28", FinancialYearUtil.getFinancialYear(java.time.LocalDate.of(2027, 4, 1)));
    }
}
