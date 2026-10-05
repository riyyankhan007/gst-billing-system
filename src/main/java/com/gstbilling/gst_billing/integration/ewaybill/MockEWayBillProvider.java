package com.gstbilling.gst_billing.integration.ewaybill;

import com.gstbilling.gst_billing.dto.ewaybill.EWayBillGenerateRequest;
import com.gstbilling.gst_billing.dto.ewaybill.EWayBillResult;
import com.gstbilling.gst_billing.dto.ewaybill.VehicleUpdateRequest;
import com.gstbilling.gst_billing.entity.Invoice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "app.ewaybill.provider", havingValue = "mock", matchIfMissing = true)
public class MockEWayBillProvider implements EWayBillProvider {

    private final AtomicLong ewbCounter = new AtomicLong(1000000000L);

    @Override
    public String getProviderName() {
        return "EWAY_BILL_MOCK";
    }

    @Override
    public EWayBillResult generateEWayBill(Invoice invoice, EWayBillGenerateRequest request) {
        // 1. Generate realistic 12-digit E-Way Bill number (e.g. 27 + 10 digits)
        long seq = ewbCounter.incrementAndGet();
        String ewbNo = String.format("27%010d", seq);

        LocalDateTime ewbDate = LocalDateTime.now();

        // 2. Calculate validity according to Indian GST rules:
        // Regular Cargo: 1 day per 200 km (or part thereof)
        // Over-Dimensional Cargo: 1 day per 20 km (or part thereof)
        int distance = (request.distanceKm() != null && request.distanceKm() > 0) ? request.distanceKm() : 100;
        boolean isOverDimensional = "OVER_DIMENSIONAL".equalsIgnoreCase(request.vehicleType());
        int kmPerDay = isOverDimensional ? 20 : 200;

        int validityDays = (int) Math.ceil((double) distance / kmPerDay);
        if (validityDays < 1) {
            validityDays = 1;
        }

        LocalDateTime validUpto = ewbDate.plusDays(validityDays).withHour(23).withMinute(59).withSecond(59);

        return EWayBillResult.success(ewbNo, ewbDate, validUpto);
    }

    @Override
    public boolean cancelEWayBill(String ewbNo, int reasonCode, String remarks) {
        return ewbNo != null && ewbNo.length() == 12;
    }

    @Override
    public boolean updateVehicleNumber(String ewbNo, VehicleUpdateRequest request) {
        return ewbNo != null && request != null && request.vehicleNumber() != null;
    }
}
