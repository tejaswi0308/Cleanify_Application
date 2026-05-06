package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.network.SupabaseClient;
import com.example.cleanify_application.utils.ErrorHandler;
import com.example.cleanify_application.utils.ValidationUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tabStudent, tabStaff, tvForgotPassword, tvRegister;
    private SupabaseClient supabaseClient;
    private boolean isStudentTab = true;

    // Flag to check if user is in demo mode
    // Demo variables removed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        supabaseClient = SupabaseClient.getInstance(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tabStudent = findViewById(R.id.tabStudent);
        tabStaff = findViewById(R.id.tabStaff);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);

        setupTabs();

        btnLogin.setOnClickListener(v -> loginUser());

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void prefillDemoCredentials() {
        // Removed demo pre-fill
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
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!ValidationUtils.validateEmailEditText(etEmail)) {
            return;
        }
        if (!ValidationUtils.validatePasswordEditText(etPassword)) {
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        // Query users table by email and verify password
        supabaseClient.loginUser(this, email, password, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    ErrorHandler.handleNetworkError(LoginActivity.this, e);
                    resetButton();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        com.google.gson.JsonArray jsonArray = JsonParser.parseString(responseData).getAsJsonArray();
                        if (jsonArray.size() == 0) {
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();
                                resetButton();
                            });
                            return;
                        }

                        JsonObject userObj = jsonArray.get(0).getAsJsonObject();
                        String storedHash = userObj.has("password_hash") && !userObj.get("password_hash").isJsonNull() ? userObj.get("password_hash").getAsString() : null;
                        String storedSalt = userObj.has("password_salt") && !userObj.get("password_salt").isJsonNull() ? userObj.get("password_salt").getAsString() : null;
                        String uid = userObj.has("id") && !userObj.get("id").isJsonNull() ? userObj.get("id").getAsString() : null;
                        String role = userObj.has("role") && !userObj.get("role").isJsonNull() ? userObj.get("role").getAsString() : null;

                        // Verify password
                        if (storedHash != null && storedSalt != null && uid != null) {
                            boolean passwordValid = com.example.cleanify_application.utils.PasswordUtils.verifyPassword(password, storedHash, storedSalt);
                            if (passwordValid) {
                                supabaseClient.setUserId(uid);
                                supabaseClient.setUserRole(role);
                                runOnUiThread(() -> navigateToDashboard(role));
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();
                                    resetButton();
                                });
                            }
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Account not properly set up. Please re-register.", Toast.LENGTH_LONG).show();
                                resetButton();
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing login response", e);
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            resetButton();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        ErrorHandler.handleResponseError(LoginActivity.this, response, null);
                        resetButton();
                    });
                }
            }
        });
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
