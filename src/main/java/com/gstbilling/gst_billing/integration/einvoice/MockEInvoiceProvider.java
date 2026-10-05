package com.gstbilling.gst_billing.integration.einvoice;

import com.gstbilling.gst_billing.dto.einvoice.EInvoiceResult;
import com.gstbilling.gst_billing.entity.Invoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "app.einvoice.provider", havingValue = "mock", matchIfMissing = true)
public class MockEInvoiceProvider implements EInvoiceProvider {

    private final AtomicLong ackCounter = new AtomicLong(10000000000L);

    @Value("${app.einvoice.mock.simulate-latency-ms:0}")
    private long simulateLatencyMs;

    @Value("${app.einvoice.mock.failure-rate:0.0}")
    private double failureRate;

    public MockEInvoiceProvider() {}

    @Override
    public String getProviderName() {
        return "IRIS_MOCK";
    }

    @Override
    public EInvoiceResult generateIrn(Invoice invoice) {
        applySimulatedLatency();

        if (failureRate > 0 && Math.random() < failureRate) {
            return EInvoiceResult.failure("Simulated IRP gateway connection timeout. Retry later.");
        }

        String supplierGstin = (invoice.getBusiness() != null && invoice.getBusiness().getGstin() != null)
                ? invoice.getBusiness().getGstin() : "27AABCB1234A1Z5";
        String customerGstin = (invoice.getCustomer() != null && invoice.getCustomer().getGstin() != null)
                ? invoice.getCustomer().getGstin() : "27DEFGH5678B1Z2";
        String fy = invoice.getFinancialYear() != null ? invoice.getFinancialYear() : "2026-27";
        String docType = "INV";
        String docNo = invoice.getInvoiceNumber();

        // 1. Generate deterministic 64-char SHA-256 IRN according to NIC schema
        String rawData = supplierGstin + ":" + fy + ":" + docType + ":" + docNo;
        String irn = sha256Hex(rawData);

        // 2. Generate Acknowledgement Number & Date
        long ackNo = 110000000000000L + ackCounter.incrementAndGet();
        LocalDateTime ackDate = LocalDateTime.now();

        // 3. Generate NIC-compliant signed QR Code payload
        String mainHsn = (invoice.getItems() != null && !invoice.getItems().isEmpty() && invoice.getItems().get(0).getHsnCode() != null)
                ? invoice.getItems().get(0).getHsnCode() : "8471";
        int itemCount = (invoice.getItems() != null) ? invoice.getItems().size() : 1;

        String docDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        double totVal = invoice.getGrandTotal() != null ? invoice.getGrandTotal().doubleValue() : 0.0;

        String signedQrCode = String.format(
                "{\"SellerGSTIN\":\"%s\",\"BuyerGSTIN\":\"%s\",\"DocNo\":\"%s\",\"DocTyp\":\"%s\",\"DocDt\":\"%s\",\"TotInvVal\":%.2f,\"ItemCnt\":%d,\"MainHsnCode\":\"%s\",\"Irn\":\"%s\"}",
                supplierGstin, customerGstin, docNo, docType, docDate, totVal, itemCount, mainHsn, irn
        );

        String signedInvoice = "JWT.MOCK_SIGNED_INVOICE_PAYLOAD." + irn.substring(0, 16);

        return EInvoiceResult.success(irn, String.valueOf(ackNo), ackDate, signedInvoice, signedQrCode);
    }

    @Override
    public boolean cancelIrn(String irn, int reasonCode, String remarks) {
        applySimulatedLatency();
        // Mock cancellation always succeeds if IRN is valid
        return irn != null && irn.length() == 64;
    }

    @Override
    public EInvoiceResult getEInvoiceDetails(String irn) {
        applySimulatedLatency();
        return EInvoiceResult.success(irn, "119999999999999", LocalDateTime.now(), "MOCK_SIGNED_PAYLOAD", "{\"Irn\":\"" + irn + "\"}");
    }

    private void applySimulatedLatency() {
        if (simulateLatencyMs > 0) {
            try {
                Thread.sleep(simulateLatencyMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
