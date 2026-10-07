package com.gstbilling.gst_billing.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;

/**
 * Cloudflare R2 object storage implementation using AWS S3-compatible API.
 * Automatically connects to Cloudflare R2 via R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY,
 * R2_ENDPOINT, and R2_BUCKET_NAME environment variables.
 */
@Service
@ConditionalOnExpression(
    "'${app.storage.provider:auto}'.equalsIgnoreCase('r2') || " +
    "('${app.storage.provider:auto}'.equalsIgnoreCase('auto') && !'${R2_ACCESS_KEY_ID:${r2.access-key-id:}}'.isEmpty())"
)
public class R2StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);

    private final String bucketName;
    private final String endpoint;
    private final String publicUrl;
    private final S3Client s3Client;
    private final LocalStorageService fallbackStorage;

    public R2StorageService(
            @Value("${r2.access-key-id:${R2_ACCESS_KEY_ID:}}") String accessKeyId,
            @Value("${r2.secret-access-key:${R2_SECRET_ACCESS_KEY:}}") String secretAccessKey,
            @Value("${r2.endpoint:${R2_ENDPOINT:}}") String endpoint,
            @Value("${r2.bucket-name:${R2_BUCKET_NAME:}}") String bucketName,
            @Value("${r2.public-url:${R2_PUBLIC_URL:}}") String publicUrl,
            @Value("${app.storage.local.base-dir:uploads}") String localBaseDir
    ) {
        this.bucketName = (bucketName != null) ? bucketName.trim() : "";
        this.endpoint = (endpoint != null) ? endpoint.trim() : "";
        this.publicUrl = (publicUrl != null && !publicUrl.isBlank()) ? publicUrl.trim() : null;
        this.fallbackStorage = new LocalStorageService(localBaseDir);

        if (isConfigured(accessKeyId, secretAccessKey, this.endpoint, this.bucketName)) {
            try {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim());
                this.s3Client = S3Client.builder()
                        .endpointOverride(URI.create(this.endpoint))
                        .credentialsProvider(StaticCredentialsProvider.create(credentials))
                        .region(Region.of("auto"))
                        .serviceConfiguration(S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                        .build();
                log.info("Initialized Cloudflare R2 storage client for endpoint: {} and bucket: {}", this.endpoint, this.bucketName);
            } catch (Exception e) {
                log.error("Failed to initialize Cloudflare R2 S3Client, falling back to local disk storage", e);
                throw new IllegalStateException("Failed to configure Cloudflare R2 client", e);
            }
        } else {
            this.s3Client = null;
            log.warn("Cloudflare R2 credentials or endpoint not fully configured. Requests will fall back to local storage.");
        }
    }

    private boolean isConfigured(String accessKey, String secretKey, String endpointUrl, String bucket) {
        return accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()
                && endpointUrl != null && !endpointUrl.isBlank()
                && bucket != null && !bucket.isBlank();
    }

    private String sanitizePath(String path) {
        if (path == null || path.contains("..")) {
            throw new IllegalArgumentException("Invalid path or path traversal detected: " + path);
        }
        String clean = path.replace("\\", "/");
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        return clean;
    }

    @Override
    public String store(String path, byte[] content, String contentType) {
        String cleanPath = sanitizePath(path);
        if (s3Client == null) {
            return fallbackStorage.store(cleanPath, content, contentType);
        }

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanPath)
                    .contentType((contentType != null && !contentType.isBlank()) ? contentType : "application/octet-stream")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(content));
            log.debug("Successfully stored object in Cloudflare R2: {}/{}", bucketName, cleanPath);
            return getUrl(cleanPath);
        } catch (Exception e) {
            log.error("Failed to store object in Cloudflare R2 key: {}, falling back to local disk", cleanPath, e);
            return fallbackStorage.store(cleanPath, content, contentType);
        }
    }

    @Override
    public byte[] retrieve(String path) {
        String cleanPath = sanitizePath(path);
        if (s3Client == null) {
            return fallbackStorage.retrieve(cleanPath);
        }

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanPath)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
            return objectBytes.asByteArray();
        } catch (NoSuchKeyException e) {
            log.debug("Object not found in Cloudflare R2: {}/{}", bucketName, cleanPath);
            return fallbackStorage.retrieve(cleanPath);
        } catch (Exception e) {
            log.warn("Error retrieving from Cloudflare R2 key: {}, falling back to local disk: {}", cleanPath, e.getMessage());
            return fallbackStorage.retrieve(cleanPath);
        }
    }

    @Override
    public boolean exists(String path) {
        String cleanPath = sanitizePath(path);
        if (s3Client == null) {
            return fallbackStorage.exists(cleanPath);
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanPath)
                    .build();

            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return fallbackStorage.exists(cleanPath);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return fallbackStorage.exists(cleanPath);
            }
            log.warn("Cloudflare R2 HeadObject error for key: {}: {}", cleanPath, e.getMessage());
            return fallbackStorage.exists(cleanPath);
        } catch (Exception e) {
            return fallbackStorage.exists(cleanPath);
        }
    }

    @Override
    public boolean delete(String path) {
        String cleanPath = sanitizePath(path);
        boolean deletedLocal = fallbackStorage.delete(cleanPath);
        if (s3Client == null) {
            return deletedLocal;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanPath)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.debug("Deleted object from Cloudflare R2: {}/{}", bucketName, cleanPath);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete object from Cloudflare R2 key: {}", cleanPath, e);
            return deletedLocal;
        }
    }

    @Override
    public String getUrl(String path) {
        String cleanPath = sanitizePath(path);
        if (publicUrl != null) {
            String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            return base + "/" + cleanPath;
        }
        if (endpoint != null && !endpoint.isBlank() && !bucketName.isBlank()) {
            String baseEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
            return baseEndpoint + "/" + bucketName + "/" + cleanPath;
        }
        return fallbackStorage.getUrl(cleanPath);
    }
}
