package com.gstbilling.gst_billing.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter that transparently encrypts sensitive database fields (like bank accounts)
 * using AES-256-GCM authenticated encryption.
 * Backwards compatible with existing plaintext rows.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "ENC:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private static volatile SecretKey secretKey;

    public EncryptedStringConverter(
            @Value("${app.encryption.secret:}") String encryptionSecret,
            @Value("${app.jwt.secret:}") String jwtSecret
    ) {
        String keySource = (encryptionSecret != null && !encryptionSecret.isBlank())
                ? encryptionSecret
                : ((jwtSecret != null && !jwtSecret.isBlank()) ? jwtSecret : "gst-billing-default-fallback-key-32b");
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(keySource.getBytes(StandardCharsets.UTF_8));
            secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize encryption key", e);
        }
    }

    public EncryptedStringConverter() {
        // Fallback no-args constructor for JPA provider instantiation
        if (secretKey == null) {
            try {
                byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                        .digest("gst-billing-default-fallback-key-32b".getBytes(StandardCharsets.UTF_8));
                secretKey = new SecretKeySpec(keyBytes, "AES");
            } catch (Exception ignored) {}
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return PREFIX + Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }

        // Backwards compatibility: If not encrypted with our prefix, return as-is
        if (!dbData.startsWith(PREFIX)) {
            return dbData;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(dbData.substring(PREFIX.length()));
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // In case of tampering or key mismatch, log or return null rather than crash
            return null;
        }
    }
}
