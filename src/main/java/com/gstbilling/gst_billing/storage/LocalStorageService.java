package com.gstbilling.gst_billing.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path baseDirectory;

    public LocalStorageService(@Value("${app.storage.local.base-dir:uploads}") String baseDir) {
        this.baseDirectory = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize local storage directory: " + this.baseDirectory, e);
        }
    }

    @Override
    public String store(String relativePath, byte[] content, String contentType) {
        validatePath(relativePath);
        Path targetFile = resolveAndEnsureParent(relativePath);
        try {
            Files.write(targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "/" + baseDirectory.getFileName().toString() + "/" + relativePath.replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file at: " + relativePath, e);
        }
    }

    @Override
    public byte[] retrieve(String relativePath) {
        validatePath(relativePath);
        Path targetFile = baseDirectory.resolve(relativePath).normalize();
        if (!Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
            return null;
        }
        try {
            return Files.readAllBytes(targetFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file at: " + relativePath, e);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        validatePath(relativePath);
        Path targetFile = baseDirectory.resolve(relativePath).normalize();
        return Files.exists(targetFile) && Files.isRegularFile(targetFile);
    }

    @Override
    public boolean delete(String relativePath) {
        validatePath(relativePath);
        Path targetFile = baseDirectory.resolve(relativePath).normalize();
        try {
            return Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String getUrl(String relativePath) {
        validatePath(relativePath);
        return "/" + baseDirectory.getFileName().toString() + "/" + relativePath.replace("\\", "/");
    }

    private void validatePath(String relativePath) {
        if (relativePath == null || relativePath.contains("..")) {
            throw new IllegalArgumentException("Invalid path or path traversal detected: " + relativePath);
        }
    }

    private Path resolveAndEnsureParent(String relativePath) {
        Path target = baseDirectory.resolve(relativePath).normalize();
        if (!target.startsWith(baseDirectory)) {
            throw new SecurityException("Attempted path traversal outside storage base directory");
        }
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new RuntimeException("Could not create parent directory: " + parent, e);
            }
        }
        return target;
    }
}
