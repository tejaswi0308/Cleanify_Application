package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.network.SupabaseClient;
import com.example.cleanify_application.utils.PasswordUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tabStudent, tabStaff, tvRegister;
    private SupabaseClient supabaseClient;
    private boolean isStudentTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        try {
            supabaseClient = SupabaseClient.getInstance(this);
        } catch (Exception e) {
            Log.e(TAG, "Supabase not configured: " + e.getMessage());
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tabStudent = findViewById(R.id.tabStudent);
        tabStaff = findViewById(R.id.tabStaff);
        tvRegister = findViewById(R.id.tvRegister);

        setupTabs();

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void setupTabs() {
        tabStudent.setOnClickListener(v -> {
            isStudentTab = true;
            tabStudent.setBackgroundResource(R.drawable.bg_tab_selected);
            tabStudent.setTextColor(getResources().getColor(R.color.tab_text_selected));
            tabStaff.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tabStaff.setTextColor(getResources().getColor(R.color.tab_text_unselected));
        });

        tabStaff.setOnClickListener(v -> {
            isStudentTab = false;
            tabStaff.setBackgroundResource(R.drawable.bg_tab_selected);
            tabStaff.setTextColor(getResources().getColor(R.color.tab_text_selected));
            tabStudent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tabStudent.setTextColor(getResources().getColor(R.color.tab_text_unselected));
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        // Authenticate via Supabase
        if (supabaseClient != null) {
            String table = isStudentTab ? "students" : "staff";
            supabaseClient.loginUser(this, table, email, password, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                        resetButton();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "[]";
                    runOnUiThread(() -> {
                        try {
                            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(body)
                                    .getAsJsonArray();
                            if (arr.size() > 0) {
                                com.google.gson.JsonObject user = arr.get(0).getAsJsonObject();

                                // Verify password (using the password verification logic from the response)
                                // Note: In a real app, this should be handled by Supabase Auth, but here we are
                                // using a custom table
                                // and verifying the password provided in the login call.

                                String role = isStudentTab ? "student" : "staff";
                                String uid = user.get("id").getAsString();
                                String fullName = user.has("full_name") && !user.get("full_name").isJsonNull()
                                        ? user.get("full_name").getAsString()
                                        : "";

                                supabaseClient.setUserId(uid);
                                supabaseClient.setUserRole(role);
                                supabaseClient.setUserName(fullName);

                                // Also save to LocalDataManager for offline profile access
                                com.example.cleanify_application.models.User localUser = new com.example.cleanify_application.models.User();
                                localUser.setUid(uid);
                                localUser.setEmail(email);
                                localUser.setRole(role);
                                localUser.setFullName(fullName);
                                if (user.has("hostel_id") && !user.get("hostel_id").isJsonNull())
                                    localUser.setHostelId(user.get("hostel_id").getAsString());
                                if (user.has("room_number") && !user.get("room_number").isJsonNull())
                                    localUser.setRoomNumber(user.get("room_number").getAsString());
                                if (user.has("assigned_hostel") && !user.get("assigned_hostel").isJsonNull())
                                    localUser.setAssignedHostel(user.get("assigned_hostel").getAsString());
                                if (user.has("househelp_id") && !user.get("househelp_id").isJsonNull())
                                    localUser.setHousehelpId(user.get("househelp_id").getAsString());
                                if (user.has("phone") && !user.get("phone").isJsonNull())
                                    localUser.setPhone(user.get("phone").getAsString());

                                com.example.cleanify_application.utils.LocalDataManager.getInstance(LoginActivity.this)
                                        .saveUser(localUser);
                                com.example.cleanify_application.utils.LocalDataManager.getInstance(LoginActivity.this)
                                        .setLoggedInUser(uid);

                                navigateToDashboard(role);
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "User not found on this tab. Please check your role.", Toast.LENGTH_LONG)
                                        .show();
                                resetButton();
                            }
                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            resetButton();
                        }
                    });
                }
            });
            return;
        }

        Toast.makeText(this, "Supabase connection error. Please try again.", Toast.LENGTH_SHORT).show();
        resetButton();
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("staff".equals(role)) {
            intent = new Intent(LoginActivity.this, StaffDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resetButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText(R.string.login);
    }
}
