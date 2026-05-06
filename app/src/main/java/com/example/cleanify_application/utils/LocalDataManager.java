package com.example.cleanify_application.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.cleanify_application.models.CleaningRequest;
import com.example.cleanify_application.models.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages local persistence for user profile caching and session management.
 * Provides quick access to the logged-in user's details without network calls.
 */
public class LocalDataManager {
    private static final String PREFS_NAME = "cleanify_local_db";
    private static final String KEY_USERS = "users_map";
    private static final String KEY_LOGGED_IN_USER_ID = "logged_in_user_id";

    private static LocalDataManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private LocalDataManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized LocalDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDataManager(context);
        }
        return instance;
    }

    // --- User Management ---

    public void saveUser(User user) {
        Map<String, User> users = getAllUsers();
        if (user.getEmail() != null) users.put(user.getEmail(), user); 
        if (user.getUid() != null) users.put(user.getUid(), user);

        String json = gson.toJson(users);
        prefs.edit().putString(KEY_USERS, json).apply();
    }

    public User getUserByEmail(String email) {
        Map<String, User> users = getAllUsers();
        return users.get(email);
    }

    public User getUserById(String uid) {
        Map<String, User> users = getAllUsers();
        return users.get(uid);
    }

    public Map<String, User> getAllUsers() {
        String json = prefs.getString(KEY_USERS, null);
        if (json == null) return new HashMap<>();

        Type type = new TypeToken<Map<String, User>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void setLoggedInUser(String uid) {
        prefs.edit().putString(KEY_LOGGED_IN_USER_ID, uid).apply();
    }

    public String getLoggedInUserId() {
        return prefs.getString(KEY_LOGGED_IN_USER_ID, null);
    }

    public void logout() {
        prefs.edit().remove(KEY_LOGGED_IN_USER_ID).apply();
    }
}
