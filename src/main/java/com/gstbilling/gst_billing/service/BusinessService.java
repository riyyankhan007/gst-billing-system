package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.util.IndianTaxValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public BusinessService(BusinessRepository businessRepository,
                           CurrentUserService currentUserService,
                           AuditLogService auditLogService) {
        this.businessRepository = businessRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Business getBusiness() {
        Long tenantId = currentUserService.getCurrentTenantId();
        if (tenantId != null) {
            return businessRepository.findById(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
        }
        return currentUserService.getCurrentUser().getBusiness();
    }

    @Transactional
    public Business updateBusiness(Business changes) {
        Business business = getBusiness();

        if (changes.getName() != null && !changes.getName().isBlank()) {
            business.setName(changes.getName().trim());
        }

        // GSTIN validation & uniqueness
        if (changes.getGstin() != null && !changes.getGstin().isBlank()) {
            String cleanGstin = changes.getGstin().trim().toUpperCase();
            if (!IndianTaxValidator.isValidGstin(cleanGstin)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GSTIN format. Expected format: 22AAAAA0000A1Z5");
            }

            if (businessRepository.existsByGstinIgnoreCaseAndIdNot(cleanGstin, business.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another business is already registered with this GSTIN.");
            }
            business.setGstin(cleanGstin);

            // Auto-extract PAN from GSTIN if PAN is not explicitly set
            if (business.getPan() == null || business.getPan().isBlank()) {
                business.setPan(IndianTaxValidator.extractPanFromGstin(cleanGstin));
            }
        } else {
            business.setGstin(null);
        }

        // PAN validation
        if (changes.getPan() != null && !changes.getPan().isBlank()) {
            String cleanPan = changes.getPan().trim().toUpperCase();
            if (!IndianTaxValidator.isValidPan(cleanPan)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PAN format. Expected format: AAAAA0000A");
            }
            business.setPan(cleanPan);
        }

        // Bank IFSC validation
        if (changes.getBankIfsc() != null && !changes.getBankIfsc().isBlank()) {
            String cleanIfsc = changes.getBankIfsc().trim().toUpperCase();
            if (!IndianTaxValidator.isValidIfsc(cleanIfsc)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Bank IFSC format. Expected 11 characters (e.g. HDFC0001234)");
            }
            business.setBankIfsc(cleanIfsc);
        } else {
            business.setBankIfsc(changes.getBankIfsc());
        }

        business.setAddress(changes.getAddress());
        business.setState(changes.getState());
        business.setStateCode(changes.getStateCode());
        business.setPhone(changes.getPhone());
        business.setEmail(changes.getEmail());
        business.setWebsite(changes.getWebsite());

        if (changes.getInvoicePrefix() != null && !changes.getInvoicePrefix().isBlank()) {
            business.setInvoicePrefix(changes.getInvoicePrefix().trim().toUpperCase());
        }

        if (changes.getFinancialYear() != null && !changes.getFinancialYear().isBlank()) {
            business.setFinancialYear(changes.getFinancialYear().trim());
        }

        if (changes.getInvoiceSeqNumber() != null && changes.getInvoiceSeqNumber() > 0) {
            business.setInvoiceSeqNumber(changes.getInvoiceSeqNumber());
        }

        business.setBankName(changes.getBankName());
        business.setBankAccountNumber(changes.getBankAccountNumber());
        business.setUpiId(changes.getUpiId());
        business.setUpiQrCode(changes.getUpiQrCode());
        business.setDefaultTerms(changes.getDefaultTerms());

        Business saved = businessRepository.save(business);
        auditLogService.logAction("UPDATE_BUSINESS", "BUSINESS", saved.getId(), "Business settings updated");
        return saved;
    }

    @Transactional
    public Business saveLogo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a logo file");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "logo.png";
        String filename = "business-" + getBusiness().getId() + "-logo-" + System.currentTimeMillis() + "-" + Path.of(original).getFileName();
        try {
            Path directory = Path.of("uploads").toAbsolutePath();
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            Business business = getBusiness();
            business.setLogo("/uploads/" + filename);
            Business saved = businessRepository.save(business);
            auditLogService.logAction("UPDATE_LOGO", "BUSINESS", saved.getId(), "Business logo updated");
            return saved;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save logo");
        }
    }

    @Transactional
    public Business saveSignature(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a signature/stamp file");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "signature.png";
        String filename = "business-" + getBusiness().getId() + "-signature-" + System.currentTimeMillis() + "-" + Path.of(original).getFileName();
        try {
            Path directory = Path.of("uploads").toAbsolutePath();
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            Business business = getBusiness();
            business.setSignature("/uploads/" + filename);
            Business saved = businessRepository.save(business);
            auditLogService.logAction("UPDATE_SIGNATURE", "BUSINESS", saved.getId(), "Business signature updated");
            return saved;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save signature/stamp");
        }
    }
}
