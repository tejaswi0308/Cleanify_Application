package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.models.User;
import com.example.cleanify_application.network.SupabaseClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextView tabStudent, tabStaff, tvSubtitle, tvLogin;
    private LinearLayout studentFields, staffFields;
    private Button btnRegister;
    private SupabaseClient supabaseClient;
    private boolean isStudentTab = true;

    // Student fields
    private EditText etFullName, etEmail, etPhone, etPassword, etRegNumber, etHostelId, etFloor, etRoom;
    // Staff fields
    private EditText etStaffName, etStaffEmail, etHousehelpId, etAssignedHostel, etStaffPassword;
    private android.widget.Spinner spinnerShift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        supabaseClient = SupabaseClient.getInstance(this);

        initViews();
        setupTabs();

        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void initViews() {
        tabStudent = findViewById(R.id.tabStudent);
        tabStaff = findViewById(R.id.tabStaff);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvLogin = findViewById(R.id.tvLogin);
        studentFields = findViewById(R.id.studentFields);
        staffFields = findViewById(R.id.staffFields);
        btnRegister = findViewById(R.id.btnRegister);

        // Student
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etRegNumber = findViewById(R.id.etRegNumber);
        etHostelId = findViewById(R.id.etHostelId);
        etFloor = findViewById(R.id.etFloor);
        etRoom = findViewById(R.id.etRoom);

        // Staff
        etStaffName = findViewById(R.id.etStaffName);
        etStaffEmail = findViewById(R.id.etStaffEmail);
        etHousehelpId = findViewById(R.id.etHousehelpId);
        etAssignedHostel = findViewById(R.id.etAssignedHostel);
        spinnerShift = findViewById(R.id.spinnerShift);
        etStaffPassword = findViewById(R.id.etStaffPassword);
        
        // Setup Shift Spinner
        String[] shifts = {"Morning (8:00 AM - 4:00 PM)", "Evening (4:00 PM - 12:00 AM)", "Night (12:00 AM - 8:00 AM)"};
        android.widget.ArrayAdapter<String> shiftAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, shifts);
        shiftAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerShift.setAdapter(shiftAdapter);
    }

    private void setupTabs() {
        tabStudent.setOnClickListener(v -> {
            isStudentTab = true;
            tabStudent.setBackgroundResource(R.drawable.bg_tab_selected);
            tabStudent.setTextColor(getResources().getColor(R.color.tab_text_selected));
            tabStaff.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tabStaff.setTextColor(getResources().getColor(R.color.tab_text_unselected));
            studentFields.setVisibility(View.VISIBLE);
            staffFields.setVisibility(View.GONE);
            tvSubtitle.setText(R.string.join_cleanify_student);
        });

        tabStaff.setOnClickListener(v -> {
            isStudentTab = false;
            tabStaff.setBackgroundResource(R.drawable.bg_tab_selected);
            tabStaff.setTextColor(getResources().getColor(R.color.tab_text_selected));
            tabStudent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tabStudent.setTextColor(getResources().getColor(R.color.tab_text_unselected));
            studentFields.setVisibility(View.GONE);
            staffFields.setVisibility(View.VISIBLE);
            tvSubtitle.setText(R.string.join_cleanify_staff);
        });
    }

    private void registerUser() {
        String email, password, fullName;

        if (isStudentTab) {
            fullName = etFullName.getText().toString().trim();
            email = etEmail.getText().toString().trim();
            password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText("Creating account...");

            String uid = java.util.UUID.randomUUID().toString();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", uid);
            userMap.put("full_name", fullName);
            userMap.put("email", email);
            userMap.put("role", "student");
            userMap.put("phone", etPhone.getText().toString().trim());
            userMap.put("hostel_id", etHostelId.getText().toString().trim());
            userMap.put("floor_number", etFloor.getText().toString().trim());
            userMap.put("room_number", etRoom.getText().toString().trim());
            userMap.put("status", "free");
            userMap.put("plain_password", password);

            supabaseClient.registerUser(RegisterActivity.this, userMap, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnRegister.setEnabled(true);
                        btnRegister.setText(R.string.register);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            supabaseClient.setUserId(uid);
                            supabaseClient.setUserRole("student");
                            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, StudentDashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String errorBody = "Unknown error";
                            try {
                                if (response.body() != null) {
                                    errorBody = response.body().string();
                                }
                            } catch (Exception ignored) {}
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + errorBody, Toast.LENGTH_LONG).show();
                            btnRegister.setEnabled(true);
                            btnRegister.setText(R.string.register);
                        }
                    });
                }
            });
        } else {
            // Staff registration
            fullName = etStaffName.getText().toString().trim();
            email = etStaffEmail.getText().toString().trim();
            password = etStaffPassword.getText().toString().trim();
            String shift = spinnerShift.getSelectedItem().toString();

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText("Creating account...");

            String uid = java.util.UUID.randomUUID().toString();
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", uid);
            userMap.put("full_name", fullName);
            userMap.put("email", email);
            userMap.put("role", "staff");
            userMap.put("status", "free");
            userMap.put("shift", shift);
            userMap.put("phone", "");
            userMap.put("hostel_id", "");
            userMap.put("floor_number", "");
            userMap.put("room_number", "");
            userMap.put("plain_password", password);

            supabaseClient.registerUser(RegisterActivity.this, userMap, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    android.util.Log.e("RegisterActivity", "Registration network failure", e);
                    runOnUiThread(() -> {
                        btnRegister.setEnabled(true);
                        btnRegister.setText(R.string.register);
                        Toast.makeText(RegisterActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    android.util.Log.d("RegisterActivity", "Registration response code: " + response.code());
                    String responseBody = "";
                    try {
                        if (response.body() != null) {
                            responseBody = response.body().string();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("RegisterActivity", "Error reading response body", e);
                    }
                    android.util.Log.d("RegisterActivity", "Registration response body: " + responseBody);
                    
                    final String finalBody = responseBody;
                    final int code = response.code();
                    runOnUiThread(() -> {
                        btnRegister.setEnabled(true);
                        btnRegister.setText(R.string.register);
                        if (response.isSuccessful()) {
                            supabaseClient.setUserId(uid);
                            supabaseClient.setUserRole("staff");
                            Toast.makeText(RegisterActivity.this, "Staff registration successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, StaffDashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            // Show error in a dialog instead of toast so it's always visible
                            new androidx.appcompat.app.AlertDialog.Builder(RegisterActivity.this)
                                .setTitle("Registration Failed")
                                .setMessage("Error " + code + ": " + finalBody)
                                .setPositiveButton("OK", null)
                                .show();
                        }
                    });
                }
            });
        }
    }
}
