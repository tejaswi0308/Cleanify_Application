package com.example.cleanify_application;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.network.SupabaseClient;
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
    private Button btnSaveChanges, btnChangePhoto;
    private ImageView btnBack;
    private SupabaseClient supabaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        supabaseClient = SupabaseClient.getInstance(this);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etHostel = findViewById(R.id.etHostel);
        etRoom = findViewById(R.id.etRoom);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        loadProfileData();

        btnSaveChanges.setOnClickListener(v -> saveProfile());

        btnChangePhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Photo upload coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    // Removed loadDemoProfileData

    private void loadProfileData() {
        supabaseClient.select(EditProfileActivity.this, "users", "limit=1", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    JsonArray jsonArray = JsonParser.parseString(jsonStr).getAsJsonArray();
                    if (jsonArray.size() > 0) {
                        JsonObject obj = jsonArray.get(0).getAsJsonObject();
                        runOnUiThread(() -> {
                            etFullName.setText(obj.has("full_name") && !obj.get("full_name").isJsonNull() ? obj.get("full_name").getAsString() : "");
                            etEmail.setText(obj.has("email") && !obj.get("email").isJsonNull() ? obj.get("email").getAsString() : "");
                            etPhone.setText(obj.has("phone") && !obj.get("phone").isJsonNull() ? obj.get("phone").getAsString() : "");
                            etHostel.setText(obj.has("hostel_id") && !obj.get("hostel_id").isJsonNull() ? obj.get("hostel_id").getAsString() : "");
                            etRoom.setText(obj.has("room_number") && !obj.get("room_number").isJsonNull() ? obj.get("room_number").getAsString() : "");
                        });
                    }
                }
            }
        });
    }

    private void saveProfile() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("full_name", etFullName.getText().toString().trim());
        updates.put("phone", etPhone.getText().toString().trim());
        updates.put("hostel_id", etHostel.getText().toString().trim());
        updates.put("room_number", etRoom.getText().toString().trim());

        btnSaveChanges.setEnabled(false);
        // Using a generic update assuming RLS scopes it to the current user
        // Typically, we would pass the user ID. But without a JWT decoder, we do our best here.
        // E.g., email=eq.the_email
        String currentEmail = etEmail.getText().toString().trim();
        supabaseClient.update(EditProfileActivity.this, "users", "email=eq." + currentEmail, updates, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(EditProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveChanges.setEnabled(true);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                        btnSaveChanges.setEnabled(true);
                    });
                }
            }
        });
    }
}
