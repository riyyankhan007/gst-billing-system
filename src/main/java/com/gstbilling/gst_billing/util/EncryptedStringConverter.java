package com.gstbilling.gst_billing.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * JPA AttributeConverter that transparently encrypts sensitive database fields (such as bank accounts)
 * using AES-256-GCM authenticated encryption.
 * Supports secure key rotation and backwards-compatible decryption.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedStringConverter.class);

    private static final String PREFIX = "ENC:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private static volatile SecretKey primaryKey;
    private static final List<SecretKey> legacyKeys = new ArrayList<>();

    public EncryptedStringConverter(
            @Value("${app.encryption.secret:}") String encryptionSecret,
            @Value("${app.encryption.legacy-secrets:}") String legacySecrets,
            @Value("${spring.profiles.active:default}") String activeProfile
    ) {
        boolean isProduction = "prod".equalsIgnoreCase(activeProfile) ||
                "production".equalsIgnoreCase(activeProfile) ||
                System.getenv("RENDER") != null;

        if (encryptionSecret == null || encryptionSecret.trim().length() < 16) {
            if (isProduction) {
                String msg = "Production encryption secret APP_ENCRYPTION_SECRET (app.encryption.secret) is required " +
                        "and must contain at least 16 characters. Startup aborted for financial data integrity.";
                log.error(msg);
                throw new IllegalStateException(msg);
            }
            log.warn("APP_ENCRYPTION_SECRET not explicitly configured in non-production profile '{}'. " +
                    "Using local development encryption key.", activeProfile);
            encryptionSecret = "dev-test-encryption-key-local-only-32b!";
        }

        primaryKey = deriveKey(encryptionSecret.trim());

        // Configure legacy keys for seamless rotation
        if (legacySecrets != null && !legacySecrets.isBlank()) {
            for (String secret : legacySecrets.split(",")) {
                if (!secret.trim().isBlank()) {
                    legacyKeys.add(deriveKey(secret.trim()));
                }
            }
        }
        // Always retain fallback key in legacy decryption chain for records written prior to rotation
        legacyKeys.add(deriveKey("gst-billing-default-fallback-key-32b"));
    }

    public EncryptedStringConverter(String encryptionSecret, String jwtSecret) {
        this(encryptionSecret, null, "test");
    }

    public EncryptedStringConverter() {
        // No-arg constructor for JPA provider instantiation
        if (primaryKey == null) {
            primaryKey = deriveKey("dev-test-encryption-key-local-only-32b!");
            legacyKeys.add(deriveKey("gst-billing-default-fallback-key-32b"));
        }
    }

    private static SecretKey deriveKey(String secret) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive AES-256 key", e);
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
            cipher.init(Cipher.ENCRYPT_MODE, primaryKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return PREFIX + Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting sensitive field", e);
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

            // Attempt decryption with primary key first
            try {
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, primaryKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
                byte[] plainText = cipher.doFinal(cipherText);
                return new String(plainText, StandardCharsets.UTF_8);
            } catch (Exception primaryEx) {
                // Key rotation fallback: try legacy keys
                for (SecretKey legacyKey : legacyKeys) {
                    try {
                        Cipher cipher = Cipher.getInstance(ALGORITHM);
                        cipher.init(Cipher.DECRYPT_MODE, legacyKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
                        byte[] plainText = cipher.doFinal(cipherText);
                        return new String(plainText, StandardCharsets.UTF_8);
                    } catch (Exception ignored) {
                        // try next legacy key
                    }
                }
                log.error("Failed to decrypt field with primary key and {} legacy keys", legacyKeys.size());
                return null;
            }
        } catch (Exception e) {
            log.error("Corrupt or invalid encrypted payload: {}", e.getMessage());
            return null;
        }
    }
}
