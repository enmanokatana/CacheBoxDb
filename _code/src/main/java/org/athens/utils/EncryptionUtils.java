package org.athens.utils;

public class EncryptionUtils {
    public static byte[] encrypt(byte[] data, byte[] key) {
        byte[] encrypted = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            encrypted[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return encrypted;
    }

    public static byte[] decrypt(byte[] encryptedData, byte[] key) {
        return encrypt(encryptedData, key);
    }
}
