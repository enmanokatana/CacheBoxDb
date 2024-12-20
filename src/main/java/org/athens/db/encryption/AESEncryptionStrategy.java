package org.athens.db.encryption;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AESEncryptionStrategy implements EncryptionStrategy {
    private static final Logger LOGGER = Logger.getLogger(AESEncryptionStrategy.class.getName());
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_LENGTH = 32; // 256-bit key for enhanced security

    // Static block to register BouncyCastle provider once
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] key) {
        try {
            // Validate input
            if (data == null || data.length == 0) {
                throw new IllegalArgumentException("Data cannot be null or empty");
            }

            // Ensure key is correct length, pad or truncate if necessary
            key = padOrTruncateKey(key);

            // Create secret key
            SecretKey secretKey = new SecretKeySpec(key, "AES");

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt
            byte[] encryptedData = cipher.doFinal(data);

            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

            // Log encryption details for debugging
//            LOGGER.log(Level.INFO, "Encryption successful");
//            LOGGER.log(Level.FINE, "Input data length: " + data.length);
//            LOGGER.log(Level.FINE, "Encrypted data length: " + combined.length);
//            LOGGER.log(Level.FINE, "IV: " + Base64.getEncoder().encodeToString(iv));
//            LOGGER.log(Level.FINE, "Key hash: " + Arrays.hashCode(key));

            return combined;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] encryptedData, byte[] key) {
        try {
            // Validate input
            if (encryptedData == null || encryptedData.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data");
            }

            // Ensure key is correct length, pad or truncate if necessary
            key = padOrTruncateKey(key);

            // Create secret key
            SecretKey secretKey = new SecretKeySpec(key, "AES");

            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);

            // Extract actual encrypted data
            byte[] cipherText = new byte[encryptedData.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            // Log decryption details for debugging
//            LOGGER.log(Level.INFO, "Starting decryption");
//            LOGGER.log(Level.FINE, "Encrypted data length: " + encryptedData.length);
//            LOGGER.log(Level.FINE, "Cipher text length: " + cipherText.length);
//            LOGGER.log(Level.FINE, "IV: " + Base64.getEncoder().encodeToString(iv));
//            LOGGER.log(Level.FINE, "Key hash: " + Arrays.hashCode(key));

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt
            byte[] decryptedData = cipher.doFinal(cipherText);

//            LOGGER.log(Level.INFO, "Decryption successful");
//            LOGGER.log(Level.FINE, "Decrypted data length: " + decryptedData.length);

            return decryptedData;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Decryption failed", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Ensures the key is exactly 32 bytes (256 bits) by padding or truncating
     * @param originalKey input key
     * @return standardized 32-byte key
     */
    private byte[] padOrTruncateKey(byte[] originalKey) {
        if (originalKey == null) {
            throw new IllegalArgumentException("Encryption key cannot be null");
        }

        // If key is shorter than 32 bytes, pad with zeros
        if (originalKey.length < KEY_LENGTH) {
            byte[] paddedKey = new byte[KEY_LENGTH];
            System.arraycopy(originalKey, 0, paddedKey, 0, originalKey.length);
            return paddedKey;
        }

        // If key is longer than 32 bytes, truncate
        return Arrays.copyOf(originalKey, KEY_LENGTH);
    }
}