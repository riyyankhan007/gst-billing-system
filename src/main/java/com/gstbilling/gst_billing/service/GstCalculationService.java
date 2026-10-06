package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.util.IndianTaxValidator;
import com.gstbilling.gst_billing.util.NumberToWordsConverter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class GstCalculationService {

    public record ItemInput(
            Long productId,
            String productName,
            String hsnCode,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal gstRate,
            BigDecimal discount,
            boolean taxInclusive
    ) {}

    public record ItemOutput(
            Long productId,
            String productName,
            String hsnCode,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal gstRate,
            BigDecimal discount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount
    ) {}

    public record GstResult(
            BigDecimal taxableAmount,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal totalTax,
            BigDecimal grandTotal,
            String amountInWords,
            boolean intraState,
            List<ItemOutput> items,
            boolean reverseCharge,
            BigDecimal roundOffAmount
    ) {
        // Compatibility constructor for existing callers
        public GstResult(
                BigDecimal taxableAmount,
                BigDecimal cgst,
                BigDecimal sgst,
                BigDecimal igst,
                BigDecimal totalTax,
                BigDecimal grandTotal,
                String amountInWords,
                boolean intraState,
                List<ItemOutput> items
        ) {
            this(taxableAmount, cgst, sgst, igst, totalTax, grandTotal, amountInWords, intraState, items, false, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
    }

    public GstResult calculate(
            String supplierState,
            String customerState,
            boolean isExport,
            BigDecimal invoiceDiscount,
            List<ItemInput> itemInputs
    ) {
        return calculate(supplierState, customerState, isExport, false, invoiceDiscount, itemInputs);
    }

    public GstResult calculate(
            String supplierState,
            String customerState,
            boolean isExport,
            boolean reverseCharge,
            BigDecimal invoiceDiscount,
            List<ItemInput> itemInputs
    ) {
        if (itemInputs == null || itemInputs.isEmpty()) {
            throw new IllegalArgumentException("Invoice must have at least one line item");
        }

        BigDecimal sumTaxable = BigDecimal.ZERO;
        BigDecimal sumTotalTax = BigDecimal.ZERO;
        List<ItemOutput> calculatedItems = new ArrayList<>();

        for (ItemInput item : itemInputs) {
            BigDecimal qty = (item.quantity() != null) ? item.quantity() : BigDecimal.ONE;
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero: " + qty);
            }

            BigDecimal price = item.unitPrice() != null ? item.unitPrice() : BigDecimal.ZERO;
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price cannot be negative: " + price);
            }

            BigDecimal gstRate = item.gstRate() != null ? item.gstRate() : BigDecimal.ZERO;
            if (gstRate.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("GST rate cannot be negative: " + gstRate);
            }

            BigDecimal discount = item.discount() != null ? item.discount() : BigDecimal.ZERO;
            if (discount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Discount cannot be negative: " + discount);
            }

            BigDecimal lineGross = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineAfterDiscount = lineGross.subtract(discount);
            if (lineAfterDiscount.compareTo(BigDecimal.ZERO) < 0) {
                lineAfterDiscount = BigDecimal.ZERO;
            }

            BigDecimal lineTaxable;
            BigDecimal lineTax;
            BigDecimal lineTotal;

            if (item.taxInclusive() && gstRate.compareTo(BigDecimal.ZERO) > 0) {
                // Price already includes GST: taxable = total / (1 + rate/100)
                BigDecimal divisor = BigDecimal.ONE.add(gstRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                lineTaxable = lineAfterDiscount.divide(divisor, 2, RoundingMode.HALF_UP);
                lineTax = lineAfterDiscount.subtract(lineTaxable).setScale(2, RoundingMode.HALF_UP);
                lineTotal = lineAfterDiscount;
            } else {
                // Price is tax-exclusive
                lineTaxable = lineAfterDiscount;
                lineTax = lineTaxable.multiply(gstRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                lineTotal = lineTaxable.add(lineTax).setScale(2, RoundingMode.HALF_UP);
            }

            sumTaxable = sumTaxable.add(lineTaxable);
            sumTotalTax = sumTotalTax.add(lineTax);

            calculatedItems.add(new ItemOutput(
                    item.productId(),
                    item.productName(),
                    item.hsnCode(),
                    qty,
                    price,
                    gstRate,
                    discount,
                    lineTaxable,
                    lineTax,
                    lineTotal
            ));
        }

        // Apply invoice-level discount if present
        if (invoiceDiscount != null && invoiceDiscount.compareTo(BigDecimal.ZERO) > 0) {
            sumTaxable = sumTaxable.subtract(invoiceDiscount);
            if (sumTaxable.compareTo(BigDecimal.ZERO) < 0) {
                sumTaxable = BigDecimal.ZERO;
            }
        }

        boolean isIntraState = false;
        if (!isExport && supplierState != null && customerState != null) {
            isIntraState = IndianTaxValidator.isIntraState(supplierState, customerState);
        }

        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;

        if (isIntraState) {
            cgst = sumTotalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            sgst = sumTotalTax.subtract(cgst);
        } else {
            igst = sumTotalTax;
        }

        // Under Reverse Charge Mechanism (RCM), buyer pays tax directly to govt,
        // so seller invoice grand total is taxable amount only.
        BigDecimal rawGrandTotal = reverseCharge
                ? sumTaxable.setScale(2, RoundingMode.HALF_UP)
                : sumTaxable.add(sumTotalTax).setScale(2, RoundingMode.HALF_UP);

        // Round off to nearest rupee
        BigDecimal roundedGrandTotal = rawGrandTotal.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        BigDecimal roundOffAmount = roundedGrandTotal.subtract(rawGrandTotal).setScale(2, RoundingMode.HALF_UP);

        String inWords = NumberToWordsConverter.convertToIndianCurrencyWords(roundedGrandTotal);

        return new GstResult(
                sumTaxable,
                cgst,
                sgst,
                igst,
                sumTotalTax,
                roundedGrandTotal,
                inWords,
                isIntraState,
                calculatedItems,
                reverseCharge,
                roundOffAmount
        );
    }
}
