package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.Purchase;
import com.gstbilling.gst_billing.entity.Supplier;
import com.gstbilling.gst_billing.repository.PurchaseRepository;
import com.gstbilling.gst_billing.repository.SupplierRepository;
import com.gstbilling.gst_billing.util.IndianTaxValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final CurrentUserService currentUserService;

    public SupplierService(SupplierRepository supplierRepository,
                           PurchaseRepository purchaseRepository,
                           CurrentUserService currentUserService) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Supplier createSupplier(Supplier supplier) {
        Business business = currentUserService.getCurrentUser().getBusiness();
        supplier.setBusiness(business);

        if (supplier.getName() == null || supplier.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier name is required");
        }

        if (supplier.getGstin() != null && !supplier.getGstin().isBlank()) {
            supplier.setGstin(supplier.getGstin().trim().toUpperCase());
            if (!IndianTaxValidator.isValidGstin(supplier.getGstin())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GSTIN format: " + supplier.getGstin());
            }
            if (supplier.getPan() == null || supplier.getPan().isBlank()) {
                supplier.setPan(IndianTaxValidator.extractPanFromGstin(supplier.getGstin()));
            }
        }

        if (supplier.getPan() != null && !supplier.getPan().isBlank()) {
            supplier.setPan(supplier.getPan().trim().toUpperCase());
            if (!IndianTaxValidator.isValidPan(supplier.getPan())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PAN format: " + supplier.getPan());
            }
        }

        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier updateSupplier(Long id, Supplier updated) {
        Supplier supplier = getSupplierById(id);

        if (updated.getName() != null && !updated.getName().isBlank()) {
            supplier.setName(updated.getName());
        }
        if (updated.getGstin() != null) {
            String gstin = updated.getGstin().trim().toUpperCase();
            if (!gstin.isBlank() && !IndianTaxValidator.isValidGstin(gstin)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GSTIN format: " + gstin);
            }
            supplier.setGstin(gstin);
            if (updated.getPan() == null || updated.getPan().isBlank()) {
                supplier.setPan(IndianTaxValidator.extractPanFromGstin(gstin));
            }
        }
        if (updated.getPan() != null && !updated.getPan().isBlank()) {
            String pan = updated.getPan().trim().toUpperCase();
            if (!IndianTaxValidator.isValidPan(pan)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PAN format: " + pan);
            }
            supplier.setPan(pan);
        }
        if (updated.getAddress() != null) supplier.setAddress(updated.getAddress());
        if (updated.getState() != null) supplier.setState(updated.getState());
        if (updated.getStateCode() != null) supplier.setStateCode(updated.getStateCode());
        if (updated.getPhone() != null) supplier.setPhone(updated.getPhone());
        if (updated.getEmail() != null) supplier.setEmail(updated.getEmail());
        if (updated.getPaymentTerms() != null) supplier.setPaymentTerms(updated.getPaymentTerms());

        return supplierRepository.save(supplier);
    }

    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return supplierRepository.findByBusiness_IdOrderByNameAsc(businessId);
    }

    @Transactional(readOnly = true)
    public List<Supplier> searchSuppliers(String query) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        if (query == null || query.isBlank()) {
            return getAllSuppliers();
        }
        return supplierRepository.searchSuppliers(businessId, query.trim());
    }

    @Transactional(readOnly = true)
    public Supplier getSupplierById(Long id) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return supplierRepository.findByIdAndBusiness_Id(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with ID: " + id));
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = getSupplierById(id);
        List<Purchase> purchases = purchaseRepository.findBySupplier_IdAndBusiness_IdOrderByPurchaseDateDesc(id, supplier.getBusiness().getId());
        if (!purchases.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete supplier with existing purchase records.");
        }
        supplierRepository.delete(supplier);
    }

    public record SupplierDetailsResponse(
            Supplier supplier,
            long totalPurchasesCount,
            BigDecimal totalPurchasesAmount,
            BigDecimal totalPaidAmount,
            BigDecimal totalOutstandingAmount,
            List<Purchase> purchases
    ) {}

    @Transactional(readOnly = true)
    public SupplierDetailsResponse getSupplierDetails(Long id) {
        Supplier supplier = getSupplierById(id);
        List<Purchase> purchases = purchaseRepository.findBySupplier_IdAndBusiness_IdOrderByPurchaseDateDesc(id, supplier.getBusiness().getId());

        BigDecimal totalAmount = purchases.stream().map(Purchase::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = purchases.stream().map(Purchase::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = purchases.stream().map(Purchase::getBalanceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SupplierDetailsResponse(supplier, purchases.size(), totalAmount, totalPaid, outstanding, purchases);
    }
}
