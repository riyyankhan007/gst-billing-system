package com.gstbilling.gst_billing.config;

import java.math.BigDecimal;
import java.util.List;

public final class GstRateConfig {

    private GstRateConfig() {}

    public static final List<BigDecimal> STANDARD_GST_RATES = List.of(
            BigDecimal.ZERO,
            BigDecimal.valueOf(5),
            BigDecimal.valueOf(12),
            BigDecimal.valueOf(18),
            BigDecimal.valueOf(28)
    );

    public static final List<String> STANDARD_UNITS = List.of(
            "PCS",
            "NOS",
            "KGS",
            "BOX",
            "SET",
            "MTR",
            "LIT",
            "SQF",
            "HRS",
            "DAY",
            "PAC",
            "TON"
    );

    public static boolean isValidRate(BigDecimal rate) {
        if (rate == null) return false;
        return STANDARD_GSTRATES_CONTAINS(rate);
    }

    private static boolean STANDARD_GSTRATES_CONTAINS(BigDecimal rate) {
        for (BigDecimal r : STANDARD_GST_RATES) {
            if (r.compareTo(rate) == 0) {
                return true;
            }
        }
        return false;
    }
}
