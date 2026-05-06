package com.example.cleanify_application;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.models.CleaningRequest;
import com.example.cleanify_application.network.SupabaseClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class NewRequestActivity extends AppCompatActivity {

    private EditText etHostel, etRoom, etDate, etNotes;
    private Spinner spinnerCleaningType, spinnerTimeSlot;
    private Button btnSubmitRequest;
    private ImageView btnBack;
    private SupabaseClient supabaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_request);

        try {
            supabaseClient = SupabaseClient.getInstance(this);
        } catch (Exception e) { /* not configured */ }

        initViews();
        setupDateField();
        setupSpinners();

        String uid = supabaseClient != null && supabaseClient.isAuthenticated() ? supabaseClient.getUserId() : com.example.cleanify_application.utils.LocalDataManager.getInstance(this).getLoggedInUserId();
        com.example.cleanify_application.models.User localUser = com.example.cleanify_application.utils.LocalDataManager.getInstance(this).getUserById(uid);
        if (localUser != null) {
            etHostel.setText(localUser.getHostelId() != null && !localUser.getHostelId().isEmpty() ? localUser.getHostelId() : "");
            etRoom.setText(localUser.getRoomNumber() != null && !localUser.getRoomNumber().isEmpty() ? localUser.getRoomNumber() : "");
        }

        loadUserLocationData();

        btnBack.setOnClickListener(v -> finish());
        btnSubmitRequest.setOnClickListener(v -> submitRequest());
    }

    private void initViews() {
        etHostel = findViewById(R.id.etHostel);
        etRoom = findViewById(R.id.etRoom);
        
        // Make them fixed (read-only) as per user request
        etHostel.setEnabled(false);
        etHostel.setFocusable(false);
        etHostel.setClickable(false);
        
        etRoom.setEnabled(false);
        etRoom.setFocusable(false);
        etRoom.setClickable(false);
        etDate = findViewById(R.id.etDate);
        etNotes = findViewById(R.id.etNotes);
        spinnerCleaningType = findViewById(R.id.spinnerCleaningType);
        spinnerTimeSlot = findViewById(R.id.spinnerTimeSlot);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String todayStr = sdf.format(new Date());
        etDate.setText(todayStr);
        etDate.setEnabled(false);
        etDate.setFocusable(false);
        etDate.setClickable(false);
    }

    private void setupSpinners() {
        String[] cleaningTypes = {"Regular Cleaning", "Deep Cleaning", "Normal Cleaning"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, cleaningTypes);
        spinnerCleaningType.setAdapter(typeAdapter);

        List<String> timeSlots = new ArrayList<>();
        timeSlots.add("Select");
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 48; i++) {
            if (cal.after(now)) {
                timeSlots.add(timeFormat.format(cal.getTime()));
            }
            cal.add(Calendar.MINUTE, 30);
        }

        if (timeSlots.size() == 1) {
            // It's past the last slot, roll over to tomorrow
            now.add(Calendar.DAY_OF_YEAR, 1);
            
            // Reset cal to tomorrow 00:00
            cal.setTime(now.getTime());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            
            // Add all 48 slots for tomorrow
            for (int i = 0; i < 48; i++) {
                timeSlots.add(timeFormat.format(cal.getTime()));
                cal.add(Calendar.MINUTE, 30);
            }
            
            // Update the Date text field to show tomorrow's date
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            etDate.setText(sdf.format(now.getTime()));
        }

        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, timeSlots);
        spinnerTimeSlot.setAdapter(timeAdapter);
    }

    private void loadUserLocationData() {
        if (supabaseClient == null || !supabaseClient.isAuthenticated()) return;

        String uid = supabaseClient.getUserId();
        supabaseClient.select(this, "students", "id=eq." + uid, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                runOnUiThread(() -> {
                    try {
                        com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(body).getAsJsonArray();
                        if (arr.size() > 0) {
                            com.google.gson.JsonObject user = arr.get(0).getAsJsonObject();
                            if (user.has("hostel_id") && !user.get("hostel_id").isJsonNull()) {
                                etHostel.setText(user.get("hostel_id").getAsString());
                            }
                            if (user.has("room_number") && !user.get("room_number").isJsonNull()) {
                                etRoom.setText(user.get("room_number").getAsString());
                            }
                        }
                    } catch (Exception ignored) { }
                });
            }
        });
    }

    private void submitRequest() {
        String hostel = etHostel.getText().toString().trim();
        String room = etRoom.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String type = spinnerCleaningType.getSelectedItem().toString();
        String timeSlot = spinnerTimeSlot.getSelectedItem().toString();
        String notes = etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(hostel) || TextUtils.isEmpty(room) || "Select".equals(timeSlot) || "No more slots today".equals(timeSlot)) {
            Toast.makeText(this, "Please fill required fields (Hostel/Room) and select a valid time", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Submitting for Room: " + room, Toast.LENGTH_SHORT).show();
        btnSubmitRequest.setEnabled(false);

        if (supabaseClient == null || !supabaseClient.isAuthenticated()) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            btnSubmitRequest.setEnabled(true);
            return;
        }

        String trimmedHostel = hostel.trim();
        String encodedHostel = trimmedHostel;
        try {
            encodedHostel = java.net.URLEncoder.encode(trimmedHostel, "UTF-8");
        } catch (Exception ignored) {}

        // First, fetch all staff in this hostel
        supabaseClient.select(this, "staff", "assigned_hostel=eq." + encodedHostel, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Fallback to global search if hostel search fails
                runOnUiThread(() -> fetchAllStaffAndSubmit(hostel, room, date, timeSlot, type, notes));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String staffBody = response.body() != null ? response.body().string() : "[]";
                runOnUiThread(() -> {
                    try {
                        com.google.gson.JsonArray staffArr = com.google.gson.JsonParser.parseString(staffBody).getAsJsonArray();
                        if (staffArr.size() > 0) {
                            // Now fetch active requests to find busy staff
                            supabaseClient.select(NewRequestActivity.this, "cleaning_requests", "status=in.(assigned,in_progress)", new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    runOnUiThread(() -> pickRandomAndSubmit(staffArr, hostel, room, date, timeSlot, type, notes));
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    String reqBody = response.body() != null ? response.body().string() : "[]";
                                    runOnUiThread(() -> {
                                        try {
                                            com.google.gson.JsonArray reqArr = com.google.gson.JsonParser.parseString(reqBody).getAsJsonArray();
                                            java.util.Set<String> busyStaffIds = new java.util.HashSet<>();
                                            for (int i = 0; i < reqArr.size(); i++) {
                                                com.google.gson.JsonObject r = reqArr.get(i).getAsJsonObject();
                                                if (r.has("assigned_staff_id") && !r.get("assigned_staff_id").isJsonNull()) {
                                                    busyStaffIds.add(r.get("assigned_staff_id").getAsString());
                                                }
                                            }

                                            // Filter available staff
                                            com.google.gson.JsonArray availableStaff = new com.google.gson.JsonArray();
                                            for (int i = 0; i < staffArr.size(); i++) {
                                                com.google.gson.JsonObject s = staffArr.get(i).getAsJsonObject();
                                                if (!busyStaffIds.contains(s.get("id").getAsString())) {
                                                    availableStaff.add(s);
                                                }
                                            }

                                            if (availableStaff.size() > 0) {
                                                com.google.gson.JsonObject staffObj = availableStaff.get(new java.util.Random().nextInt(availableStaff.size())).getAsJsonObject();
                                                String staffId = staffObj.get("id").getAsString();
                                                String staffName = staffObj.has("full_name") && !staffObj.get("full_name").isJsonNull() ? staffObj.get("full_name").getAsString() : "Staff Member";
                                                submitToSupabase(staffId, staffName, hostel, room, date, timeSlot, type, notes, "assigned");
                                            } else {
                                                // All staff in this hostel are busy, try global search
                                                fetchAllStaffAndSubmit(hostel, room, date, timeSlot, type, notes);
                                            }
                                        } catch (Exception e) {
                                            pickRandomAndSubmit(staffArr, hostel, room, date, timeSlot, type, notes);
                                        }
                                    });
                                }
                            });
                        } else {
                            // No staff found for this specific hostel name, check any staff
                            fetchAllStaffAndSubmit(hostel, room, date, timeSlot, type, notes);
                        }
                    } catch (Exception e) {
                        fetchAllStaffAndSubmit(hostel, room, date, timeSlot, type, notes);
                    }
                });
            }
        });
    }

    private void pickRandomAndSubmit(com.google.gson.JsonArray staffArr, String hostel, String room, String date, String timeSlot, String type, String notes) {
        try {
            com.google.gson.JsonObject staffObj = staffArr.get(new java.util.Random().nextInt(staffArr.size())).getAsJsonObject();
            String staffId = staffObj.get("id").getAsString();
            String staffName = staffObj.has("full_name") && !staffObj.get("full_name").isJsonNull() ? staffObj.get("full_name").getAsString() : "Staff Member";
            submitToSupabase(staffId, staffName, hostel, room, date, timeSlot, type, notes, "assigned");
        } catch (Exception e) {
            submitToSupabase("", "", hostel, room, date, timeSlot, type, notes, "pending");
        }
    }

    private void fetchAllStaffAndSubmit(String hostel, String room, String date, String timeSlot, String type, String notes) {
        supabaseClient.select(this, "staff", "", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> submitToSupabase("", "", hostel, room, date, timeSlot, type, notes, "pending"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String staffBody = response.body() != null ? response.body().string() : "[]";
                runOnUiThread(() -> {
                    try {
                        com.google.gson.JsonArray staffArr = com.google.gson.JsonParser.parseString(staffBody).getAsJsonArray();
                        if (staffArr.size() > 0) {
                            // Check for busy staff across all staff
                            supabaseClient.select(NewRequestActivity.this, "cleaning_requests", "status=in.(assigned,in_progress)", new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    runOnUiThread(() -> pickRandomAndSubmit(staffArr, hostel, room, date, timeSlot, type, notes));
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    String reqBody = response.body() != null ? response.body().string() : "[]";
                                    runOnUiThread(() -> {
                                        try {
                                            com.google.gson.JsonArray reqArr = com.google.gson.JsonParser.parseString(reqBody).getAsJsonArray();
                                            java.util.Set<String> busyStaffIds = new java.util.HashSet<>();
                                            for (int i = 0; i < reqArr.size(); i++) {
                                                com.google.gson.JsonObject r = reqArr.get(i).getAsJsonObject();
                                                if (r.has("assigned_staff_id") && !r.get("assigned_staff_id").isJsonNull()) {
                                                    busyStaffIds.add(r.get("assigned_staff_id").getAsString());
                                                }
                                            }

                                            com.google.gson.JsonArray availableStaff = new com.google.gson.JsonArray();
                                            for (int i = 0; i < staffArr.size(); i++) {
                                                com.google.gson.JsonObject s = staffArr.get(i).getAsJsonObject();
                                                if (!busyStaffIds.contains(s.get("id").getAsString())) {
                                                    availableStaff.add(s);
                                                }
                                            }

                                            if (availableStaff.size() > 0) {
                                                com.google.gson.JsonObject staffObj = availableStaff.get(new java.util.Random().nextInt(availableStaff.size())).getAsJsonObject();
                                                String staffId = staffObj.get("id").getAsString();
                                                String staffName = staffObj.has("full_name") && !staffObj.get("full_name").isJsonNull() ? staffObj.get("full_name").getAsString() : "Staff Member";
                                                submitToSupabase(staffId, staffName, hostel, room, date, timeSlot, type, notes, "assigned");
                                            } else {
                                                submitToSupabase("", "", hostel, room, date, timeSlot, type, notes, "pending");
                                            }
                                        } catch (Exception e) {
                                            pickRandomAndSubmit(staffArr, hostel, room, date, timeSlot, type, notes);
                                        }
                                    });
                                }
                            });
                        } else {
                            submitToSupabase("", "", hostel, room, date, timeSlot, type, notes, "pending");
                        }
                    } catch (Exception e) {
                        submitToSupabase("", "", hostel, room, date, timeSlot, type, notes, "pending");
                    }
                });
            }
        });
    }

    private void submitToSupabase(String staffId, String staffName, String hostel, String room, String date, String timeSlot, String type, String notes, String status) {
        String uid = supabaseClient.getUserId();

        java.util.Map<String, Object> reqMap = new java.util.HashMap<>();
        reqMap.put("student_id", uid);
        reqMap.put("student_name", com.example.cleanify_application.utils.LocalDataManager.getInstance(this).getUserById(uid) != null ? com.example.cleanify_application.utils.LocalDataManager.getInstance(this).getUserById(uid).getFullName() : "Student");
        reqMap.put("hostel", hostel);
        reqMap.put("room_number", room);
        reqMap.put("date", date);
        reqMap.put("time_slot", timeSlot);
        reqMap.put("cleaning_type", type);
        reqMap.put("additional_notes", notes);
        reqMap.put("status", status);
        if (staffId != null && !staffId.isEmpty()) {
            reqMap.put("assigned_staff_id", staffId);
        }
        if (staffName != null && !staffName.isEmpty()) {
            reqMap.put("assigned_staff_name", staffName);
        }
        // Let Supabase handle created_at with now() default
        reqMap.put("estimated_minutes", 25);

        supabaseClient.insert(this, "cleaning_requests", reqMap, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(NewRequestActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmitRequest.setEnabled(true);
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(NewRequestActivity.this, "Request submitted to Supabase!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        try {
                            String err = response.body() != null ? response.body().string() : "Unknown error";
                            android.util.Log.e("NewRequest", "Insert failed: " + err);
                            Toast.makeText(NewRequestActivity.this, "Database Error: " + response.code(), Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(NewRequestActivity.this, "Database Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                        btnSubmitRequest.setEnabled(true);
                    }
                });
            }
        });
    }
}
