package com.gstbilling.gst_billing.health;

import com.gstbilling.gst_billing.controller.HealthController;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ActuatorAndObservabilityTest {

    static {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired private SystemHealthService healthService;
    @Autowired private HealthController healthController;
    @Autowired(required = false) private Flyway flyway;

    @Test
    @DisplayName("SystemHealthService reports overall UP and all subsystem statuses")
    void testSystemHealthService() {
        SystemHealthResponse health = healthService.getHealth();
        assertNotNull(health);
        assertEquals(HealthStatus.UP, health.status());
        assertNotNull(health.timestamp());
        assertTrue(health.uptimeSeconds() >= 0);

        Map<String, ComponentHealth> components = health.components();
        assertTrue(components.containsKey("database"));
        assertEquals(HealthStatus.UP, components.get("database").status());

        assertTrue(components.containsKey("storage"));
        assertEquals(HealthStatus.UP, components.get("storage").status());

        assertTrue(components.containsKey("einvoiceGateway"));
        assertEquals(HealthStatus.UP, components.get("einvoiceGateway").status());
        assertEquals("IRIS_MOCK", components.get("einvoiceGateway").details().get("activeProvider"));

        assertTrue(components.containsKey("ewayBillGateway"));
        assertEquals(HealthStatus.UP, components.get("ewayBillGateway").status());
        assertEquals("EWAY_BILL_MOCK", components.get("ewayBillGateway").details().get("activeProvider"));

        assertTrue(components.containsKey("paymentGateway"));
        assertEquals(HealthStatus.UP, components.get("paymentGateway").status());
        assertEquals("RAZORPAY", components.get("paymentGateway").details().get("activeProvider"));
    }

    @Test
    @DisplayName("SystemHealthService provides memory and thread runtime metrics")
    void testSystemMetrics() {
        Map<String, Object> metrics = healthService.getMetrics();
        assertNotNull(metrics);
        assertEquals("UP", metrics.get("status"));
        assertTrue((int) metrics.get("activeThreads") > 0);
        assertTrue((long) metrics.get("uptimeSeconds") >= 0);

        @SuppressWarnings("unchecked")
        Map<String, Long> memory = (Map<String, Long>) metrics.get("memory");
        assertNotNull(memory);
        assertTrue(memory.get("totalMb") > 0);
        assertTrue(memory.get("maxMb") > 0);
    }

    @Test
    @DisplayName("HealthController exposes /api/health returning HTTP 200 OK")
    void testHealthControllerEndpoint() {
        ResponseEntity<SystemHealthResponse> resp = healthController.getHealth();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(HealthStatus.UP, resp.getBody().status());
    }

    @Test
    @DisplayName("HealthController exposes /api/metrics returning HTTP 200 OK")
    void testMetricsControllerEndpoint() {
        ResponseEntity<Map<String, Object>> resp = healthController.getMetrics();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("UP", resp.getBody().get("status"));
    }

    @Test
    @DisplayName("Flyway migration engine is successfully configured and active")
    void testFlywayEngine() {
        assertNotNull(flyway, "Flyway bean must be configured and loaded in ApplicationContext");
        assertNotNull(flyway.info());
        assertTrue(flyway.info().all().length >= 1, "Flyway should discover V1 baseline migration script");
    }
}
