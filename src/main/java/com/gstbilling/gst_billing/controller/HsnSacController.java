package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.HsnSacCodeDto;
import com.gstbilling.gst_billing.dto.HsnSacImportResult;
import com.gstbilling.gst_billing.dto.HsnSacValidationResult;
import com.gstbilling.gst_billing.service.CurrentUserService;
import com.gstbilling.gst_billing.service.HsnSacCatalogueService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/hsn-sac")
public class HsnSacController {

    private final HsnSacCatalogueService catalogueService;
    private final CurrentUserService currentUserService;

    public HsnSacController(HsnSacCatalogueService catalogueService, CurrentUserService currentUserService) {
        this.catalogueService = catalogueService;
        this.currentUserService = currentUserService;
    }

    /**
     * Search official HSN/SAC catalogue entries by code prefix or description.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public Page<HsnSacCodeDto> searchCatalogue(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return catalogueService.search(q, type, page, size);
    }

    /**
     * Lookup a catalogue entry by internal database ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public HsnSacCodeDto getById(@PathVariable Long id) {
        return catalogueService.getById(id);
    }

    /**
     * Lookup an exact official code and classification type.
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public HsnSacCodeDto getByCode(
            @PathVariable String code,
            @RequestParam(required = false, defaultValue = "HSN") String type
    ) {
        return catalogueService.getByCodeAndType(code, type);
    }

    /**
     * Validate whether a code is in compliant format and present in the active catalogue.
     */
    @GetMapping("/validate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public HsnSacValidationResult validateCode(
            @RequestParam String code,
            @RequestParam(required = false, defaultValue = "HSN") String type
    ) {
        return catalogueService.validateCode(code, type);
    }

    /**
     * Administrator-only import endpoint for bulk catalogue upload via CSV.
     */
    @PostMapping("/admin/import")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<HsnSacImportResult> importCatalogue(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(defaultValue = "CBIC_OFFICIAL_IMPORT") String source,
            @RequestParam(defaultValue = "v1.0") String version
    ) {
        String csvContent = null;
        if (file != null && !file.isEmpty()) {
            try {
                csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded CSV file: " + e.getMessage());
            }
        } else if (body != null && body.containsKey("csvData")) {
            csvContent = body.get("csvData");
        }

        if (csvContent == null || csvContent.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV data must be provided via file upload or 'csvData' payload.");
        }

        String adminUser = currentUserService.getCurrentUser().getEmail();
        HsnSacImportResult result = catalogueService.importFromCsv(csvContent, source, version, adminUser);
        return ResponseEntity.ok(result);
    }
}
