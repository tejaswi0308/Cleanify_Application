package com.example.cleanify_application.utils;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility class for password hashing and verification
 */
public class PasswordUtils {

    private static final int SALT_LENGTH = 16;
    private static final String HASH_ALGORITHM = "SHA-256";

    private PasswordUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generates a random salt
     * @return Base64 encoded salt string
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    /**
     * Hashes a password with a salt
     * @param password Plain text password
     * @param salt Base64 encoded salt
     * @return Base64 encoded hashed password
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.reset();
            digest.update(Base64.decode(salt, Base64.NO_WRAP));
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hashedBytes, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not available", e);
        }
    }

    /**
     * Verifies a password against a stored hash and salt
     * @param password Plain text password to verify
     * @param storedHash Stored Base64 encoded hash
     * @param storedSalt Stored Base64 encoded salt
     * @return true if password matches
     */
    public static boolean verifyPassword(String password, String storedHash, String storedSalt) {
        if (password == null || storedHash == null || storedSalt == null) {
            return false;
        }
        String computedHash = hashPassword(password, storedSalt);
        return computedHash.equals(storedHash);
    }
}
