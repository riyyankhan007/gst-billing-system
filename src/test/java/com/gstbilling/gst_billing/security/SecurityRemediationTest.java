package com.gstbilling.gst_billing.security;

import com.gstbilling.gst_billing.dto.RegisterRequest;
import com.gstbilling.gst_billing.util.EncryptedStringConverter;
import com.gstbilling.gst_billing.util.IndianTaxValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SecurityRemediationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Password complexity should reject weak passwords during registration")
    void testPasswordComplexityValidation() {
        // Password too short (< 8 chars)
        RegisterRequest shortPwd = new RegisterRequest("Test User", "test@example.com", "Aa1!", "Biz");
        assertFalse(validator.validate(shortPwd).isEmpty(), "Password shorter than 8 chars must fail");

        // Password missing uppercase
        RegisterRequest noUpper = new RegisterRequest("Test User", "test@example.com", "password123", "Biz");
        assertFalse(validator.validate(noUpper).isEmpty(), "Password missing uppercase must fail");

        // Password missing number
        RegisterRequest noDigit = new RegisterRequest("Test User", "test@example.com", "PasswordSecure", "Biz");
        assertFalse(validator.validate(noDigit).isEmpty(), "Password missing number must fail");

        // Valid strong password
        RegisterRequest valid = new RegisterRequest("Test User", "test@example.com", "Password@123", "Biz");
        assertTrue(validator.validate(valid).isEmpty(), "Strong password must pass validation");
    }

    @Test
    @DisplayName("AuthRateLimitingFilter should throttle IP after exceeding request limit")
    void testAuthRateLimitingFilter() throws Exception {
        AuthRateLimitingFilter filter = new AuthRateLimitingFilter();

        // 30 requests should succeed (200 OK)
        for (int i = 0; i < 30; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("198.51.100.5");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }

        // 31st request from same IP should receive HTTP 429 Too Many Requests
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setRemoteAddr("198.51.100.5");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilter(blockedReq, blockedRes, new MockFilterChain());

        assertEquals(429, blockedRes.getStatus());
        assertTrue(blockedRes.getContentAsString().contains("Too Many Requests"));
    }

    @Test
    @DisplayName("EncryptedStringConverter should encrypt with AES-GCM and decrypt accurately")
    void testEncryptedStringConverter() {
        EncryptedStringConverter converter = new EncryptedStringConverter("my-test-aes-key-for-bank-secrets-32b!", "");

        String rawBankAccount = "9876543210123456";
        String encrypted = converter.convertToDatabaseColumn(rawBankAccount);

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC:"));
        assertNotEquals(rawBankAccount, encrypted);

        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertEquals(rawBankAccount, decrypted);

        // Backwards compatibility with legacy unencrypted plaintext:
        String legacyPlain = "123456789";
        assertEquals("123456789", converter.convertToEntityAttribute(legacyPlain));
    }

    @Test
    @DisplayName("IndianTaxValidator should validate standard GST rate slabs")
    void testGstSlabValidation() {
        assertTrue(IndianTaxValidator.isValidGstRate(BigDecimal.ZERO));
        assertTrue(IndianTaxValidator.isValidGstRate(new BigDecimal("5.0")));
        assertTrue(IndianTaxValidator.isValidGstRate(new BigDecimal("12.00")));
        assertTrue(IndianTaxValidator.isValidGstRate(new BigDecimal("18")));
        assertTrue(IndianTaxValidator.isValidGstRate(new BigDecimal("28.0")));
        assertTrue(IndianTaxValidator.isValidGstRate(new BigDecimal("0.25")));

        // Invalid rates
        assertFalse(IndianTaxValidator.isValidGstRate(new BigDecimal("7.5")));
        assertFalse(IndianTaxValidator.isValidGstRate(new BigDecimal("-5")));
        assertFalse(IndianTaxValidator.isValidGstRate(new BigDecimal("42")));
        assertFalse(IndianTaxValidator.isValidGstRate(null));
    }
}
