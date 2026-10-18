package com.gstbilling.gst_billing.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Local disk implementation of StorageService used for development, testing,
 * and environments where Cloudflare R2 is explicitly not enabled.
 */
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path baseDirectory;

    public LocalStorageService(String baseDir) {
        String dir = (baseDir != null && !baseDir.isBlank()) ? baseDir.trim() : "uploads";
        this.baseDirectory = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDirectory);
            log.info("Initialized local disk storage directory at: {}", this.baseDirectory);
        } catch (IOException e) {
            throw new StorageConfigurationException("Failed to initialize local storage directory: " + this.baseDirectory, e);
        }
    }

    private String sanitizePath(String relativePath) {
        if (relativePath == null) {
            throw new IllegalArgumentException("Storage path cannot be null");
        }
        if (relativePath.contains("..")) {
            throw new IllegalArgumentException("Path traversal attempt detected in path: " + relativePath);
        }
        String clean = relativePath.replace("\\", "/");
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

    private Path resolveAndEnsureParent(String cleanPath) {
        Path target = baseDirectory.resolve(cleanPath).normalize();
        if (!target.startsWith(baseDirectory)) {
            throw new SecurityException("Attempted path traversal outside storage base directory: " + cleanPath);
        }
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new StorageException("Could not create parent directories for file: " + cleanPath, e);
            }
        }
        return target;
    }

    @Override
    public String store(String relativePath, byte[] content, String contentType) {
        String cleanPath = sanitizePath(relativePath);
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Cannot store null or empty content for file: " + cleanPath);
        }
        Path targetFile = resolveAndEnsureParent(cleanPath);
        try {
            Files.write(targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("Stored local file at: {} ({} bytes)", targetFile, content.length);
            return getUrl(cleanPath);
        } catch (IOException e) {
            throw new StorageException("Failed to store file on disk at: " + cleanPath, e);
        }
    }

    @Override
    public byte[] retrieve(String relativePath) {
        String cleanPath = sanitizePath(relativePath);
        Path targetFile = baseDirectory.resolve(cleanPath).normalize();
        if (!targetFile.startsWith(baseDirectory) || !Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
            return null;
        }
        try {
            return Files.readAllBytes(targetFile);
        } catch (IOException e) {
            throw new StorageException("Failed to read file from disk at: " + cleanPath, e);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String cleanPath = sanitizePath(relativePath);
        Path targetFile = baseDirectory.resolve(cleanPath).normalize();
        return targetFile.startsWith(baseDirectory) && Files.exists(targetFile) && Files.isRegularFile(targetFile);
    }

    @Override
    public boolean delete(String relativePath) {
        String cleanPath = sanitizePath(relativePath);
        Path targetFile = baseDirectory.resolve(cleanPath).normalize();
        if (!targetFile.startsWith(baseDirectory)) {
            return false;
        }
        try {
            return Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            log.error("Failed to delete local file at {}: {}", targetFile, e.getMessage());
            return false;
        }
    }

    @Override
    public String getUrl(String relativePath) {
        String cleanPath = sanitizePath(relativePath);
        return "/uploads/" + cleanPath;
    }

    @Override
    public String getProviderName() {
        return "LOCAL";
    }

    @Override
    public boolean isHealthy() {
        return Files.isDirectory(baseDirectory) && Files.isWritable(baseDirectory);
    }
}
