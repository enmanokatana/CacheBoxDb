package org.athens.utils;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class PasswordBasedEncryptionStrategy implements EncryptionStrategy {
    private static final String ALGORITHM = "AES/GCM/PKCS5Padding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    private static final int TIME_COST = 2;
    private static final int MEMORY_COST = 65536;
    private static final int PARALLELISM = 1;
    private static final int KEY_LENGTH = 16; // 128 bits for AES

    @Override
    public byte[] encrypt(byte[] data, byte[] passwordBytes) {
        char[] password = new String(passwordBytes).toCharArray();
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] key = deriveKey(password, salt);

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] encryptedData = cipher.doFinal(data);

            byte[] combined = new byte[salt.length + iv.length + encryptedData.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(encryptedData, 0, combined, salt.length + iv.length, encryptedData.length);
            return combined;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        } finally {
            wipeArray(password);
        }
    }

    @Override
    public byte[] decrypt(byte[] encryptedData, byte[] passwordBytes) {
        char[] password = new String(passwordBytes).toCharArray();
        byte[] salt = new byte[16];
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] cipherText = new byte[encryptedData.length - salt.length - iv.length];
        System.arraycopy(encryptedData, 0, salt, 0, salt.length);
        System.arraycopy(encryptedData, salt.length, iv, 0, iv.length);
        System.arraycopy(encryptedData, salt.length + iv.length, cipherText, 0, cipherText.length);

        byte[] key = deriveKey(password, salt);

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        } finally {
            wipeArray(password);
        }
    }

    private byte[] deriveKey(char[] password, byte[] salt) {
        // Combine password and salt
        String saltedPassword = new String(salt) + new String(password);

        // Derive hash using Argon2
        String hash = argon2.hash(TIME_COST, MEMORY_COST, PARALLELISM, saltedPassword);

        // Convert hash to bytes
        byte[] hashBytes = hash.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Truncate or pad to the required key length
        byte[] key = new byte[KEY_LENGTH];
        System.arraycopy(hashBytes, 0, key, 0, Math.min(hashBytes.length, KEY_LENGTH));
        return key;
    }



    private void wipeArray(char[] array) {
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                array[i] = 0;
            }
        }
    }
}