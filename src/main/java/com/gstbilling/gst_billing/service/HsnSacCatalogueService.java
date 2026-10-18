package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.HsnSacCodeDto;
import com.gstbilling.gst_billing.dto.HsnSacImportResult;
import com.gstbilling.gst_billing.dto.HsnSacValidationResult;
import com.gstbilling.gst_billing.entity.GstTaxRule;
import com.gstbilling.gst_billing.entity.HsnSacCode;
import com.gstbilling.gst_billing.entity.HsnSacImportBatch;
import com.gstbilling.gst_billing.entity.HsnSacType;
import com.gstbilling.gst_billing.repository.GstTaxRuleRepository;
import com.gstbilling.gst_billing.repository.HsnSacCodeRepository;
import com.gstbilling.gst_billing.repository.HsnSacImportBatchRepository;
import com.gstbilling.gst_billing.util.HsnSacValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class HsnSacCatalogueService {

    private final HsnSacCodeRepository hsnSacCodeRepository;
    private final GstTaxRuleRepository gstTaxRuleRepository;
    private final HsnSacImportBatchRepository importBatchRepository;
    private final AuditLogService auditLogService;

    public HsnSacCatalogueService(
            HsnSacCodeRepository hsnSacCodeRepository,
            GstTaxRuleRepository gstTaxRuleRepository,
            HsnSacImportBatchRepository importBatchRepository,
            AuditLogService auditLogService
    ) {
        this.hsnSacCodeRepository = hsnSacCodeRepository;
        this.gstTaxRuleRepository = gstTaxRuleRepository;
        this.importBatchRepository = importBatchRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<HsnSacCodeDto> search(String query, String typeStr, int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must not be less than zero.");
        }
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100.");
        }

        HsnSacType type = HsnSacType.fromString(typeStr);
        String cleanQuery = (query != null) ? query.trim() : "";

        Pageable pageable = PageRequest.of(page, size);
        Page<HsnSacCode> results = hsnSacCodeRepository.searchCatalogue(cleanQuery, type, pageable);

        return results.map(HsnSacCodeDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public HsnSacCodeDto getById(Long id) {
        HsnSacCode code = hsnSacCodeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "HSN/SAC code not found with ID: " + id));
        return HsnSacCodeDto.fromEntity(code);
    }

    @Transactional(readOnly = true)
    public HsnSacCodeDto getByCodeAndType(String code, String typeStr) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code is required.");
        }
        HsnSacType type = HsnSacType.fromString(typeStr);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Classification type must be HSN or SAC.");
        }

        HsnSacCode entity = hsnSacCodeRepository.findByCodeAndTypeAndActiveTrue(code.trim(), type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Official " + type + " code '" + code + "' not found or inactive."));
        return HsnSacCodeDto.fromEntity(entity);
    }

    @Transactional(readOnly = true)
    public HsnSacValidationResult validateCode(String code, String typeStr) {
        if (code == null || code.isBlank()) {
            return new HsnSacValidationResult(false, code, typeStr, false, null, "Code cannot be empty.");
        }
        HsnSacType type = HsnSacType.fromString(typeStr);
        if (type == null) {
            return new HsnSacValidationResult(false, code, typeStr, false, null, "Type must be either HSN or SAC.");
        }

        String cleanCode = code.trim();
        boolean validFormat = HsnSacValidator.isValidFormat(cleanCode, type);
        if (!validFormat) {
            String formatHelp = (type == HsnSacType.HSN)
                    ? "HSN code must be 2, 4, 6, or 8 digits."
                    : "SAC code must start with '99' and be 4, 5, or 6 digits in length.";
            return new HsnSacValidationResult(false, cleanCode, type.name(), false, null, "Invalid " + type + " format: " + formatHelp);
        }

        Optional<HsnSacCode> existing = hsnSacCodeRepository.findByCodeAndTypeAndActiveTrue(cleanCode, type);
        if (existing.isPresent()) {
            HsnSacCodeDto dto = HsnSacCodeDto.fromEntity(existing.get());
            return new HsnSacValidationResult(true, cleanCode, type.name(), true, dto, "Valid official " + type + " code: " + dto.description());
        } else {
            return new HsnSacValidationResult(false, cleanCode, type.name(), false, null,
                    "Code '" + cleanCode + "' has a valid " + type + " structure but does not exist in the official active catalogue.");
        }
    }

    @Transactional
    public HsnSacImportResult importFromCsv(String csvContent, String source, String version, String importedBy) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV content cannot be empty.");
        }

        String batchId = "BATCH-" + UUID.randomUUID();
        String safeSource = (source != null && !source.isBlank()) ? source.trim() : "ADMIN_IMPORT";
        String safeVersion = (version != null && !version.isBlank()) ? version.trim() : "v1.0";
        String safeImportedBy = (importedBy != null && !importedBy.isBlank()) ? importedBy.trim() : "ADMIN";

        int total = 0;
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int rejected = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            int lineNumber = 0;
            boolean hasHeader = false;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Header check
                if (!hasHeader) {
                    hasHeader = true;
                    if (line.toLowerCase().contains("code") && line.toLowerCase().contains("type")) {
                        continue; // Skip header line
                    }
                }

                total++;
                // Format: code,type,description,classification_level,gst_rate,effective_from,source
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length < 3) {
                    rejected++;
                    errors.add("Line " + lineNumber + ": Insufficient columns. Minimum required: code,type,description.");
                    continue;
                }

                String code = stripQuotes(parts[0]);
                String typeStr = stripQuotes(parts[1]);
                String description = stripQuotes(parts[2]);
                String level = parts.length > 3 ? stripQuotes(parts[3]) : null;
                String rateStr = parts.length > 4 ? stripQuotes(parts[4]) : null;
                String effectiveFromStr = parts.length > 5 ? stripQuotes(parts[5]) : null;
                String lineSource = parts.length > 6 ? stripQuotes(parts[6]) : safeSource;

                HsnSacType type = HsnSacType.fromString(typeStr);
                if (type == null) {
                    rejected++;
                    errors.add("Line " + lineNumber + ": Invalid type '" + typeStr + "'. Must be HSN or SAC.");
                    continue;
                }

                if (!HsnSacValidator.isValidFormat(code, type)) {
                    rejected++;
                    errors.add("Line " + lineNumber + ": Invalid code format '" + code + "' for " + type + ".");
                    continue;
                }

                if (description == null || description.isBlank()) {
                    rejected++;
                    errors.add("Line " + lineNumber + ": Description is required.");
                    continue;
                }

                BigDecimal rate = null;
                if (rateStr != null && !rateStr.isBlank()) {
                    try {
                        rate = new BigDecimal(rateStr.trim()).setScale(2, RoundingMode.HALF_UP);
                        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
                            rejected++;
                            errors.add("Line " + lineNumber + ": Invalid GST rate '" + rateStr + "'. Must be between 0 and 100.");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        rejected++;
                        errors.add("Line " + lineNumber + ": Numeric GST rate required, got '" + rateStr + "'.");
                        continue;
                    }
                }

                LocalDate effectiveFrom = LocalDate.of(2017, 7, 1);
                if (effectiveFromStr != null && !effectiveFromStr.isBlank()) {
                    try {
                        effectiveFrom = LocalDate.parse(effectiveFromStr.trim());
                    } catch (Exception e) {
                        rejected++;
                        errors.add("Line " + lineNumber + ": Invalid date format '" + effectiveFromStr + "'. Expected YYYY-MM-DD.");
                        continue;
                    }
                }

                if (level == null || level.isBlank()) {
                    level = HsnSacValidator.determineClassificationLevel(code, type);
                }

                // Idempotent upsert
                Optional<HsnSacCode> existingOpt = hsnSacCodeRepository.findByCodeAndType(code, type);
                if (existingOpt.isPresent()) {
                    HsnSacCode existing = existingOpt.get();
                    boolean changed = false;

                    if (!description.equals(existing.getDescription())) {
                        existing.setDescription(description);
                        changed = true;
                    }
                    if (rate != null && (existing.getGstRate() == null || rate.compareTo(existing.getGstRate()) != 0)) {
                        existing.setGstRate(rate);
                        changed = true;
                    }
                    if (level != null && !level.equalsIgnoreCase(existing.getClassificationLevel())) {
                        existing.setClassificationLevel(level);
                        changed = true;
                    }
                    if (!lineSource.equals(existing.getSource())) {
                        existing.setSource(lineSource);
                        changed = true;
                    }
                    if (!Boolean.TRUE.equals(existing.getActive())) {
                        existing.setActive(true);
                        changed = true;
                    }

                    if (changed) {
                        existing.setVerificationDate(LocalDate.now());
                        existing.setUpdatedAt(LocalDateTime.now());
                        hsnSacCodeRepository.save(existing);
                        updated++;
                    } else {
                        skipped++;
                    }
                } else {
                    HsnSacCode newCode = new HsnSacCode(code, type, description, level, rate, lineSource);
                    newCode.setEffectiveFrom(effectiveFrom);
                    newCode.setVerificationDate(LocalDate.now());
                    HsnSacCode saved = hsnSacCodeRepository.save(newCode);

                    // If rate provided, create initial tax rule
                    if (rate != null) {
                        BigDecimal halfRate = rate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                        GstTaxRule rule = new GstTaxRule(saved, rate, halfRate, halfRate, "Import Batch: " + batchId);
                        rule.setEffectiveFrom(effectiveFrom);
                        gstTaxRuleRepository.save(rule);
                    }
                    inserted++;
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading CSV stream: " + e.getMessage());
        }

        // Save batch record
        HsnSacImportBatch batch = new HsnSacImportBatch(batchId, safeSource, safeVersion, safeImportedBy);
        batch.setTotalRecords(total);
        batch.setInsertedRecords(inserted);
        batch.setUpdatedRecords(updated);
        batch.setSkippedRecords(skipped);
        batch.setRejectedRecords(rejected);
        if (!errors.isEmpty()) {
            batch.setErrorSummary(String.join("\n", errors.subList(0, Math.min(errors.size(), 50))));
        }
        batch.setStatus(rejected > 0 ? (inserted + updated > 0 ? "PARTIAL_SUCCESS" : "FAILED") : "SUCCESS");
        importBatchRepository.save(batch);

        // Audit log
        auditLogService.logAction(
                "IMPORT_HSN_SAC_CATALOGUE",
                "CATALOGUE",
                batch.getId(),
                String.format("Batch %s: Total=%d, Inserted=%d, Updated=%d, Skipped=%d, Rejected=%d",
                        batchId, total, inserted, updated, skipped, rejected)
        );

        return new HsnSacImportResult(
                batchId,
                safeSource,
                safeVersion,
                total,
                inserted,
                updated,
                skipped,
                rejected,
                errors,
                batch.getStatus()
        );
    }

    private String stripQuotes(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
