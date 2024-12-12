package org.athens.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Set;

public class KeyManager {
    private static final String KEY_FILE = "encryption_key.dat";
    private static final int KEY_LENGTH = 32; // 256-bit key

    public static byte[] getOrCreateEncryptionKey() {
        File keyFile = new File(KEY_FILE);

        // If key file exists, read and return the key
        if (keyFile.exists()) {
            try {
                return Files.readAllBytes(keyFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read encryption key", e);
            }
        }

        // Generate a new persistent key
        byte[] key = new byte[KEY_LENGTH];
        new SecureRandom().nextBytes(key);



        // Write key to file
        try {
            Files.write(keyFile.toPath(), key, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

            // Set file permissions to be readable only by the owner
            try {
                Path path = keyFile.toPath();
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
                perms.remove(PosixFilePermission.GROUP_READ);
                perms.remove(PosixFilePermission.OTHERS_READ);
                Files.setPosixFilePermissions(path, perms);
            } catch (UnsupportedOperationException | IOException e) {
                // Ignore if permission setting is not supported (e.g., on Windows)
                System.out.println("Could not set file permissions: " + e.getMessage());
            }

            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save encryption key", e);
        }
    }

    // Optional: Method to rotate the key if needed
    public static byte[] rotateEncryptionKey() {
        File oldKeyFile = new File(KEY_FILE);

        // Backup old key
        if (oldKeyFile.exists()) {
            try {
                Files.copy(
                        oldKeyFile.toPath(),
                        Paths.get(KEY_FILE + ".backup"),
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException e) {
                System.err.println("Failed to backup old encryption key");
            }
        }

        // Delete old key file to force regeneration
        oldKeyFile.delete();

        // Generate and return new key
        return getOrCreateEncryptionKey();
    }
}