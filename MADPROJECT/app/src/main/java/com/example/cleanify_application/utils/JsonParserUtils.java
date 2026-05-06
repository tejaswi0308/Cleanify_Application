package com.example.cleanify_application.utils;

import com.example.cleanify_application.models.CleaningRequest;
import com.example.cleanify_application.models.User;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utility class for JSON parsing to reduce code duplication
 */
public class JsonParserUtils {

    private JsonParserUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Safely gets a string from a JsonObject
     * @param obj JsonObject to parse
     * @param key Key to retrieve
     * @return String value or empty string if null/missing
     */
    public static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        return obj.get(key).getAsString();
    }

    /**
     * Safely gets a string from a JsonObject with default value
     * @param obj JsonObject to parse
     * @param key Key to retrieve
     * @param defaultValue Default value if null/missing
     * @return String value or defaultValue if null/missing
     */
    public static String getString(JsonObject obj, String key, String defaultValue) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        return obj.get(key).getAsString();
    }

    /**
     * Safely gets an integer from a JsonObject
     * @param obj JsonObject to parse
     * @param key Key to retrieve
     * @return Integer value or 0 if null/missing
     */
    public static int getInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return 0;
        }
        return obj.get(key).getAsInt();
    }

    /**
     * Safely gets an integer from a JsonObject with default value
     * @param obj JsonObject to parse
     * @param key Key to retrieve
     * @param defaultValue Default value if null/missing
     * @return Integer value or defaultValue if null/missing
     */
    public static int getInt(JsonObject obj, String key, int defaultValue) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        return obj.get(key).getAsInt();
    }

    /**
     * Safely gets a long from a JsonObject
     * @param obj JsonObject to parse
     * @param key Key to retrieve
     * @return Long value or 0 if null/missing
     */
    public static long getLong(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return 0L;
        }
        return obj.get(key).getAsLong();
    }

    /**
     * Safely gets a boolean from a JsonObject
     * @param obj JsonObject to parse
     * @param key Key to retrieve
     * @return Boolean value or false if null/missing
     */
    public static boolean getBoolean(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return false;
        }
        return obj.get(key).getAsBoolean();
    }

    /**
     * Parses a CleaningRequest from a JsonObject
     * @param obj JsonObject to parse
     * @return CleaningRequest object
     */
    public static CleaningRequest parseCleaningRequest(JsonObject obj) {
        if (obj == null) {
            return new CleaningRequest();
        }

        CleaningRequest request = new CleaningRequest();
        request.setRequestId(getString(obj, "request_id"));
        request.setStudentId(getString(obj, "student_id"));
        request.setStudentName(getString(obj, "student_name"));
        request.setHostel(getString(obj, "hostel"));
        request.setRoomNumber(getString(obj, "room_number"));
        request.setFloorNumber(getString(obj, "floor_number"));
        request.setCleaningType(getString(obj, "cleaning_type"));
        request.setDate(getString(obj, "date"));
        request.setTimeSlot(getString(obj, "time_slot"));
        request.setAdditionalNotes(getString(obj, "additional_notes"));
        request.setStatus(getString(obj, "status"));
        request.setAssignedStaffId(getString(obj, "assigned_staff_id"));
        request.setAssignedStaffName(getString(obj, "assigned_staff_name"));
        request.setCreatedAt(getLong(obj, "created_at"));
        request.setStartTime(getLong(obj, "start_time"));
        request.setCompletedAt(getLong(obj, "completed_at"));
        request.setEstimatedMinutes(getInt(obj, "estimated_minutes"));
        request.setRating(getInt(obj, "rating"));
        request.setFeedback(getString(obj, "feedback"));
        request.setQrCode(getString(obj, "qr_code"));

        return request;
    }

    /**
     * Parses a User from a JsonObject
     * @param obj JsonObject to parse
     * @return User object
     */
    public static User parseUser(JsonObject obj) {
        if (obj == null) {
            return new User();
        }

        User user = new User();
        user.setUid(getString(obj, "uid"));
        user.setFullName(getString(obj, "full_name"));
        user.setEmail(getString(obj, "email"));
        user.setPhone(getString(obj, "phone"));
        user.setRole(getString(obj, "role"));
        user.setRegistrationNumber(getString(obj, "registration_number"));
        user.setHostelId(getString(obj, "hostel_id"));
        user.setFloorNumber(getString(obj, "floor_number"));
        user.setRoomNumber(getString(obj, "room_number"));
        user.setHousehelpId(getString(obj, "househelp_id"));
        user.setAssignedHostel(getString(obj, "assigned_hostel"));
        user.setShift(getString(obj, "shift"));
        user.setProfileImageUrl(getString(obj, "profile_image_url"));

        return user;
    }

    /**
     * Checks if a JSON response indicates success
     * @param jsonResponse JSON string response
     * @return true if response is valid and not empty
     */
    public static boolean isValidResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return false;
        }
        try {
            JsonElement element = JsonParser.parseString(jsonResponse);
            return element != null && !element.isJsonNull();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts error message from error response
     * @param jsonResponse JSON error response
     * @return Error message or default message
     */
    public static String extractErrorMessage(String jsonResponse) {
        if (!isValidResponse(jsonResponse)) {
            return "An unknown error occurred";
        }

        try {
            JsonObject obj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            if (obj.has("message")) {
                return getString(obj, "message");
            }
            if (obj.has("error")) {
                return getString(obj, "error");
            }
            if (obj.has("error_description")) {
                return getString(obj, "error_description");
            }
        } catch (Exception e) {
            // Return default if parsing fails
        }

        return "An unknown error occurred";
    }
}
