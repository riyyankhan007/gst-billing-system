package com.gstbilling.gst_billing.storage;

/**
 * Base runtime exception for all document and object storage failures.
 * Preserves the original cause while keeping client-facing error messages sanitized.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
