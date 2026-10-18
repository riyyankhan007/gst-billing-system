package com.gstbilling.gst_billing.dto;

import com.gstbilling.gst_billing.entity.GstTaxRule;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HsnSacTaxRuleDto(
        Long id,
        BigDecimal igstRate,
        BigDecimal cgstRate,
        BigDecimal sgstRate,
        BigDecimal cessRate,
        String conditionDescription,
        String notificationNumber,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean isConditional,
        boolean requiresReview,
        boolean active
) {
    public static HsnSacTaxRuleDto fromEntity(GstTaxRule rule) {
        if (rule == null) return null;
        return new HsnSacTaxRuleDto(
                rule.getId(),
                rule.getIgstRate(),
                rule.getCgstRate(),
                rule.getSgstRate(),
                rule.getCessRate(),
                rule.getConditionDescription(),
                rule.getNotificationNumber(),
                rule.getEffectiveFrom(),
                rule.getEffectiveTo(),
                Boolean.TRUE.equals(rule.getIsConditional()),
                Boolean.TRUE.equals(rule.getRequiresReview()),
                Boolean.TRUE.equals(rule.getActive())
        );
    }
}
