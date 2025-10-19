package com.instagram.backend.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Slf4j
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    // This should be stored in environment variables or AWS Secrets Manager
    private static final String ENCRYPTION_KEY = System.getenv("ENCRYPTION_KEY") != null
            ? System.getenv("ENCRYPTION_KEY")
            : "MySecretKeyForEncryptionMustBe256BitLong1234567890123456";

    public static String encrypt(String data) {
        try {
            SecretKey key = getKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encryptedData = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            log.error("Error encrypting data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String encryptedData) {
        try {
            SecretKey key = getKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);

            return new String(decryptedData);
        } catch (Exception e) {
            log.error("Error decrypting data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private static SecretKey getKey() {
        byte[] decodedKey = Base64.getDecoder().decode(ENCRYPTION_KEY);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    public static String generateEncryptionKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("Error generating encryption key", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }

    // Mask sensitive information for logs
    public static String maskSensitiveData(String data) {
        if (data == null || data.length() < 4) {
            return "****";
        }
        int visibleChars = Math.min(2, data.length() / 3);
        return data.substring(0, visibleChars) +
                "*".repeat(Math.max(0, data.length() - visibleChars - 2)) +
                data.substring(Math.max(0, data.length() - 2));
    }
}