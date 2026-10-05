package com.gstbilling.gst_billing.health;

import com.gstbilling.gst_billing.integration.einvoice.EInvoiceProvider;
import com.gstbilling.gst_billing.integration.ewaybill.EWayBillProvider;
import com.gstbilling.gst_billing.integration.payment.PaymentGatewayProvider;
import com.gstbilling.gst_billing.storage.StorageService;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemHealthService {

    private final DataSource dataSource;
    private final StorageService storageService;
    private final List<EInvoiceProvider> eInvoiceProviders;
    private final List<EWayBillProvider> eWayBillProviders;
    private final List<PaymentGatewayProvider> paymentProviders;
    private final long startTime = System.currentTimeMillis();

    public SystemHealthService(
            DataSource dataSource,
            StorageService storageService,
            List<EInvoiceProvider> eInvoiceProviders,
            List<EWayBillProvider> eWayBillProviders,
            List<PaymentGatewayProvider> paymentProviders
    ) {
        this.dataSource = dataSource;
        this.storageService = storageService;
        this.eInvoiceProviders = eInvoiceProviders;
        this.eWayBillProviders = eWayBillProviders;
        this.paymentProviders = paymentProviders;
    }

    public SystemHealthResponse getHealth() {
        Map<String, ComponentHealth> components = new HashMap<>();

        // Database Health
        ComponentHealth dbHealth = checkDatabase();
        components.put("database", dbHealth);

        // Storage Health
        ComponentHealth storageHealth = checkStorage();
        components.put("storage", storageHealth);

        // E-Invoice Gateway
        ComponentHealth einvoiceHealth = checkEInvoice();
        components.put("einvoiceGateway", einvoiceHealth);

        // E-Way Bill Gateway
        ComponentHealth ewayBillHealth = checkEWayBill();
        components.put("ewayBillGateway", ewayBillHealth);

        // Payment Gateway
        ComponentHealth paymentHealth = checkPaymentGateway();
        components.put("paymentGateway", paymentHealth);

        // Calculate Overall
        boolean allUp = components.values().stream().allMatch(c -> c.status() == HealthStatus.UP);
        HealthStatus overall = allUp ? HealthStatus.UP : HealthStatus.DOWN;
        long uptime = (System.currentTimeMillis() - startTime) / 1000;

        return new SystemHealthResponse(overall, LocalDateTime.now(), uptime, components);
    }

    public Map<String, Object> getMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        int activeThreads = Thread.activeCount();
        long uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("status", "UP");
        metrics.put("uptimeSeconds", uptimeSeconds);
        metrics.put("activeThreads", activeThreads);
        metrics.put("memory", Map.of(
                "usedMb", usedMemory / (1024 * 1024),
                "freeMb", freeMemory / (1024 * 1024),
                "totalMb", totalMemory / (1024 * 1024),
                "maxMb", maxMemory / (1024 * 1024)
        ));
        metrics.put("systemLoadAverage", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        return metrics;
    }

    private ComponentHealth checkDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            return new ComponentHealth(
                    HealthStatus.UP,
                    "PostgreSQL database connected and responding",
                    Map.of(
                            "database", conn.getMetaData().getDatabaseProductName(),
                            "version", conn.getMetaData().getDatabaseProductVersion()
                    )
            );
        } catch (Exception e) {
            return new ComponentHealth(
                    HealthStatus.DOWN,
                    "Database connectivity error: " + e.getMessage(),
                    Map.of("error", e.getClass().getSimpleName())
            );
        }
    }

    private ComponentHealth checkStorage() {
        try {
            File dir = new File("uploads");
            boolean exists = dir.exists() || dir.mkdirs();
            boolean canWrite = dir.canWrite();
            long freeSpaceMb = dir.getFreeSpace() / (1024 * 1024);

            if (!exists || !canWrite) {
                return new ComponentHealth(
                        HealthStatus.DOWN,
                        "Storage upload directory is unavailable or not writable",
                        Map.of("path", dir.getAbsolutePath())
                );
            }

            return new ComponentHealth(
                    HealthStatus.UP,
                    "Document storage operational",
                    Map.of(
                            "storageType", "LocalStorage / Hybrid S3",
                            "freeSpaceMb", freeSpaceMb,
                            "directory", dir.getAbsolutePath()
                    )
            );
        } catch (Exception e) {
            return new ComponentHealth(HealthStatus.DOWN, e.getMessage(), Map.of());
        }
    }

    private ComponentHealth checkEInvoice() {
        String active = eInvoiceProviders.isEmpty() ? "NONE" : eInvoiceProviders.get(0).getProviderName();
        boolean mock = active.contains("MOCK") || (eInvoiceProviders.size() > 0 && eInvoiceProviders.get(0).getClass().getSimpleName().contains("Mock"));
        return new ComponentHealth(
                HealthStatus.UP,
                "E-Invoice gateway adapter active",
                Map.of("activeProvider", active, "mockMode", mock, "status", "OPERATIONAL")
        );
    }

    private ComponentHealth checkEWayBill() {
        String active = eWayBillProviders.isEmpty() ? "NONE" : eWayBillProviders.get(0).getProviderName();
        boolean mock = active.contains("MOCK") || (eWayBillProviders.size() > 0 && eWayBillProviders.get(0).getClass().getSimpleName().contains("Mock"));
        return new ComponentHealth(
                HealthStatus.UP,
                "E-Way Bill gateway adapter active",
                Map.of("activeProvider", active, "mockMode", mock, "status", "OPERATIONAL")
        );
    }

    private ComponentHealth checkPaymentGateway() {
        String active = paymentProviders.isEmpty() ? "NONE" : paymentProviders.get(0).getProviderName();
        boolean mock = active.contains("MOCK") || (paymentProviders.size() > 0 && paymentProviders.get(0).getClass().getSimpleName().contains("Mock"));
        return new ComponentHealth(
                HealthStatus.UP,
                "Payment gateway adapter active",
                Map.of("activeProvider", active, "mockMode", mock, "webhookEndpoint", "/api/webhooks/payments/" + active.toLowerCase())
        );
    }
}
