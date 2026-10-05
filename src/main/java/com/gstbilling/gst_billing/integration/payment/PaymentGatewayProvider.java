package com.gstbilling.gst_billing.integration.payment;

public interface PaymentGatewayProvider {
    String getProviderName();

    PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request);

    boolean verifyWebhookSignature(String payload, String signature, String secret);

    PaymentWebhookEvent parseWebhookEvent(String payload);
}
