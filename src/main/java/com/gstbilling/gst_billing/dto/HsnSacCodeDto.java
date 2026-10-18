package com.gstbilling.gst_billing.dto;

import com.gstbilling.gst_billing.entity.HsnSacCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record HsnSacCodeDto(
        Long id,
        String code,
        String type,
        String description,
        String classificationLevel,
        BigDecimal gstRate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        String source,
        LocalDate verificationDate,
        List<HsnSacTaxRuleDto> taxRules
) {
    public static HsnSacCodeDto fromEntity(HsnSacCode entity) {
        if (entity == null) return null;
        List<HsnSacTaxRuleDto> rules = (entity.getTaxRules() != null)
                ? entity.getTaxRules().stream().map(HsnSacTaxRuleDto::fromEntity).toList()
                : Collections.emptyList();

        return new HsnSacCodeDto(
                entity.getId(),
                entity.getCode(),
                entity.getType() != null ? entity.getType().name() : null,
                entity.getDescription(),
                entity.getClassificationLevel(),
                entity.getGstRate(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                Boolean.TRUE.equals(entity.getActive()),
                entity.getSource(),
                entity.getVerificationDate(),
                rules
        );
    }
}
