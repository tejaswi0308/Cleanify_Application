package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.models.User;
import com.example.cleanify_application.network.SupabaseClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private TextView tabStudent, tabStaff, tvSubtitle, tvLogin;
    private LinearLayout studentFields, staffFields;
    private Button btnRegister;
    private SupabaseClient supabaseClient;
    private boolean isStudentTab = true;

    // Student fields
    private EditText etFullName, etEmail, etPhone, etPassword, etRegNumber, etHostelId, etFloor, etRoom;
    // Staff fields
    private EditText etStaffName, etStaffEmail, etStaffPhone, etHousehelpId, etAssignedHostel, etShift, etStaffPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        try {
            supabaseClient = SupabaseClient.getInstance(this);
        } catch (Exception e) {
            Log.e(TAG, "Supabase not configured: " + e.getMessage());
        }

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
        etStaffPhone = findViewById(R.id.etStaffPhone);
        etHousehelpId = findViewById(R.id.etHousehelpId);
        etAssignedHostel = findViewById(R.id.etAssignedHostel);
        etShift = findViewById(R.id.etShift);
        etStaffPassword = findViewById(R.id.etStaffPassword);
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
            email = etEmail.getText().toString().trim().toLowerCase();
            password = etPassword.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(etHostelId.getText().toString().trim()) || TextUtils.isEmpty(etRoom.getText().toString().trim())) {
                Toast.makeText(this, "Please enter your Hostel and Room Number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, R.string.password_length_error, Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText(R.string.creating_account);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (btnRegister != null && !btnRegister.isEnabled()) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText(R.string.register);
                }
            }, 10000);

            String uid = UUID.randomUUID().toString();
            
            // Supabase integration
            if (supabaseClient != null) {
                Map<String, Object> supabaseMap = new HashMap<>();
                supabaseMap.put("id", uid);
                supabaseMap.put("email", email);
                supabaseMap.put("full_name", fullName);
                supabaseMap.put("phone", phone);
                supabaseMap.put("reg_number", etRegNumber.getText().toString().trim());
                supabaseMap.put("hostel_id", etHostelId.getText().toString().trim());
                supabaseMap.put("floor_number", etFloor.getText().toString().trim());
                supabaseMap.put("room_number", etRoom.getText().toString().trim());
                supabaseMap.put("plain_password", password);

                supabaseClient.registerUser(this, "students", supabaseMap, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Supabase registration failed: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(RegisterActivity.this, "Supabase Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            btnRegister.setEnabled(true);
                            btnRegister.setText(R.string.register);
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Supabase registration successful");
                            runOnUiThread(() -> {
                                Toast.makeText(RegisterActivity.this, R.string.registration_successful, Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            String errorBody = response.body().string();
                            Log.e(TAG, "Supabase registration error: " + errorBody);
                            runOnUiThread(() -> {
                                Toast.makeText(RegisterActivity.this, "DB Error: " + errorBody, Toast.LENGTH_LONG).show();
                                btnRegister.setEnabled(true);
                                btnRegister.setText(R.string.register);
                            });
                        }
                    }
                });
            } else {
                Toast.makeText(this, R.string.supabase_not_connected, Toast.LENGTH_LONG).show();
                btnRegister.setEnabled(true);
                btnRegister.setText(R.string.register);
            }
        } else {
            fullName = etStaffName.getText().toString().trim();
            email = etStaffEmail.getText().toString().trim().toLowerCase();
            password = etStaffPassword.getText().toString().trim();

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText(R.string.creating_account);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (btnRegister != null && !btnRegister.isEnabled()) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText(R.string.register);
                }
            }, 10000);

            String uid = UUID.randomUUID().toString();

            // Supabase integration
            if (supabaseClient != null) {
                Map<String, Object> supabaseMap = new HashMap<>();
                supabaseMap.put("id", uid);
                supabaseMap.put("email", email);
                supabaseMap.put("full_name", fullName);
                supabaseMap.put("phone", etStaffPhone.getText().toString().trim());
                supabaseMap.put("assigned_hostel", etAssignedHostel.getText().toString().trim());
                supabaseMap.put("househelp_id", etHousehelpId.getText().toString().trim());
                supabaseMap.put("shift", etShift.getText().toString().trim());
                supabaseMap.put("plain_password", password);

                supabaseClient.registerUser(this, "staff", supabaseMap, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Supabase registration failed: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(RegisterActivity.this, "Supabase Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            btnRegister.setEnabled(true);
                            btnRegister.setText(R.string.register);
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Supabase registration successful");
                            runOnUiThread(() -> {
                                Toast.makeText(RegisterActivity.this, R.string.registration_successful, Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            String errorBody = response.body().string();
                            Log.e(TAG, "Supabase registration error: " + errorBody);
                            runOnUiThread(() -> {
                                Toast.makeText(RegisterActivity.this, "DB Error: " + errorBody, Toast.LENGTH_LONG).show();
                                btnRegister.setEnabled(true);
                                btnRegister.setText(R.string.register);
                            });
                        }
                    }
                });
            } else {
                Toast.makeText(this, R.string.supabase_not_connected, Toast.LENGTH_LONG).show();
                btnRegister.setEnabled(true);
                btnRegister.setText(R.string.register);
            }
        }
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText(R.string.register);
    }
}
