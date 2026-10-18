package com.gstbilling.gst_billing.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;

/**
 * Production Cloudflare R2 object storage implementation using the AWS S3-compatible API.
 * Guarantees persistent cloud storage for invoices, receipts, logos, and signatures.
 * Never silently falls back to ephemeral local disk in production.
 */
public class R2StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);

    private final String bucketName;
    private final String endpoint;
    private final String publicUrl;
    private final S3Client s3Client;

    /**
     * Primary constructor creating an authenticated S3Client configured for Cloudflare R2.
     */
    public R2StorageService(
            String accessKeyId,
            String secretAccessKey,
            String endpoint,
            String bucketName,
            String publicUrl
    ) {
        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new StorageConfigurationException("R2_ACCESS_KEY_ID cannot be null or blank");
        }
        if (secretAccessKey == null || secretAccessKey.isBlank()) {
            throw new StorageConfigurationException("R2_SECRET_ACCESS_KEY cannot be null or blank");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new StorageConfigurationException("R2_ENDPOINT cannot be null or blank");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new StorageConfigurationException("R2_BUCKET_NAME cannot be null or blank");
        }

        this.bucketName = bucketName.trim();
        String cleanEndpoint = endpoint.trim();
        while (cleanEndpoint.endsWith("/")) {
            cleanEndpoint = cleanEndpoint.substring(0, cleanEndpoint.length() - 1);
        }
        this.endpoint = cleanEndpoint;
        this.publicUrl = (publicUrl != null && !publicUrl.isBlank()) ? publicUrl.trim() : null;

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
            log.error("Failed to initialize Cloudflare R2 S3Client: {}", e.getMessage());
            throw new StorageConfigurationException("Failed to initialize Cloudflare R2 S3Client", e);
        }
    }

    /**
     * Package-private constructor for unit testing with mocked S3Client.
     */
    public R2StorageService(S3Client s3Client, String bucketName, String endpoint, String publicUrl) {
        if (s3Client == null) {
            throw new IllegalArgumentException("S3Client cannot be null");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("Bucket name cannot be null or blank");
        }
        this.s3Client = s3Client;
        this.bucketName = bucketName.trim();
        this.endpoint = (endpoint != null) ? endpoint.trim() : "";
        this.publicUrl = (publicUrl != null && !publicUrl.isBlank()) ? publicUrl.trim() : null;
    }

    private String sanitizePath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Storage path cannot be null");
        }
        if (path.contains("..")) {
            throw new IllegalArgumentException("Path traversal attempt detected in path: " + path);
        }
        String clean = path.replace("\\", "/");
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        if (clean.isBlank()) {
            throw new IllegalArgumentException("Storage path cannot be empty");
        }
        return clean;
    }

    @Override
    public String store(String path, byte[] content, String contentType) {
        String cleanPath = sanitizePath(path);
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Cannot store null or empty content for object key: " + cleanPath);
        }

        String effectiveContentType = (contentType != null && !contentType.isBlank())
                ? contentType.trim()
                : "application/octet-stream";

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanPath)
                    .contentType(effectiveContentType)
                    .contentLength((long) content.length)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(content));
            log.debug("Successfully stored object in Cloudflare R2: {}/{} ({} bytes, type={})",
                    bucketName, cleanPath, content.length, effectiveContentType);
            return getUrl(cleanPath);
        } catch (AwsServiceException e) {
            log.error("Cloudflare R2 service rejected PutObject: bucket={}, key={}, statusCode={}, error={}",
                    bucketName, cleanPath, e.statusCode(), e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            throw new StorageException("Failed to store object in Cloudflare R2: " + cleanPath + " (status " + e.statusCode() + ")", e);
        } catch (SdkException e) {
            log.error("SDK/network failure during Cloudflare R2 PutObject: bucket={}, key={}, error={}",
                    bucketName, cleanPath, e.getMessage());
            throw new StorageException("Network failure while storing object in Cloudflare R2: " + cleanPath, e);
        } catch (Exception e) {
            log.error("Unexpected error storing object in Cloudflare R2: bucket={}, key={}, error={}",
                    bucketName, cleanPath, e.getMessage());
            throw new StorageException("Unexpected error storing object in Cloudflare R2: " + cleanPath, e);
        }
    }

    @Override
    public byte[] retrieve(String path) {
        String cleanPath = sanitizePath(path);

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanPath)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
            byte[] bytes = objectBytes.asByteArray();
            log.debug("Retrieved object from Cloudflare R2: {}/{} ({} bytes)", bucketName, cleanPath, bytes.length);
            return bytes;
        } catch (NoSuchKeyException e) {
            log.debug("Object not found in Cloudflare R2: {}/{}", bucketName, cleanPath);
            return null;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.debug("Cloudflare R2 returned 404 Not Found for key: {}/{}", bucketName, cleanPath);
                return null;
            }
            if (e.statusCode() == 403) {
                log.error("Access denied retrieving object from Cloudflare R2: bucket={}, key={}", bucketName, cleanPath);
                throw new StorageException("Access denied retrieving object from Cloudflare R2: " + cleanPath, e);
            }
            log.error("Cloudflare R2 service error during GetObject: bucket={}, key={}, statusCode={}, error={}",
                    bucketName, cleanPath, e.statusCode(), e.getMessage());
            throw new StorageException("Failed to retrieve object from Cloudflare R2: " + cleanPath + " (status " + e.statusCode() + ")", e);
        } catch (SdkException e) {
            log.error("SDK/network error retrieving object from Cloudflare R2: bucket={}, key={}, error={}",
                    bucketName, cleanPath, e.getMessage());
            throw new StorageException("Network failure while retrieving object from Cloudflare R2: " + cleanPath, e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving object from Cloudflare R2: bucket={}, key={}, error={}",
                    bucketName, cleanPath, e.getMessage());
            throw new StorageException("Unexpected error retrieving object from Cloudflare R2: " + cleanPath, e);
        }
    }

    @Override
    public boolean exists(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String cleanPath = sanitizePath(path);

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanPath)
                    .build();

            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.warn("Cloudflare R2 HeadObject returned status {}: bucket={}, key={}", e.statusCode(), bucketName, cleanPath);
            throw new StorageException("Error checking existence in Cloudflare R2: " + cleanPath + " (status " + e.statusCode() + ")", e);
        } catch (Exception e) {
            log.warn("Network error during HeadObject in Cloudflare R2: bucket={}, key={}, error={}", bucketName, cleanPath, e.getMessage());
            throw new StorageException("Network error checking existence in Cloudflare R2: " + cleanPath, e);
        }
    }

    @Override
    public boolean delete(String path) {
        String cleanPath = sanitizePath(path);

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanPath)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.debug("Deleted object from Cloudflare R2: {}/{}", bucketName, cleanPath);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.error("Failed to delete object from Cloudflare R2: bucket={}, key={}, statusCode={}, error={}",
                    bucketName, cleanPath, e.statusCode(), e.getMessage());
            throw new StorageException("Failed to delete object from Cloudflare R2: " + cleanPath + " (status " + e.statusCode() + ")", e);
        } catch (Exception e) {
            log.error("Error deleting object from Cloudflare R2: bucket={}, key={}, error={}",
                    bucketName, cleanPath, e.getMessage());
            throw new StorageException("Failed to delete object from Cloudflare R2: " + cleanPath, e);
        }
    }

    @Override
    public String getUrl(String path) {
        String cleanPath = sanitizePath(path);
        if (publicUrl != null && !publicUrl.isBlank()) {
            String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            return base + "/" + cleanPath;
        }
        // When no public CDN URL is configured, route via application's authenticated upload/download endpoint
        return "/uploads/" + cleanPath;
    }

    @Override
    public String getProviderName() {
        return "R2";
    }

    @Override
    public boolean isHealthy() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (Exception e) {
            log.warn("Cloudflare R2 health check failed for bucket '{}': {}", bucketName, e.getMessage());
            return false;
        }
    }
}
