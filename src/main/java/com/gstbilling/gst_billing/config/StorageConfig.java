package com.gstbilling.gst_billing.config;

import com.gstbilling.gst_billing.storage.LocalStorageService;
import com.gstbilling.gst_billing.storage.R2StorageService;
import com.gstbilling.gst_billing.storage.StorageConfigurationException;
import com.gstbilling.gst_billing.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring configuration defining the primary StorageService bean.
 * Guarantees fail-fast validation when Cloudflare R2 is explicitly chosen in production,
 * and predictable fallback to local storage for local development.
 */
@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    public StorageService storageService(
            @Value("${app.storage.provider:auto}") String provider,
            @Value("${r2.access-key-id:${R2_ACCESS_KEY_ID:}}") String accessKeyId,
            @Value("${r2.secret-access-key:${R2_SECRET_ACCESS_KEY:}}") String secretAccessKey,
            @Value("${r2.endpoint:${R2_ENDPOINT:}}") String endpoint,
            @Value("${r2.bucket-name:${R2_BUCKET_NAME:}}") String bucketName,
            @Value("${r2.public-url:${R2_PUBLIC_URL:}}") String publicUrl,
            @Value("${app.storage.local.base-dir:uploads}") String localBaseDir
    ) {
        String cleanProvider = (provider != null) ? provider.trim().toLowerCase() : "auto";

        boolean hasAccessKey = accessKeyId != null && !accessKeyId.isBlank();
        boolean hasSecretKey = secretAccessKey != null && !secretAccessKey.isBlank();
        boolean hasEndpoint = endpoint != null && !endpoint.isBlank();
        boolean hasBucket = bucketName != null && !bucketName.isBlank();
        boolean allR2Configured = hasAccessKey && hasSecretKey && hasEndpoint && hasBucket;

        if ("r2".equals(cleanProvider)) {
            List<String> missing = new ArrayList<>();
            if (!hasAccessKey) missing.add("R2_ACCESS_KEY_ID (or r2.access-key-id)");
            if (!hasSecretKey) missing.add("R2_SECRET_ACCESS_KEY (or r2.secret-access-key)");
            if (!hasEndpoint) missing.add("R2_ENDPOINT (or r2.endpoint)");
            if (!hasBucket) missing.add("R2_BUCKET_NAME (or r2.bucket-name)");

            if (!missing.isEmpty()) {
                String errorMsg = "Cloudflare R2 is explicitly selected (APP_STORAGE_PROVIDER=r2), " +
                        "but the following required configuration variables are missing or empty: " + missing + ". " +
                        "Application startup aborted to prevent silent fallback to ephemeral storage in production.";
                log.error(errorMsg);
                throw new StorageConfigurationException(errorMsg);
            }

            log.info("Storage provider initialized: Cloudflare R2 (persistent bucket: '{}', publicUrl: '{}')",
                    bucketName.trim(), (publicUrl != null && !publicUrl.isBlank()) ? publicUrl.trim() : "none (private)");
            return new R2StorageService(accessKeyId, secretAccessKey, endpoint, bucketName, publicUrl);

        } else if ("local".equals(cleanProvider)) {
            log.info("Storage provider initialized: Local disk storage (base directory: '{}')", localBaseDir);
            return new LocalStorageService(localBaseDir);

        } else if ("auto".equals(cleanProvider)) {
            if (allR2Configured) {
                log.info("Storage provider auto-detection: Cloudflare R2 credentials verified. Using Cloudflare R2 bucket '{}'.", bucketName.trim());
                return new R2StorageService(accessKeyId, secretAccessKey, endpoint, bucketName, publicUrl);
            }

            if (hasAccessKey || hasSecretKey || hasEndpoint || hasBucket) {
                List<String> missing = new ArrayList<>();
                if (!hasAccessKey) missing.add("R2_ACCESS_KEY_ID");
                if (!hasSecretKey) missing.add("R2_SECRET_ACCESS_KEY");
                if (!hasEndpoint) missing.add("R2_ENDPOINT");
                if (!hasBucket) missing.add("R2_BUCKET_NAME");
                log.warn("Storage provider is set to 'auto', but Cloudflare R2 is only partially configured (missing: {}). " +
                        "Falling back to local disk storage for development.", missing);
            } else {
                log.info("Storage provider auto-detection: No Cloudflare R2 credentials detected. Using local disk storage.");
            }
            return new LocalStorageService(localBaseDir);

        } else {
            throw new StorageConfigurationException("Unsupported storage provider: '" + provider +
                    "'. Valid options are 'r2', 'local', or 'auto'.");
        }
    }
}
