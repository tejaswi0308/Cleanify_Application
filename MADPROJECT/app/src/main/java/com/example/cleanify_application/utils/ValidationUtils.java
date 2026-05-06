package com.example.cleanify_application.utils;

import android.text.TextUtils;
import android.widget.EditText;

import java.util.regex.Pattern;

/**
 * Utility class for input validation
 */
public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9]{10,15}$"
    );

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 20;

    private ValidationUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Validates email format
     * @param email Email string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates phone number format
     * @param phone Phone string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validates password strength
     * @param password Password string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (TextUtils.isEmpty(password)) {
            return false;
        }
        int length = password.length();
        return length >= MIN_PASSWORD_LENGTH && length <= MAX_PASSWORD_LENGTH;
    }

    /**
     * Checks if a string is not empty
     * @param text Text to check
     * @return true if not empty, false otherwise
     */
    public static boolean isNotEmpty(String text) {
        return !TextUtils.isEmpty(text) && !text.trim().isEmpty();
    }

    /**
     * Validates EditText field and sets error if invalid
     * @param editText EditText to validate
     * @param fieldName Name of the field for error message
     * @return true if valid, false otherwise
     */
    public static boolean validateEditText(EditText editText, String fieldName) {
        String text = editText.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            editText.setError(fieldName + " is required");
            return false;
        }
        return true;
    }

    /**
     * Validates email EditText
     * @param editText EditText containing email
     * @return true if valid, false otherwise
     */
    public static boolean validateEmailEditText(EditText editText) {
        String email = editText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            editText.setError("Email is required");
            return false;
        }
        if (!isValidEmail(email)) {
            editText.setError("Please enter a valid email");
            return false;
        }
        return true;
    }

    /**
     * Validates password EditText
     * @param editText EditText containing password
     * @return true if valid, false otherwise
     */
    public static boolean validatePasswordEditText(EditText editText) {
        String password = editText.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            editText.setError("Password is required");
            return false;
        }
        if (!isValidPassword(password)) {
            editText.setError("Password must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters");
            return false;
        }
        return true;
    }

    /**
     * Validates phone EditText
     * @param editText EditText containing phone
     * @return true if valid, false otherwise
     */
    public static boolean validatePhoneEditText(EditText editText) {
        String phone = editText.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            editText.setError("Phone number is required");
            return false;
        }
        if (!isValidPhone(phone)) {
            editText.setError("Please enter a valid phone number");
            return false;
        }
        return true;
    }

    /**
     * Sanitizes input by trimming and removing potentially harmful characters
     * @param input Input string to sanitize
     * @return Sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.trim()
                .replaceAll("['\"\\\\]", "")
                .replaceAll("<[^>]*>", "");
    }

    /**
     * Gets error message for invalid email
     * @return Error message
     */
    public static String getEmailErrorMessage() {
        return "Please enter a valid email address";
    }

    /**
     * Gets error message for invalid password
     * @return Error message
     */
    public static String getPasswordErrorMessage() {
        return "Password must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters";
    }

    /**
     * Gets error message for invalid phone
     * @return Error message
     */
    public static String getPhoneErrorMessage() {
        return "Please enter a valid phone number (10-15 digits)";
    }
}
