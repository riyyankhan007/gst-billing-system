package com.gstbilling.gst_billing.util;

import java.util.regex.Pattern;

public final class IndianTaxValidator {

    private IndianTaxValidator() {}

    // 15 alphanumeric characters: 2 digits state code + 10 chars PAN + 1 entity num + 1 'Z' + 1 check digit
    private static final Pattern GSTIN_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    // 10 alphanumeric characters: 5 letters + 4 digits + 1 letter
    private static final Pattern PAN_PATTERN =
            Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");

    // 11 characters: 4 letters + '0' + 6 alphanumeric
    private static final Pattern IFSC_PATTERN =
            Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    public static boolean isValidGstin(String gstin) {
        if (gstin == null || gstin.trim().isEmpty()) {
            return false;
        }
        return GSTIN_PATTERN.matcher(gstin.trim().toUpperCase()).matches();
    }

    public static boolean isValidPan(String pan) {
        if (pan == null || pan.trim().isEmpty()) {
            return false;
        }
        return PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches();
    }

    public static boolean isValidIfsc(String ifsc) {
        if (ifsc == null || ifsc.trim().isEmpty()) {
            return false;
        }
        return IFSC_PATTERN.matcher(ifsc.trim().toUpperCase()).matches();
    }

    public static String extractPanFromGstin(String gstin) {
        if (gstin != null && gstin.trim().length() >= 12) {
            String clean = gstin.trim().toUpperCase();
            return clean.substring(2, 12);
        }
        return null;
    }
}
