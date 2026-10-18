package com.gstbilling.gst_billing.dto;

import java.util.List;

public record HsnSacImportResult(
        String batchId,
        String source,
        String version,
        int totalRecords,
        int insertedRecords,
        int updatedRecords,
        int skippedRecords,
        int rejectedRecords,
        List<String> errors,
        String status
) {}
