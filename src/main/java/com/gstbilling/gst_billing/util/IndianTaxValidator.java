package com.gstbilling.gst_billing.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class IndianTaxValidator {

    private IndianTaxValidator() {}

    private static final String ALPHANUMERIC_CODE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // 15 alphanumeric characters: 2 digits state code + 10 chars PAN + 1 entity num + 1 'Z' + 1 check digit
    private static final Pattern GSTIN_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    // 10 alphanumeric characters: 5 letters + 4 digits + 1 letter
    private static final Pattern PAN_PATTERN =
            Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");

    // 11 characters: 4 letters + '0' + 6 alphanumeric
    private static final Pattern IFSC_PATTERN =
            Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    // HSN / SAC: 4, 6, or 8 digits
    private static final Pattern HSN_PATTERN =
            Pattern.compile("^[0-9]{4}([0-9]{2})?([0-9]{2})?$");

    private static final Map<String, String> STATE_CODE_TO_NAME;
    private static final Map<String, String> STATE_NAME_TO_CODE;

    static {
        Map<String, String> c2n = new HashMap<>();
        c2n.put("01", "Jammu and Kashmir");
        c2n.put("02", "Himachal Pradesh");
        c2n.put("03", "Punjab");
        c2n.put("04", "Chandigarh");
        c2n.put("05", "Uttarakhand");
        c2n.put("06", "Haryana");
        c2n.put("07", "Delhi");
        c2n.put("08", "Rajasthan");
        c2n.put("09", "Uttar Pradesh");
        c2n.put("10", "Bihar");
        c2n.put("11", "Sikkim");
        c2n.put("12", "Arunachal Pradesh");
        c2n.put("13", "Nagaland");
        c2n.put("14", "Manipur");
        c2n.put("15", "Mizoram");
        c2n.put("16", "Tripura");
        c2n.put("17", "Meghalaya");
        c2n.put("18", "Assam");
        c2n.put("19", "West Bengal");
        c2n.put("20", "Jharkhand");
        c2n.put("21", "Odisha");
        c2n.put("22", "Chhattisgarh");
        c2n.put("23", "Madhya Pradesh");
        c2n.put("24", "Gujarat");
        c2n.put("26", "Dadra and Nagar Haveli and Daman and Diu");
        c2n.put("27", "Maharashtra");
        c2n.put("29", "Karnataka");
        c2n.put("30", "Goa");
        c2n.put("31", "Lakshadweep");
        c2n.put("32", "Kerala");
        c2n.put("33", "Tamil Nadu");
        c2n.put("34", "Puducherry");
        c2n.put("35", "Andaman and Nicobar Islands");
        c2n.put("36", "Telangana");
        c2n.put("37", "Andhra Pradesh");
        c2n.put("38", "Ladakh");
        c2n.put("97", "Other Territory");
        c2n.put("99", "Centre Jurisdiction");

        STATE_CODE_TO_NAME = Collections.unmodifiableMap(c2n);

        Map<String, String> n2c = new HashMap<>();
        for (Map.Entry<String, String> entry : c2n.entrySet()) {
            n2c.put(entry.getValue().toLowerCase().replaceAll("[^a-z0-9]", ""), entry.getKey());
        }
        STATE_NAME_TO_CODE = Collections.unmodifiableMap(n2c);
    }

    public static boolean isValidStateCode(String stateCode) {
        if (stateCode == null || stateCode.trim().length() != 2) {
            return false;
        }
        return STATE_CODE_TO_NAME.containsKey(stateCode.trim());
    }

    public static String getStateNameByCode(String stateCode) {
        if (stateCode == null) return null;
        return STATE_CODE_TO_NAME.get(stateCode.trim());
    }

    public static String getStateCodeByName(String stateName) {
        if (stateName == null) return null;
        String clean = stateName.toLowerCase().replaceAll("[^a-z0-9]", "");
        return STATE_NAME_TO_CODE.get(clean);
    }

    public static boolean isIntraState(String supplierState, String customerState) {
        if (supplierState == null || customerState == null) {
            return false;
        }
        String s1 = supplierState.trim();
        String s2 = customerState.trim();
        if (s1.equalsIgnoreCase(s2)) {
            return true;
        }

        // Check if both match same state code
        String code1 = isValidStateCode(s1) ? s1 : getStateCodeByName(s1);
        String code2 = isValidStateCode(s2) ? s2 : getStateCodeByName(s2);
        return code1 != null && code1.equals(code2);
    }

    public static boolean isValidGstin(String gstin) {
        if (gstin == null || gstin.trim().isEmpty()) {
            return false;
        }
        String clean = gstin.trim().toUpperCase();
        if (!GSTIN_PATTERN.matcher(clean).matches()) {
            return false;
        }
        String stateCode = clean.substring(0, 2);
        return isValidStateCode(stateCode);
    }

    /**
     * Calculates the 15th character checksum using the official Mod-36 Luhn-variant algorithm.
     */
    public static char calculateGstinChecksum(String partialGstin) {
        if (partialGstin == null || partialGstin.trim().length() < 14) {
            throw new IllegalArgumentException("Partial GSTIN must have at least 14 characters");
        }
        String clean = partialGstin.trim().toUpperCase().substring(0, 14);
        int sum = 0;
        boolean factorTwo = false;

        for (int i = clean.length() - 1; i >= 0; i--) {
            int codePoint = ALPHANUMERIC_CODE.indexOf(clean.charAt(i));
            if (codePoint == -1) {
                throw new IllegalArgumentException("Invalid character in GSTIN: " + clean.charAt(i));
            }
            int factor = factorTwo ? 2 : 1;
            int product = codePoint * factor;
            product = (product / 36) + (product % 36);
            sum += product;
            factorTwo = !factorTwo;
        }

        int remainder = sum % 36;
        int checkCode = (36 - remainder) % 36;
        return ALPHANUMERIC_CODE.charAt(checkCode);
    }

    /**
     * Validates whether the 15th character of the GSTIN matches the calculated Mod-36 checksum.
     */
    public static boolean isValidGstinChecksum(String gstin) {
        if (!isValidGstin(gstin)) {
            return false;
        }
        String clean = gstin.trim().toUpperCase();
        char expected = calculateGstinChecksum(clean);
        return clean.charAt(14) == expected;
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

    public static boolean isValidHsn(String hsn) {
        if (hsn == null || hsn.trim().isEmpty()) {
            return false;
        }
        return HSN_PATTERN.matcher(hsn.trim()).matches();
    }

    public static String extractPanFromGstin(String gstin) {
        if (gstin != null && gstin.trim().length() >= 12) {
            String clean = gstin.trim().toUpperCase();
            return clean.substring(2, 12);
        }
        return null;
    }

    public static String extractStateCodeFromGstin(String gstin) {
        if (gstin != null && gstin.trim().length() >= 2) {
            return gstin.trim().toUpperCase().substring(0, 2);
        }
        return null;
    }

    private static final java.util.Set<java.math.BigDecimal> VALID_GST_RATES = java.util.Set.of(
            java.math.BigDecimal.valueOf(0),
            new java.math.BigDecimal("0.1"),
            new java.math.BigDecimal("0.25"),
            new java.math.BigDecimal("1.5"),
            java.math.BigDecimal.valueOf(3),
            java.math.BigDecimal.valueOf(5),
            java.math.BigDecimal.valueOf(6),
            java.math.BigDecimal.valueOf(12),
            java.math.BigDecimal.valueOf(18),
            java.math.BigDecimal.valueOf(28)
    );

    public static boolean isValidGstRate(java.math.BigDecimal rate) {
        if (rate == null) return false;
        java.math.BigDecimal stripped = rate.stripTrailingZeros();
        for (java.math.BigDecimal valid : VALID_GST_RATES) {
            if (stripped.compareTo(valid.stripTrailingZeros()) == 0) {
                return true;
            }
        }
        return false;
    }
}
