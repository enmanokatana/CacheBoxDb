package org.athens.utils;

public class NoEncryptionStrategy implements EncryptionStrategy {
    @Override
    public byte[] encrypt(byte[] data, byte[] key) {
        return data;
    }

    @Override
    public byte[] decrypt(byte[] encryptedData, byte[] key) {
        return encryptedData;
    }
}