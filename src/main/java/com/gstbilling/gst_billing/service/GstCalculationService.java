package com.gstbilling.gst_billing.service;

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
            List<ItemOutput> items
    ) {}

    public GstResult calculate(
            String supplierState,
            String customerState,
            boolean isExport,
            BigDecimal invoiceDiscount,
            List<ItemInput> itemInputs
    ) {
        BigDecimal sumTaxable = BigDecimal.ZERO;
        BigDecimal sumTotalTax = BigDecimal.ZERO;
        List<ItemOutput> calculatedItems = new ArrayList<>();

        for (ItemInput item : itemInputs) {
            BigDecimal qty = (item.quantity() != null && item.quantity().compareTo(BigDecimal.ZERO) > 0)
                    ? item.quantity()
                    : BigDecimal.ONE;

            BigDecimal price = item.unitPrice() != null ? item.unitPrice() : BigDecimal.ZERO;
            BigDecimal gstRate = item.gstRate() != null ? item.gstRate() : BigDecimal.ZERO;
            BigDecimal discount = item.discount() != null ? item.discount() : BigDecimal.ZERO;

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
                BigDecimal divisor = BigDecimal.ONE.add(gstRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
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
            isIntraState = supplierState.trim().equalsIgnoreCase(customerState.trim());
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

        BigDecimal grandTotal = sumTaxable.add(sumTotalTax).setScale(2, RoundingMode.HALF_UP);
        String inWords = NumberToWordsConverter.convertToIndianCurrencyWords(grandTotal);

        return new GstResult(
                sumTaxable,
                cgst,
                sgst,
                igst,
                sumTotalTax,
                grandTotal,
                inWords,
                isIntraState,
                calculatedItems
        );
    }
}
