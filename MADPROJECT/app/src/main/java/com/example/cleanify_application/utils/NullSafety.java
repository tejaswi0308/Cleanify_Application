package com.example.cleanify_application.utils;

/**
 * Utility class for null safety checks
 */
public class NullSafety {

    private NullSafety() {
        // Private constructor to prevent instantiation
    }

    /**
     * Returns a default value if the input is null
     * @param value Input value to check
     * @param defaultValue Default value to return if input is null
     * @return Input value or default value if null
     */
    public static <T> T orDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Returns empty string if input is null
     * @param value Input string
     * @return Input string or empty string if null
     */
    public static String orEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * Returns empty string if input is null or empty after trim
     * @param value Input string
     * @return Input string or empty string if null/empty
     */
    public static String orEmptyTrimmed(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    /**
     * Checks if a string is null or empty
     * @param value Input string
     * @return true if null or empty
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Safely converts an integer to string
     * @param value Integer value
     * @return String representation or "0" if null
     */
    public static String intToString(Integer value) {
        return value != null ? String.valueOf(value) : "0";
    }

    /**
     * Safely converts a long to string
     * @param value Long value
     * @return String representation or "0" if null
     */
    public static String longToString(Long value) {
        return value != null ? String.valueOf(value) : "0";
    }

    /**
     * Safely gets an integer from a string
     * @param value String value
     * @param defaultValue Default value if parsing fails
     * @return Integer value or default
     */
    public static int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Safely gets a long from a string
     * @param value String value
     * @param defaultValue Default value if parsing fails
     * @return Long value or default
     */
    public static long parseLong(String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Checks if all provided values are non-null
     * @param values Values to check
     * @return true if all values are non-null
     */
    @SafeVarargs
    public static <T> boolean allNonNull(T... values) {
        for (T value : values) {
            if (value == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if any provided value is non-null
     * @param values Values to check
     * @return true if at least one value is non-null
     */
    @SafeVarargs
    public static <T> boolean anyNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requires a non-null value, throws NullPointerException if null
     * @param value Value to check
     * @param errorMessage Error message to throw if null
     * @return The value if non-null
     * @throws NullPointerException if value is null
     */
    public static <T> T requireNonNull(T value, String errorMessage) {
        if (value == null) {
            throw new NullPointerException(errorMessage);
        }
        return value;
    }
}
