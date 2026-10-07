package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.security.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller to securely serve uploaded files (logos, signatures, receipts)
 * with strict path traversal protection and multi-tenant authorization checks.
 */
@RestController
@RequestMapping("/uploads")
public class UploadController {

    private final Path baseDirectory;
    private static final Pattern BIZ_PATTERN = Pattern.compile("^business-(\\d+)-.*");

    public UploadController(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.baseDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @GetMapping("/{filename:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getUploadedFile(@PathVariable String filename) {
        // Path traversal validation
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        // Multi-tenant check: if file is prefixed with business-{id}-, verify caller matches tenant
        Matcher matcher = BIZ_PATTERN.matcher(filename);
        if (matcher.matches()) {
            try {
                Long fileBizId = Long.parseLong(matcher.group(1));
                Long callerTenantId = TenantContext.getTenantId();
                if (callerTenantId != null && !callerTenantId.equals(fileBizId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: file belongs to another business");
                }
            } catch (NumberFormatException ignored) {}
        }

        Path target = baseDirectory.resolve(filename).normalize();
        if (!target.startsWith(baseDirectory) || !Files.exists(target) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        MediaType mediaType = determineMediaType(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .contentType(mediaType)
                .body(new FileSystemResource(target));
    }

    private MediaType determineMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
