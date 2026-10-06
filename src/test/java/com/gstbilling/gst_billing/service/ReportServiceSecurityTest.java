package com.gstbilling.gst_billing.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceSecurityTest {

    @Test
    @DisplayName("Should sanitize CSV formula injection characters by prepending single quote")
    void shouldSanitizeCsvFormulaInjection() {
        ReportService reportService = new ReportService(null, null, null, null, null);

        String formula1 = "=SUM(A1:A10)";
        String formula2 = "+cmd|' /C calc'!A0";
        String formula3 = "-100";
        String formula4 = "@dangerous";
        String normal = "Regular Customer Name";

        String escaped1 = ReflectionTestUtils.invokeMethod(reportService, "escape", formula1);
        String escaped2 = ReflectionTestUtils.invokeMethod(reportService, "escape", formula2);
        String escaped3 = ReflectionTestUtils.invokeMethod(reportService, "escape", formula3);
        String escaped4 = ReflectionTestUtils.invokeMethod(reportService, "escape", formula4);
        String escapedNormal = ReflectionTestUtils.invokeMethod(reportService, "escape", normal);

        assertEquals("\"'=SUM(A1:A10)\"", escaped1);
        assertEquals("\"'+cmd|' /C calc'!A0\"", escaped2);
        assertEquals("\"'-100\"", escaped3);
        assertEquals("\"'@dangerous\"", escaped4);
        assertEquals("\"Regular Customer Name\"", escapedNormal);
    }
}
