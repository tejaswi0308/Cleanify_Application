package com.example.cleanify_application;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.models.CleaningRequest;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class NewRequestActivity extends AppCompatActivity {

    private EditText etHostel, etRoom, etDate, etNotes;
    private Spinner spinnerCleaningType, spinnerTimeSlot;
    private Button btnSubmitRequest;
    private ImageView btnBack;
    private SupabaseClient supabaseClient;
    private Calendar selectedDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_request);

        supabaseClient = SupabaseClient.getInstance(this);

        initViews();
        setupSpinners();

        loadUserLocationData();

        etDate.setOnClickListener(v -> showDatePicker());
        btnBack.setOnClickListener(v -> finish());
        btnSubmitRequest.setOnClickListener(v -> submitRequest());
    }

    private void initViews() {
        etHostel = findViewById(R.id.etHostel);
        etRoom = findViewById(R.id.etRoom);
        etDate = findViewById(R.id.etDate);
        etNotes = findViewById(R.id.etNotes);
        spinnerCleaningType = findViewById(R.id.spinnerCleaningType);
        spinnerTimeSlot = findViewById(R.id.spinnerTimeSlot);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupSpinners() {
        String[] cleaningTypes = {"Regular Cleaning", "Deep Cleaning", "Normal Cleaning"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, cleaningTypes);
        spinnerCleaningType.setAdapter(typeAdapter);

        String[] timeSlots = {"Select", "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
                "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"};
        ArrayAdapter<String> slotAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, timeSlots);
        spinnerTimeSlot.setAdapter(slotAdapter);
    }

    private void loadUserLocationData() {
        // We'll skip pre-loading for now since user is handled via JWT and we'd need to parse JWT or keep UID stored.
        // For simplicity in Supabase, we rely on user input if not cached, or we could fetch the user profile.
        // Assuming we store UID in SharedPreferences or parse it from JWT. 
        // For this demo, let's let the user type it or leave it blank if they don't know it.
    }

    private void showDatePicker() {
        DatePickerDialog picker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    etDate.setText(sdf.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private void submitRequest() {
        String hostel = etHostel.getText().toString().trim();
        String room = etRoom.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String timeSlot = spinnerTimeSlot.getSelectedItem().toString();
        String cleaningType = spinnerCleaningType.getSelectedItem().toString();
        String notes = etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(hostel) || TextUtils.isEmpty(room) || TextUtils.isEmpty(date)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("Select".equals(timeSlot)) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitRequest.setEnabled(false);
        btnSubmitRequest.setText("Submitting...");

        String requestId = UUID.randomUUID().toString();
        int durationMinutes = cleaningType.equals("Deep Cleaning") ? 45 : 25;

        // 2. Query for a free staff member
        supabaseClient.select(NewRequestActivity.this, "users", "role=eq.staff&status=eq.free&limit=1", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(NewRequestActivity.this, "Network error finding worker", Toast.LENGTH_SHORT).show();
                    btnSubmitRequest.setEnabled(true);
                    btnSubmitRequest.setText(R.string.submit_request);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    JsonArray workers = JsonParser.parseString(jsonStr).getAsJsonArray();
                    if (workers.size() == 0) {
                        runOnUiThread(() -> {
                            Toast.makeText(NewRequestActivity.this, "No available worker at this time. Please try later.", Toast.LENGTH_LONG).show();
                            btnSubmitRequest.setEnabled(true);
                            btnSubmitRequest.setText(R.string.submit_request);
                        });
                        return;
                    }
                    
                    JsonObject worker = workers.get(0).getAsJsonObject();
                    String assignedStaffId = worker.get("id").getAsString();
                    String assignedStaffName = worker.get("full_name").getAsString();

                    insertCleaningRequest(requestId, hostel, room, cleaningType, date, timeSlot, notes, durationMinutes, assignedStaffId, assignedStaffName);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(NewRequestActivity.this, "Error finding worker", Toast.LENGTH_SHORT).show();
                        btnSubmitRequest.setEnabled(true);
                        btnSubmitRequest.setText(R.string.submit_request);
                    });
                }
            }
        });
    }

    private void insertCleaningRequest(String requestId, String hostel, String room, String cleaningType, String date, String timeSlot, String notes, int durationMinutes, String assignedStaffId, String assignedStaffName) {
        String requestStatus = "assigned";

        // Convert date to YYYY-MM-DD for PostgreSQL DATE type compatibility
        String dbDate = date;
        try {
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date parsedDate = displayFormat.parse(date);
            if (parsedDate != null) {
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                dbDate = dbFormat.format(parsedDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Convert time_slot to standard format if needed (e.g., 04:00 PM to 16:00:00)
        String dbTime = timeSlot;
        try {
            SimpleDateFormat displayTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date parsedTime = displayTime.parse(timeSlot);
            if (parsedTime != null) {
                SimpleDateFormat dbFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                dbTime = dbFormat.format(parsedTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("request_id", requestId);
        requestMap.put("student_id", supabaseClient.getUserId());
        requestMap.put("student_name", "Student"); 
        requestMap.put("hostel", hostel);
        requestMap.put("room_number", room);
        requestMap.put("cleaning_type", cleaningType);
        requestMap.put("date", dbDate);
        requestMap.put("time_slot", dbTime);
        requestMap.put("additional_notes", notes);
        requestMap.put("status", requestStatus);
        requestMap.put("assigned_staff_id", assignedStaffId);
        requestMap.put("assigned_staff_name", assignedStaffName);
        requestMap.put("estimated_minutes", durationMinutes);

        // Insert request
        supabaseClient.insert(NewRequestActivity.this, "cleaning_requests", requestMap, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(NewRequestActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSubmitRequest.setEnabled(true);
                    btnSubmitRequest.setText(R.string.submit_request);
                });
            }

            @Override
            public void onResponse(Call call, Response insertResponse) throws IOException {
                if (insertResponse.isSuccessful()) {
                    // Update worker status to busy
                    Map<String, Object> statusUpdate = new HashMap<>();
                    statusUpdate.put("status", "busy");
                    supabaseClient.update(NewRequestActivity.this, "users", "id=eq." + assignedStaffId, statusUpdate, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {}
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {}
                    });

                    runOnUiThread(() -> {
                        Toast.makeText(NewRequestActivity.this, "Request assigned to " + assignedStaffName + "!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        String errorBody = "Unknown error";
                        try {
                            if (insertResponse.body() != null) {
                                errorBody = insertResponse.body().string();
                            }
                        } catch (Exception ignored) {}
                        Toast.makeText(NewRequestActivity.this, "Error: " + errorBody, Toast.LENGTH_LONG).show();
                        btnSubmitRequest.setEnabled(true);
                        btnSubmitRequest.setText(R.string.submit_request);
                    });
                }
            }
        });
    }
}
