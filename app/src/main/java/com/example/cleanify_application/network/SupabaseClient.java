package com.example.cleanify_application.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.cleanify_application.BuildConfig;
import com.example.cleanify_application.constants.AppConstants;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SupabaseClient {

    private static SupabaseClient instance;
    private final OkHttpClient client;
    private final Gson gson;
    private final SharedPreferences sharedPreferences;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private SupabaseClient(Context context) {
        if (BuildConfig.SUPABASE_URL.isEmpty()) {
            android.util.Log.e("SupabaseClient", "SUPABASE_URL is empty! Check local.properties configuration");
            throw new RuntimeException("Supabase URL not configured. Please check local.properties file.");
        }

        client = new OkHttpClient.Builder()
                .connectTimeout(AppConstants.TIMEOUT_CONNECT, TimeUnit.SECONDS)
                .readTimeout(AppConstants.TIMEOUT_READ, TimeUnit.SECONDS)
                .writeTimeout(AppConstants.TIMEOUT_WRITE, TimeUnit.SECONDS)
                .build();
        gson = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();
        sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SupabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseClient(context.getApplicationContext());
        }
        return instance;
    }

    public String getAuthToken() {
        return sharedPreferences.getString(AppConstants.PREF_AUTH_TOKEN, null);
    }

    public boolean isAuthenticated() {
        return getUserId() != null;
    }

    public void setAuthToken(String token) {
        sharedPreferences.edit().putString(AppConstants.PREF_AUTH_TOKEN, token).apply();
    }

    public void clearAuthToken() {
        sharedPreferences.edit()
                .remove(AppConstants.PREF_AUTH_TOKEN)
                .remove(AppConstants.PREF_USER_ID)
                .remove(AppConstants.PREF_USER_ROLE)
                .remove(AppConstants.PREF_USER_NAME)
                .apply();
    }

    public void setUserRole(String role) {
        sharedPreferences.edit().putString(AppConstants.PREF_USER_ROLE, role).apply();
    }

    public String getUserRole() {
        return sharedPreferences.getString(AppConstants.PREF_USER_ROLE, null);
    }

    public void setUserId(String uid) {
        sharedPreferences.edit().putString(AppConstants.PREF_USER_ID, uid).apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(AppConstants.PREF_USER_ID, null);
    }

    public void setUserName(String name) {
        sharedPreferences.edit().putString(com.example.cleanify_application.constants.AppConstants.PREF_USER_NAME, name).apply();
    }

    public String getUserName() {
        return sharedPreferences.getString(com.example.cleanify_application.constants.AppConstants.PREF_USER_NAME, "Student");
    }

    public String getCachedUserName() {
        return sharedPreferences.getString(com.example.cleanify_application.constants.AppConstants.PREF_USER_NAME, null);
    }

    public void logout() {
        sharedPreferences.edit()
                .remove(com.example.cleanify_application.constants.AppConstants.PREF_USER_ID)
                .remove(com.example.cleanify_application.constants.AppConstants.PREF_USER_ROLE)
                .remove(com.example.cleanify_application.constants.AppConstants.PREF_USER_NAME)
                .apply();
    }

    /**
     * Register a new user by inserting directly into the students or staff table
     * Password is hashed with salt before storage
     */
    public void registerUser(Context context, String table, java.util.Map<String, Object> userMap, Callback callback) {
        if (BuildConfig.SUPABASE_URL.isEmpty()) {
            android.util.Log.e("SupabaseClient", "Cannot register: SUPABASE_URL is empty");
            callback.onFailure(null, new IOException("Supabase URL not configured"));
            return;
        }

        // Lowercase and trim email
        String email = (String) userMap.get("email");
        if (email != null) {
            userMap.put("email", email.trim().toLowerCase());
        }

        // Hash the password before storing
        String plainPassword = (String) userMap.remove("plain_password");
        if (plainPassword != null) {
            String salt = com.example.cleanify_application.utils.PasswordUtils.generateSalt();
            String hashedPassword = com.example.cleanify_application.utils.PasswordUtils.hashPassword(plainPassword, salt);
            userMap.put("password_hash", hashedPassword);
            userMap.put("password_salt", salt);
        }

        insert(context, table, userMap, callback);
    }

    /**
     * Login a user by querying the students or staff table and verifying password
     */
    public void loginUser(Context context, String table, String email, String password, Callback callback) {
        if (BuildConfig.SUPABASE_URL.isEmpty()) {
            android.util.Log.e("SupabaseClient", "Cannot login: SUPABASE_URL is empty");
            callback.onFailure(null, new IOException("Supabase URL not configured"));
            return;
        }

        // Query user by email from the specific table to get stored hash and salt (selecting all fields to sync local data)
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String encodedEmail = "";
        try {
            encodedEmail = java.net.URLEncoder.encode(cleanEmail, "UTF-8");
        } catch (Exception e) {
            encodedEmail = cleanEmail;
        }
        select(context, table, "email=eq." + encodedEmail + "&select=*", callback);
    }

    public void insert(Context context, String table, Object data, Callback callback) {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/" + table;
        String jsonStr = gson.toJson(data);
        android.util.Log.d("SupabaseClient", "Inserting into " + table + ": " + jsonStr);

        RequestBody body = RequestBody.create(jsonStr, JSON);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", BuildConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_KEY)
                .addHeader("Prefer", "return=representation")
                .addHeader("Content-Type", "application/json");

        client.newCall(requestBuilder.build()).enqueue(SafeCallback.wrap(callback, context));
    }

    public void select(Context context, String table, String queryParams, Callback callback) {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/" + table;
        if (queryParams != null && !queryParams.isEmpty()) {
            url += "?" + queryParams;
        }
        android.util.Log.d("SupabaseClient", "Selecting from " + table + " with query: " + queryParams);
        android.util.Log.d("SupabaseClient", "URL: " + url);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", BuildConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_KEY);

        client.newCall(requestBuilder.build()).enqueue(SafeCallback.wrap(callback, context));
    }

    public void update(Context context, String table, String queryParams, Object data, Callback callback) {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/" + table;
        if (queryParams != null && !queryParams.isEmpty()) {
            url += "?" + queryParams;
        }
        String jsonStr = gson.toJson(data);
        android.util.Log.d("SupabaseClient", "Updating " + table + " with query: " + queryParams);
        android.util.Log.d("SupabaseClient", "Data: " + jsonStr);
        android.util.Log.d("SupabaseClient", "URL: " + url);

        RequestBody body = RequestBody.create(jsonStr, JSON);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", BuildConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_KEY)
                .addHeader("Content-Profile", "public");

        client.newCall(requestBuilder.build()).enqueue(SafeCallback.wrap(callback, context));
    }

    public void delete(Context context, String table, String queryParams, Callback callback) {
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/" + table;
        if (queryParams != null && !queryParams.isEmpty()) {
            url += "?" + queryParams;
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", BuildConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_KEY);

        client.newCall(requestBuilder.build()).enqueue(SafeCallback.wrap(callback, context));
    }
}
