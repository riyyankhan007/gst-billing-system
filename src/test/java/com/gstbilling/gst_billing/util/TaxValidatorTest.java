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
}
