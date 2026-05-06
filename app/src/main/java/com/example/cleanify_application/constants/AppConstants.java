package com.example.cleanify_application.constants;

/**
 * Application constants to avoid magic strings and improve maintainability
 */
public class AppConstants {

    // Database table names
    public static final String TABLE_STUDENTS = "students";
    public static final String TABLE_STAFF = "staff";
    public static final String TABLE_CLEANING_REQUESTS = "cleaning_requests";

    // Request status values
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ASSIGNED = "assigned";
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_AWAITING_VERIFICATION = "awaiting_verification";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    // User roles
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_STAFF = "staff";

    // Staff status values
    public static final String STAFF_STATUS_FREE = "free";
    public static final String STAFF_STATUS_BUSY = "busy";

    // SharedPreferences keys
    public static final String PREFS_NAME = "supabase_prefs";
    public static final String PREF_AUTH_TOKEN = "auth_token";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_ROLE = "user_role";
    public static final String PREF_USER_NAME = "user_name";

    // Network timeouts (in seconds)
    public static final int TIMEOUT_CONNECT = 30;
    public static final int TIMEOUT_READ = 30;
    public static final int TIMEOUT_WRITE = 30;

    // Default values
    public static final int DEFAULT_ESTIMATED_MINUTES = 25;
    public static final int DEFAULT_REQUEST_LIMIT = 20;

    // Intent extras
    public static final String EXTRA_REQUEST_ID = "requestId";
    public static final String EXTRA_QR_CODE = "qrCode";

    private AppConstants() {
        // Private constructor to prevent instantiation
    }
}
