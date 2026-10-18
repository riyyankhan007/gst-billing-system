package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.HsnSacCodeDto;
import com.gstbilling.gst_billing.dto.HsnSacImportResult;
import com.gstbilling.gst_billing.dto.HsnSacValidationResult;
import com.gstbilling.gst_billing.entity.HsnSacCode;
import com.gstbilling.gst_billing.entity.HsnSacType;
import com.gstbilling.gst_billing.repository.GstTaxRuleRepository;
import com.gstbilling.gst_billing.repository.HsnSacCodeRepository;
import com.gstbilling.gst_billing.repository.HsnSacImportBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HsnSacCatalogueServiceTest {

    private HsnSacCodeRepository hsnSacCodeRepository;
    private GstTaxRuleRepository gstTaxRuleRepository;
    private HsnSacImportBatchRepository importBatchRepository;
    private AuditLogService auditLogService;
    private HsnSacCatalogueService catalogueService;

    @BeforeEach
    void setUp() {
        hsnSacCodeRepository = mock(HsnSacCodeRepository.class);
        gstTaxRuleRepository = mock(GstTaxRuleRepository.class);
        importBatchRepository = mock(HsnSacImportBatchRepository.class);
        auditLogService = mock(AuditLogService.class);

        catalogueService = new HsnSacCatalogueService(
                hsnSacCodeRepository,
                gstTaxRuleRepository,
                importBatchRepository,
                auditLogService
        );
    }

    @Test
    @DisplayName("Search delegates to repository with normalized query and pagination")
    void testSearch() {
        HsnSacCode sample = new HsnSacCode("8471", HsnSacType.HSN, "Automatic data processing machines", "4_DIGIT", BigDecimal.valueOf(18), "CBIC");
        Page<HsnSacCode> page = new PageImpl<>(List.of(sample));

        when(hsnSacCodeRepository.searchCatalogue(eq("computer"), eq(HsnSacType.HSN), any(PageRequest.class)))
                .thenReturn(page);

        Page<HsnSacCodeDto> result = catalogueService.search(" computer ", "HSN", 0, 10);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("8471", result.getContent().get(0).code());
        assertEquals("HSN", result.getContent().get(0).type());
    }

    @Test
    @DisplayName("Lookup by ID returns DTO or throws 404 if missing")
    void testGetById() {
        HsnSacCode sample = new HsnSacCode("998311", HsnSacType.SAC, "IT Consulting", "TARIFF", BigDecimal.valueOf(18), "CBIC");
        sample.setId(101L);

        when(hsnSacCodeRepository.findById(101L)).thenReturn(Optional.of(sample));
        when(hsnSacCodeRepository.findById(999L)).thenReturn(Optional.empty());

        HsnSacCodeDto dto = catalogueService.getById(101L);
        assertNotNull(dto);
        assertEquals("998311", dto.code());
        assertEquals("SAC", dto.type());

        assertThrows(ResponseStatusException.class, () -> catalogueService.getById(999L));
    }

    @Test
    @DisplayName("Validate returns existence, official item details, or appropriate error message")
    void testValidateCode() {
        HsnSacCode sample = new HsnSacCode("84713010", HsnSacType.HSN, "Personal computer", "8_DIGIT", BigDecimal.valueOf(18), "CBIC");
        when(hsnSacCodeRepository.findByCodeAndTypeAndActiveTrue("84713010", HsnSacType.HSN))
                .thenReturn(Optional.of(sample));
        when(hsnSacCodeRepository.findByCodeAndTypeAndActiveTrue("84719999", HsnSacType.HSN))
                .thenReturn(Optional.empty());

        // 1. Valid existing code
        HsnSacValidationResult validRes = catalogueService.validateCode("84713010", "HSN");
        assertTrue(validRes.valid());
        assertTrue(validRes.existsInCatalogue());
        assertNotNull(validRes.item());
        assertEquals("84713010", validRes.item().code());

        // 2. Structurally valid format but missing in catalogue
        HsnSacValidationResult notFoundRes = catalogueService.validateCode("84719999", "HSN");
        assertFalse(notFoundRes.valid());
        assertFalse(notFoundRes.existsInCatalogue());
        assertTrue(notFoundRes.message().contains("does not exist in the official active catalogue"));

        // 3. Invalid format (odd length for HSN)
        HsnSacValidationResult invalidFormatRes = catalogueService.validateCode("84715", "HSN");
        assertFalse(invalidFormatRes.valid());
        assertTrue(invalidFormatRes.message().contains("Invalid HSN format"));
    }

    @Test
    @DisplayName("Import CSV processes idempotent upserts, creates rules, records provenance, and tracks rejections")
    void testImportFromCsv() {
        String csv = """
                code,type,description,classification_level,gst_rate,effective_from,source
                84714190,HSN,"Micro computers",8_DIGIT,18.00,2017-07-01,CBIC_IMPORT
                998313,SAC,"IT design and development",TARIFF,18.00,2017-07-01,CBIC_IMPORT
                INVALID_CODE,HSN,"Bad Code",8_DIGIT,18.00,2017-07-01,CBIC_IMPORT
                """;

        when(hsnSacCodeRepository.findByCodeAndType("84714190", HsnSacType.HSN)).thenReturn(Optional.empty());
        when(hsnSacCodeRepository.findByCodeAndType("998313", HsnSacType.SAC)).thenReturn(Optional.empty());

        when(hsnSacCodeRepository.save(any(HsnSacCode.class))).thenAnswer(i -> {
            HsnSacCode c = i.getArgument(0);
            c.setId(200L);
            return c;
        });

        HsnSacImportResult res = catalogueService.importFromCsv(csv, "CBIC_CUSTOMS", "2024.2", "admin@gstbilling.com");

        assertNotNull(res);
        assertEquals(3, res.totalRecords());
        assertEquals(2, res.insertedRecords());
        assertEquals(1, res.rejectedRecords());
        assertFalse(res.errors().isEmpty());

        verify(hsnSacCodeRepository, times(2)).save(any(HsnSacCode.class));
        verify(gstTaxRuleRepository, times(2)).save(any());
        verify(importBatchRepository, times(1)).save(any());
        verify(auditLogService, times(1)).logAction(eq("IMPORT_HSN_SAC_CATALOGUE"), any(), any(), any());
    }

    @Test
    @DisplayName("Import CSV updates existing record idempotently without creating duplicates")
    void testImportFromCsvIdempotentUpdate() {
        HsnSacCode existing = new HsnSacCode("8471", HsnSacType.HSN, "Old Description", "4_DIGIT", BigDecimal.valueOf(18), "OLD_SOURCE");
        existing.setId(50L);

        when(hsnSacCodeRepository.findByCodeAndType("8471", HsnSacType.HSN)).thenReturn(Optional.of(existing));

        String csv = """
                code,type,description,classification_level,gst_rate,effective_from,source
                8471,HSN,"Updated Official Description",4_DIGIT,18.00,2017-07-01,NEW_SOURCE
                """;

        HsnSacImportResult res = catalogueService.importFromCsv(csv, "UPDATE_TEST", "2024.3", "admin@test.com");

        assertEquals(1, res.totalRecords());
        assertEquals(0, res.insertedRecords());
        assertEquals(1, res.updatedRecords());
        assertEquals("Updated Official Description", existing.getDescription());
        assertEquals("NEW_SOURCE", existing.getSource());

        verify(hsnSacCodeRepository, times(1)).save(existing);
    }
}
