package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.network.SupabaseClient;
import com.example.cleanify_application.utils.PasswordUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPhone, etHostel, etRoom;
    private Button btnSaveChanges, btnChangePhoto, btnLogout;
    private ImageView btnBack;
    private de.hdodenhof.circleimageview.CircleImageView ivProfileImage;
    private SupabaseClient supabaseClient;
    private String selectedImageUriStr = null;

    private final androidx.activity.result.ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUriStr = uri.toString();
                    com.bumptech.glide.Glide.with(this).load(uri).into(ivProfileImage);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        try {
            supabaseClient = SupabaseClient.getInstance(this);
        } catch (Exception e) { /* not configured */ }

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etHostel = findViewById(R.id.etHostel);
        etRoom = findViewById(R.id.etRoom);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnBack = findViewById(R.id.btnBack);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        btnLogout = findViewById(R.id.btnLogout);

        btnBack.setOnClickListener(v -> finish());

        loadProfileData();

        btnSaveChanges.setOnClickListener(v -> {
            saveProfile();
        });

        btnChangePhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser() {
        // Clear Supabase session
        if (supabaseClient != null) {
            supabaseClient.clearAuthToken();
        }
        
        // Clear Local session
        com.example.cleanify_application.utils.LocalDataManager.getInstance(this).logout();
        
        // Redirect to Login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadProfileData() {
        if (supabaseClient == null || !supabaseClient.isAuthenticated()) return;

        String uid = supabaseClient.getUserId();
        String role = supabaseClient.getUserRole();
        String table = "student".equals(role) ? "students" : "staff";

        supabaseClient.select(this, table, "id=eq." + uid, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                runOnUiThread(() -> {
                    try {
                        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                        if (arr.size() > 0) {
                            JsonObject user = arr.get(0).getAsJsonObject();
                            String name = getStr(user, "full_name");
                            String email = getStr(user, "email");
                            String phone = getStr(user, "phone");
                            
                            etFullName.setText(name);
                            etEmail.setText(email);
                            etPhone.setText(phone);
                            
                            String hostel, room;
                            if ("staff".equals(role)) {
                                hostel = getStr(user, "assigned_hostel");
                                room = getStr(user, "househelp_id");
                                etHostel.setText(hostel);
                                etRoom.setText(room);
                            } else {
                                hostel = getStr(user, "hostel_id");
                                room = getStr(user, "room_number");
                                etHostel.setText(hostel);
                                etRoom.setText(room);
                            }

                            String profileUrl = getStr(user, "profile_image_url");
                            if (!profileUrl.isEmpty()) {
                                selectedImageUriStr = profileUrl;
                                com.bumptech.glide.Glide.with(EditProfileActivity.this)
                                        .load(android.net.Uri.parse(profileUrl))
                                        .into(ivProfileImage);
                            }

                            // Sync to LocalDataManager
                            com.example.cleanify_application.models.User localUser = new com.example.cleanify_application.models.User();
                            localUser.setUid(uid);
                            localUser.setFullName(name);
                            localUser.setEmail(email);
                            localUser.setPhone(phone);
                            localUser.setRole(role);
                            localUser.setProfileImageUrl(profileUrl);
                            if ("staff".equals(role)) {
                                localUser.setAssignedHostel(hostel);
                                localUser.setHousehelpId(room);
                            } else {
                                localUser.setHostelId(hostel);
                                localUser.setRoomNumber(room);
                            }
                            com.example.cleanify_application.utils.LocalDataManager.getInstance(EditProfileActivity.this).saveUser(localUser);
                        }
                    } catch (Exception ignored) { }
                });
            }
        });
    }

    private void saveProfile() {
        if (supabaseClient == null || !supabaseClient.isAuthenticated()) return;

        String uid = supabaseClient.getUserId();
        String newName  = etFullName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newHostel = etHostel.getText().toString().trim();
        String newRoom   = etRoom.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("full_name", newName);
        updates.put("email", newEmail);
        updates.put("phone", newPhone);

        String role = supabaseClient.getUserRole();
        if ("staff".equals(role)) {
            updates.put("assigned_hostel", newHostel);
            updates.put("househelp_id", newRoom);
        } else {
            updates.put("hostel_id", newHostel);
            updates.put("room_number", newRoom);
        }

        if (selectedImageUriStr != null) {
            updates.put("profile_image_url", selectedImageUriStr);
        }

        btnSaveChanges.setEnabled(false);
        String table = "student".equals(role) ? "students" : "staff";

        supabaseClient.update(this, table, "id=eq." + uid, updates, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveChanges.setEnabled(true);
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    // Update SupabaseClient in-memory name so dashboard refreshes correctly
                    supabaseClient.setUserName(newName);

                    // Also update LocalDataManager cache for offline access
                    com.example.cleanify_application.utils.LocalDataManager ldm =
                            com.example.cleanify_application.utils.LocalDataManager.getInstance(EditProfileActivity.this);
                    com.example.cleanify_application.models.User localUser = ldm.getUserById(uid);
                    if (localUser == null) localUser = new com.example.cleanify_application.models.User();
                    localUser.setUid(uid);
                    localUser.setFullName(newName);
                    localUser.setEmail(newEmail);
                    localUser.setPhone(newPhone);
                    if ("staff".equals(role)) {
                        localUser.setAssignedHostel(newHostel);
                    } else {
                        localUser.setHostelId(newHostel);
                        localUser.setRoomNumber(newRoom);
                    }
                    if (selectedImageUriStr != null) localUser.setProfileImageUrl(selectedImageUriStr);
                    ldm.saveUser(localUser);

                    Toast.makeText(EditProfileActivity.this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Signal dashboard to reload
                    finish();
                });
            }
        });
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
}
    