package com.gstbilling.gst_billing.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumberToWordsConverter {

    private NumberToWordsConverter() {}

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convertToIndianCurrencyWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Rupees Zero Only";
        }

        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = rounded.longValue();
        int paise = rounded.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();

        StringBuilder words = new StringBuilder("Rupees ");
        if (rupees > 0) {
            words.append(convertIndianNumber(rupees));
        } else {
            words.append("Zero");
        }

        if (paise > 0) {
            words.append(" and ").append(convertIndianNumber(paise)).append(" Paise");
        }
        words.append(" Only");

        return words.toString().replaceAll("\\s+", " ").trim();
    }

    private static String convertIndianNumber(long number) {
        if (number == 0) return "";

        StringBuilder sb = new StringBuilder();

        // Crores (>= 1,00,00,000)
        long crore = number / 10000000;
        if (crore > 0) {
            sb.append(convertIndianNumber(crore)).append(" Crore ");
            number %= 10000000;
        }

        // Lakhs (>= 1,00,000)
        long lakh = number / 100000;
        if (lakh > 0) {
            sb.append(convertUnderThousand((int) lakh)).append(" Lakh ");
            number %= 100000;
        }

        // Thousands (>= 1,000)
        long thousand = number / 1000;
        if (thousand > 0) {
            sb.append(convertUnderThousand((int) thousand)).append(" Thousand ");
            number %= 1000;
        }

        // Hundreds and tens
        if (number > 0) {
            sb.append(convertUnderThousand((int) number));
        }

        return sb.toString().trim();
    }

    private static String convertUnderThousand(int n) {
        StringBuilder sb = new StringBuilder();
        if (n >= 100) {
            sb.append(UNITS[n / 100]).append(" Hundred ");
            n %= 100;
        }
        if (n >= 20) {
            sb.append(TENS[n / 10]).append(" ");
            n %= 10;
        }
        if (n > 0) {
            sb.append(UNITS[n]).append(" ");
        }
        return sb.toString().trim();
    }
}
