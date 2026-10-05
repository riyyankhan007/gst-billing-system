package com.gstbilling.gst_billing.util;

import java.time.LocalDate;

/**
 * Utility class for Indian Financial Year calculation (April 1 to March 31).
 */
public final class FinancialYearUtil {

    private FinancialYearUtil() {}

    /**
     * Determines Indian Financial Year string (e.g. "2026-27") for a given date.
     * FY runs from April 1 to March 31.
     *
     * @param date Date to evaluate, or LocalDate.now() if null
     * @return Formatted FY string (e.g., "2026-27")
     */
    public static String getFinancialYear(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        int year = date.getYear();
        int month = date.getMonthValue();
        int startYear = (month >= 4) ? year : year - 1;
        int endYear = startYear + 1;
        return String.format("%d-%02d", startYear, endYear % 100);
    }

    /**
     * Returns the start date of the financial year (April 1st).
     */
    public static LocalDate getStartDateOfFinancialYear(String financialYear) {
        if (financialYear == null || !financialYear.contains("-")) {
            return LocalDate.of(LocalDate.now().getYear(), 4, 1);
        }
        try {
            int startYear = Integer.parseInt(financialYear.split("-")[0].trim());
            return LocalDate.of(startYear, 4, 1);
        } catch (Exception e) {
            return LocalDate.of(LocalDate.now().getYear(), 4, 1);
        }
    }

    /**
     * Returns the end date of the financial year (March 31st).
     */
    public static LocalDate getEndDateOfFinancialYear(String financialYear) {
        LocalDate startDate = getStartDateOfFinancialYear(financialYear);
        return startDate.plusYears(1).minusDays(1);
    }
}
