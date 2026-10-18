package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.HsnSacType;
import com.gstbilling.gst_billing.util.HsnSacValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class HsnSacValidationTest {

    @Test
    @DisplayName("HSN preserves leading zeros for statutory classifications")
    void testLeadingZeroPreservation() {
        assertTrue(HsnSacValidator.isValidFormat("0401", HsnSacType.HSN));
        assertTrue(HsnSacValidator.isValidFormat("0101", HsnSacType.HSN));
        assertTrue(HsnSacValidator.isValidFormat("0901", HsnSacType.HSN));
        assertEquals("4_DIGIT", HsnSacValidator.determineClassificationLevel("0401", HsnSacType.HSN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"84", "8471", "847130", "84713010", "04", "0401", "85171300"})
    @DisplayName("Valid HSN formats: 2, 4, 6, and 8 digits")
    void testValidHsnFormats(String code) {
        assertTrue(HsnSacValidator.isValidFormat(code, HsnSacType.HSN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "123", "12345", "1234567", "123456789", "8471A", "HSN-84", ""})
    @DisplayName("Invalid HSN formats: odd digit lengths or non-numeric characters")
    void testInvalidHsnFormats(String code) {
        assertFalse(HsnSacValidator.isValidFormat(code, HsnSacType.HSN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"9983", "99831", "998311", "9982", "998222", "9965"})
    @DisplayName("Valid SAC formats: starting with 99, lengths 4, 5, or 6")
    void testValidSacFormats(String code) {
        assertTrue(HsnSacValidator.isValidFormat(code, HsnSacType.SAC));
    }

    @ParameterizedTest
    @ValueSource(strings = {"8471", "9883", "99", "998", "9983119", "SAC9983", ""})
    @DisplayName("Invalid SAC formats: not starting with 99 or invalid length")
    void testInvalidSacFormats(String code) {
        assertFalse(HsnSacValidator.isValidFormat(code, HsnSacType.SAC));
    }

    @Test
    @DisplayName("Correct classification levels determined for statutory codes")
    void testClassificationLevels() {
        assertEquals("2_DIGIT", HsnSacValidator.determineClassificationLevel("84", HsnSacType.HSN));
        assertEquals("4_DIGIT", HsnSacValidator.determineClassificationLevel("8471", HsnSacType.HSN));
        assertEquals("6_DIGIT", HsnSacValidator.determineClassificationLevel("847130", HsnSacType.HSN));
        assertEquals("8_DIGIT", HsnSacValidator.determineClassificationLevel("84713010", HsnSacType.HSN));

        assertEquals("HEADING", HsnSacValidator.determineClassificationLevel("9983", HsnSacType.SAC));
        assertEquals("GROUP", HsnSacValidator.determineClassificationLevel("99831", HsnSacType.SAC));
        assertEquals("TARIFF", HsnSacValidator.determineClassificationLevel("998311", HsnSacType.SAC));

        assertEquals("UNKNOWN", HsnSacValidator.determineClassificationLevel("INVALID", HsnSacType.HSN));
    }
}
