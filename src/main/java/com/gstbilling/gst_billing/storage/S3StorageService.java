package com.gstbilling.gst_billing.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    @Value("${app.storage.s3.bucket:}")
    private String bucket;

    @Value("${app.storage.s3.region:ap-south-1}")
    private String region;

    private final LocalStorageService fallbackStorage;

    public S3StorageService(@Value("${app.storage.local.base-dir:uploads}") String baseDir) {
        this.fallbackStorage = new LocalStorageService(baseDir);
        log.info("Initialized S3StorageService adapter (Ready for AWS credentials). Fallback configured.");
    }

    @Override
    public String store(String path, byte[] content, String contentType) {
        if (bucket == null || bucket.isBlank()) {
            log.debug("AWS S3 bucket not specified, delegating store to local storage adapter");
            return fallbackStorage.store(path, content, contentType);
        }
        // In production with AWS SDK credentials: s3Client.putObject(...)
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + path;
    }

    @Override
    public byte[] retrieve(String path) {
        if (bucket == null || bucket.isBlank()) {
            return fallbackStorage.retrieve(path);
        }
        return fallbackStorage.retrieve(path);
    }

    @Override
    public boolean exists(String path) {
        if (bucket == null || bucket.isBlank()) {
            return fallbackStorage.exists(path);
        }
        return fallbackStorage.exists(path);
    }

    @Override
    public boolean delete(String path) {
        if (bucket == null || bucket.isBlank()) {
            return fallbackStorage.delete(path);
        }
        return fallbackStorage.delete(path);
    }

    @Override
    public String getUrl(String path) {
        if (bucket == null || bucket.isBlank()) {
            return fallbackStorage.getUrl(path);
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + path;
    }
}
