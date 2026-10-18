package com.gstbilling.gst_billing.storage;

/**
 * Thrown during application initialization when a storage provider (such as Cloudflare R2)
 * is explicitly configured but is missing required environment variables or credentials.
 */
public class StorageConfigurationException extends StorageException {

    public StorageConfigurationException(String message) {
        super(message);
    }

    public StorageConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
