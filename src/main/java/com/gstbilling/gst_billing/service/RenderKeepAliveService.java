package com.gstbilling.gst_billing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service to periodically ping the public Render deployment URL every 60 seconds
 * to prevent the free-tier container from spinning down due to inactivity.
 *
 * Render provides RENDER_EXTERNAL_URL automatically in production web services.
 */
@Service
@EnableScheduling
public class RenderKeepAliveService {

    private static final Logger log = LoggerFactory.getLogger(RenderKeepAliveService.class);

    private final String pingUrl;
    private final HttpClient httpClient;

    public RenderKeepAliveService(
            @Value("${app.ping.url:${app.render.external-url:}}") String externalUrl
    ) {
        if (externalUrl != null && !externalUrl.isBlank()) {
            String baseUrl = externalUrl.trim();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            this.pingUrl = baseUrl.endsWith("/api") ? baseUrl + "/ping" : baseUrl + "/api/ping";
        } else {
            this.pingUrl = null;
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        if (this.pingUrl != null) {
            log.info("Render keep-alive service initialized. Target URL: {}", this.pingUrl);
        } else {
            log.info("Render keep-alive service is idle (RENDER_EXTERNAL_URL or app.ping.url not configured).");
        }
    }

    /**
     * Executes every 60 seconds (60,000 ms) with an initial delay of 30 seconds.
     */
    @Scheduled(fixedRateString = "${app.ping.interval-ms:60000}", initialDelay = 30000)
    public void pingSelf() {
        if (pingUrl == null || pingUrl.isBlank()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pingUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Render-KeepAlive-Worker")
                    .header("Cache-Control", "no-cache")
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("Keep-alive ping sent to {} - Status: {}", pingUrl, response.statusCode());
            } else {
                log.warn("Keep-alive ping to {} returned unexpected status: {}", pingUrl, response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Keep-alive ping to {} failed: {}", pingUrl, e.getMessage());
        }
    }

    public String getPingUrl() {
        return pingUrl;
    }
}
