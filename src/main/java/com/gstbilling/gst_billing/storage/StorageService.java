package com.gstbilling.gst_billing.storage;

/**
 * Pluggable document and file storage interface (Local disk, S3, Azure Blob, GCS).
 */
public interface StorageService {

    /**
     * Stores content at the specified logical path.
     *
     * @param path Relative path (e.g. "logos/biz-1.png", "invoices/101.pdf")
     * @param content Raw binary content
     * @param contentType MIME type (e.g. "image/png", "application/pdf")
     * @return Public or accessible URL/path of stored file
     */
    String store(String path, byte[] content, String contentType);

    /**
     * Retrieves stored binary content.
     */
    byte[] retrieve(String path);

    /**
     * Checks if a file exists at the specified path.
     */
    boolean exists(String path);

    /**
     * Deletes a file at the specified path.
     */
    boolean delete(String path);

    /**
     * Resolves accessible URL for the given path.
     */
    String getUrl(String path);
}
