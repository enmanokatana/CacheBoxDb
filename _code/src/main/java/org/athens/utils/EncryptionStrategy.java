package org.athens.utils;

public interface EncryptionStrategy {
    byte[] encrypt(byte[] data, byte[] key);
    byte[] decrypt(byte[] encryptedData, byte[] key);
}
