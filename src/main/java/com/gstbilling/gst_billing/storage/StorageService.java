package com.gstbilling.gst_billing.storage;

/**
 * Pluggable document and file storage interface (Local disk, Cloudflare R2 / S3-compatible).
 */
public interface StorageService {

    /**
     * Stores content at the specified logical path.
     *
     * @param path Relative path (e.g. "logos/business-1-logo.png", "invoices/1/101.pdf")
     * @param content Raw binary content (must be non-null and non-empty)
     * @param contentType MIME type (e.g. "image/png", "application/pdf")
     * @return Accessible URL or storage path of stored file
     */
    String store(String path, byte[] content, String contentType);

    /**
     * Retrieves stored binary content.
     *
     * @param path Relative logical path
     * @return Raw binary bytes, or null if object does not exist
     */
    byte[] retrieve(String path);

    /**
     * Checks if a file exists at the specified path.
     *
     * @param path Relative logical path
     * @return true if object exists, false otherwise
     */
    boolean exists(String path);

    /**
     * Deletes a file at the specified path.
     *
     * @param path Relative logical path
     * @return true if deleted, false if file did not exist
     */
    boolean delete(String path);

    /**
     * Resolves accessible URL for the given path.
     * For public assets with R2_PUBLIC_URL configured, returns the public CDN URL.
     * For private assets or unconfigured CDN, returns the application-relative endpoint.
     */
    String getUrl(String path);

    /**
     * Returns the name/type of the active storage provider (e.g. "R2" or "LOCAL").
     */
    default String getProviderName() {
        return "UNKNOWN";
    }

    /**
     * Safe diagnostic health check verifying connectivity to the storage backend
     * without exposing secrets or leaking private documents.
     */
    default boolean isHealthy() {
        return true;
    }
}
