package org.athens.db.encryption;

public interface EncryptionStrategy {
    byte[] encrypt(byte[] data, byte[] key);
    byte[] decrypt(byte[] encryptedData, byte[] key);
}