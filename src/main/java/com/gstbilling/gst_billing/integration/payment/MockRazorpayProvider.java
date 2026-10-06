package com.gstbilling.gst_billing.integration.payment;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MockRazorpayProvider implements PaymentGatewayProvider {

    private static final Pattern EVENT_PATTERN = Pattern.compile("\"event\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("\"order_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PAYMENT_ID_PATTERN = Pattern.compile("\"(?:payment_id|id)\"\\s*:\\s*\"(pay_[^\"]+)\"");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\"amount\"\\s*:\\s*(\\d+)");
    private static final Pattern INVOICE_ID_PATTERN = Pattern.compile("\"invoiceId\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Pattern BUSINESS_ID_PATTERN = Pattern.compile("\"businessId\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Pattern METHOD_PATTERN = Pattern.compile("\"method\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }

    @Override
    public PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request) {
        String uniqueSuffix = Long.toHexString(System.currentTimeMillis()) + (request.invoiceId() != null ? "_" + request.invoiceId() : "");
        String orderId = "order_rzp_mock_" + uniqueSuffix;
        String checkoutUrl = "https://api.razorpay.com/v1/checkout/mock?order_id=" + orderId;

        return new PaymentOrderResponse(
                orderId,
                request.amount(),
                request.currency() != null ? request.currency() : "INR",
                "RAZORPAY",
                checkoutUrl,
                "rzp_test_mockKeyId123",
                "created"
        );
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        if (signature == null || signature.isBlank()) {
            return false;
        }

        // Mock mode shortcut for testing and development
        if ("mock_valid_signature".equals(signature) || signature.startsWith("mock_sig_")) {
            return true;
        }

        if (secret == null || secret.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return MessageDigest.isEqual(hex.toString().getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PaymentWebhookEvent parseWebhookEvent(String payload) {
        String eventType = extractPattern(EVENT_PATTERN, payload, "payment.captured");
        String orderId = extractPattern(ORDER_ID_PATTERN, payload, "order_rzp_mock_default");
        String paymentId = extractPattern(PAYMENT_ID_PATTERN, payload, "pay_mock_" + System.currentTimeMillis());

        BigDecimal amount = BigDecimal.ZERO;
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(payload);
        if (amountMatcher.find()) {
            try {
                // Razorpay amounts in webhook are usually in paise (1 INR = 100 paise)
                long paise = Long.parseLong(amountMatcher.group(1));
                amount = BigDecimal.valueOf(paise).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } catch (Exception ignored) {}
        }

        Map<String, String> notes = new HashMap<>();
        String invoiceIdStr = extractPattern(INVOICE_ID_PATTERN, payload, null);
        if (invoiceIdStr != null) {
            notes.put("invoiceId", invoiceIdStr);
        }
        String businessIdStr = extractPattern(BUSINESS_ID_PATTERN, payload, null);
        if (businessIdStr != null) {
            notes.put("businessId", businessIdStr);
        }
        String methodStr = extractPattern(METHOD_PATTERN, payload, "UPI");
        notes.put("method", methodStr);

        return new PaymentWebhookEvent(
                eventType,
                orderId,
                paymentId,
                amount,
                "INR",
                "SUCCESS",
                null,
                notes
        );
    }

    private String extractPattern(Pattern pattern, String text, String defaultValue) {
        if (text == null) return defaultValue;
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return defaultValue;
    }
}
