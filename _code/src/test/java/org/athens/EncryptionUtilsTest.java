package org.athens;

import org.athens.utils.EncryptionUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

    public class EncryptionUtilsTest {
        @Test
        public void testEncryptionDecryption() {
            String original = "Hello, World!";
            byte[] data = original.getBytes();
            byte[] encrypted = EncryptionUtils.encrypt(data, "testestestes".getBytes());
            byte[] decrypted = EncryptionUtils.decrypt(encrypted,"testestestes".getBytes());
            String result = new String(decrypted);
            assertEquals(original, result);
        }
    }
