package com.gstbilling.gst_billing.util;

import com.gstbilling.gst_billing.entity.HsnSacType;

import java.util.regex.Pattern;

public final class HsnSacValidator {

    private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

    private HsnSacValidator() {}

    /**
     * Validates whether the code conforms to official format rules for HSN or SAC.
     */
    public static boolean isValidFormat(String code, HsnSacType type) {
        if (code == null || type == null) {
            return false;
        }
        String cleanCode = code.trim();
        if (!DIGITS_ONLY.matcher(cleanCode).matches()) {
            return false;
        }

        if (type == HsnSacType.HSN) {
            // Chapter 99 is reserved exclusively for Services (SAC)
            if (cleanCode.startsWith("99")) {
                return false;
            }
            int len = cleanCode.length();
            return len == 2 || len == 4 || len == 6 || len == 8;
        } else if (type == HsnSacType.SAC) {
            // SAC codes start with 99 and are 4, 5, or 6 digits in length
            if (!cleanCode.startsWith("99")) {
                return false;
            }
            int len = cleanCode.length();
            return len == 4 || len == 5 || len == 6;
        }
        return false;
    }

    /**
     * Determines the statutory classification level for a given code.
     */
    public static String determineClassificationLevel(String code, HsnSacType type) {
        if (!isValidFormat(code, type)) {
            return "UNKNOWN";
        }
        String cleanCode = code.trim();
        int len = cleanCode.length();

        if (type == HsnSacType.HSN) {
            return switch (len) {
                case 2 -> "2_DIGIT";
                case 4 -> "4_DIGIT";
                case 6 -> "6_DIGIT";
                case 8 -> "8_DIGIT";
                default -> "UNKNOWN";
            };
        } else {
            return switch (len) {
                case 4 -> "HEADING";
                case 5 -> "GROUP";
                case 6 -> "TARIFF";
                default -> "UNKNOWN";
            };
        }
    }
}
