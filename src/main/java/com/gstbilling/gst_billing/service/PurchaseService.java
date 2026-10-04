package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.*;
import com.gstbilling.gst_billing.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CurrentUserService currentUserService;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           SupplierRepository supplierRepository,
                           ProductRepository productRepository,
                           StockMovementRepository stockMovementRepository,
                           CurrentUserService currentUserService) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Purchase createPurchase(Purchase purchase) {
        Business business = currentUserService.getCurrentUser().getBusiness();
        purchase.setBusiness(business);

        Supplier supplier = supplierRepository.findByIdAndBusiness_Id(purchase.getSupplierId(), business.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found with ID: " + purchase.getSupplierId()));

        purchase.setSupplier(supplier);
        purchase.setSupplierState(supplier.getState() != null ? supplier.getState() : "");
        purchase.setPurchaseDate(purchase.getPurchaseDate() != null ? purchase.getPurchaseDate() : LocalDate.now());
        if (purchase.getDueDate() == null) {
            purchase.setDueDate(purchase.getPurchaseDate().plusDays(30));
        }
        purchase.setCreatedAt(LocalDateTime.now());

        if (purchase.getItems() == null || purchase.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchase must include at least one item");
        }

        boolean isIntraState = purchase.getSupplierState() != null
                && business.getState() != null
                && purchase.getSupplierState().trim().equalsIgnoreCase(business.getState().trim());

        BigDecimal sumTaxable = BigDecimal.ZERO;
        BigDecimal sumTax = BigDecimal.ZERO;

        for (PurchaseItem item : purchase.getItems()) {
            item.setPurchase(purchase);

            if (item.getProductId() != null) {
                Product prod = productRepository.findByIdAndBusinessId(item.getProductId(), business.getId()).orElse(null);
                if (prod != null) {
                    if (item.getProductName() == null || item.getProductName().isBlank()) item.setProductName(prod.getName());
                    if (item.getHsnCode() == null || item.getHsnCode().isBlank()) item.setHsnCode(prod.getHsnCode());
                    if (item.getUnitPrice() == null) item.setUnitPrice(prod.getPrice());
                    if (item.getGstRate() == null) item.setGstRate(prod.getGstRate());
                    if (item.getUnit() == null) item.setUnit(prod.getUnit());
                }
            }

            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal disc = item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO;
            BigDecimal rate = item.getGstRate() != null ? item.getGstRate() : BigDecimal.ZERO;

            BigDecimal lineGross = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTaxable = lineGross.subtract(disc);
            if (lineTaxable.compareTo(BigDecimal.ZERO) < 0) lineTaxable = BigDecimal.ZERO;

            BigDecimal lineTax = lineTaxable.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineTaxable.add(lineTax).setScale(2, RoundingMode.HALF_UP);

            item.setTaxableAmount(lineTaxable);
            item.setTaxAmount(lineTax);
            item.setTotalAmount(lineTotal);

            sumTaxable = sumTaxable.add(lineTaxable);
            sumTax = sumTax.add(lineTax);
        }

        purchase.setTaxableAmount(sumTaxable);
        purchase.setTotalTax(sumTax);

        if (isIntraState) {
            BigDecimal half = sumTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            purchase.setCgst(half);
            purchase.setSgst(sumTax.subtract(half));
            purchase.setIgst(BigDecimal.ZERO);
        } else {
            purchase.setCgst(BigDecimal.ZERO);
            purchase.setSgst(BigDecimal.ZERO);
            purchase.setIgst(sumTax);
        }

        BigDecimal grandTotal = sumTaxable.add(sumTax).setScale(2, RoundingMode.HALF_UP);
        purchase.setGrandTotal(grandTotal);

        BigDecimal paid = purchase.getPaidAmount() != null ? purchase.getPaidAmount() : BigDecimal.ZERO;
        purchase.setPaidAmount(paid);
        BigDecimal balance = grandTotal.subtract(paid).setScale(2, RoundingMode.HALF_UP);
        purchase.setBalanceAmount(balance);

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            purchase.setPaymentStatus("PAID");
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            purchase.setPaymentStatus("PARTIALLY_PAID");
        } else {
            purchase.setPaymentStatus("UNPAID");
        }

        Purchase saved = purchaseRepository.save(purchase);

        // Increase inventory stock for products
        for (PurchaseItem item : saved.getItems()) {
            if (item.getProductId() != null) {
                Product prod = productRepository.findByIdAndBusinessId(item.getProductId(), business.getId()).orElse(null);
                if (prod != null && !"SERVICE".equalsIgnoreCase(prod.getProductType())) {
                    BigDecimal before = prod.getStockQuantity() != null ? prod.getStockQuantity() : BigDecimal.ZERO;
                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                    BigDecimal after = before.add(qty);
                    prod.setStockQuantity(after);
                    productRepository.save(prod);

                    StockMovement movement = new StockMovement(
                            business,
                            prod,
                            "PURCHASE",
                            qty,
                            before,
                            after,
                            saved.getSupplierInvoiceNumber(),
                            "Stock added from supplier purchase #" + saved.getSupplierInvoiceNumber()
                    );
                    stockMovementRepository.save(movement);
                }
            }
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Purchase> getAllPurchases() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return purchaseRepository.findByBusiness_IdOrderByPurchaseDateDescIdDesc(businessId);
    }

    @Transactional(readOnly = true)
    public Purchase getPurchaseById(Long id) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return purchaseRepository.findByIdAndBusiness_Id(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found with ID: " + id));
    }

    @Transactional
    public Purchase recordPurchasePayment(Long id, BigDecimal amount) {
        Purchase purchase = getPurchaseById(id);
        BigDecimal payAmount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal currentPaid = purchase.getPaidAmount() != null ? purchase.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaid = currentPaid.add(payAmount);
        purchase.setPaidAmount(newPaid);
        BigDecimal newBalance = purchase.getGrandTotal().subtract(newPaid).setScale(2, RoundingMode.HALF_UP);
        purchase.setBalanceAmount(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            purchase.setPaymentStatus("PAID");
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            purchase.setPaymentStatus("PARTIALLY_PAID");
        }

        return purchaseRepository.save(purchase);
    }

    @Transactional
    public void deletePurchase(Long id) {
        Purchase purchase = getPurchaseById(id);
        Business business = purchase.getBusiness();

        // Reverse stock increase
        for (PurchaseItem item : purchase.getItems()) {
            if (item.getProductId() != null) {
                Product prod = productRepository.findByIdAndBusinessId(item.getProductId(), business.getId()).orElse(null);
                if (prod != null && !"SERVICE".equalsIgnoreCase(prod.getProductType())) {
                    BigDecimal before = prod.getStockQuantity() != null ? prod.getStockQuantity() : BigDecimal.ZERO;
                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                    BigDecimal after = before.subtract(qty);
                    prod.setStockQuantity(after);
                    productRepository.save(prod);

                    StockMovement movement = new StockMovement(
                            business,
                            prod,
                            "PURCHASE_CANCELLED",
                            qty.negate(),
                            before,
                            after,
                            purchase.getSupplierInvoiceNumber(),
                            "Purchase deleted - stock reverted"
                    );
                    stockMovementRepository.save(movement);
                }
            }
        }

        purchaseRepository.delete(purchase);
    }
}
