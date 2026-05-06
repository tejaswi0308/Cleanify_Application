package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cleanify_application.adapters.RequestHistoryAdapter;
import com.example.cleanify_application.models.CleaningRequest;
import com.example.cleanify_application.network.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StudentDashboard";
    private TextView tvUserName, tvRoomInfo, tvLastCleaned, tvTotalRequests, tvActiveStatus;
    private Button btnNewRequest, btnCancelActive;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnViewHistory;
    private ImageView btnProfile;
    private RecyclerView rvHistory;
    private LinearLayout activeCleaningCard;
    private TextView tvActiveStaffName, tvActiveCleaningType, tvActiveTimer;
    private SupabaseClient supabaseClient;
    private List<CleaningRequest> requestHistory;
    private RequestHistoryAdapter adapter;
    private CountDownTimer countDownTimer;
    private CleaningRequest activeRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        try {
            supabaseClient = SupabaseClient.getInstance(this);
        } catch (Exception e) {
            Log.e(TAG, "Supabase not configured: " + e.getMessage());
        }

        initViews();

        if (supabaseClient == null || !supabaseClient.isAuthenticated()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        loadUserData();
        loadRequestHistory();

        btnNewRequest.setOnClickListener(v -> {
            startActivity(new Intent(this, NewRequestActivity.class));
        });

        btnProfile.setOnClickListener(v -> {
            // Use startActivityForResult so we can reload dashboard on profile save
            startActivityForResult(new Intent(this, EditProfileActivity.class), 101);
        });

        btnCancelActive.setOnClickListener(v -> cancelActiveRequest());

        btnViewHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentHistoryActivity.class));
        });

        activeCleaningCard.setOnClickListener(v -> {
            if (activeRequest != null) {
                Intent intent = new Intent(this, CleaningProgressActivity.class);
                intent.putExtra("requestId", activeRequest.getRequestId());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (supabaseClient != null && supabaseClient.isAuthenticated()) {
            loadUserData();
            loadRequestHistory();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null)
            countDownTimer.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Reload dashboard when profile is saved in EditProfileActivity
        if (requestCode == 101 && resultCode == RESULT_OK) {
            if (supabaseClient != null && supabaseClient.isAuthenticated()) {
                loadUserData();
                loadRequestHistory();
            }
        }
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvRoomInfo = findViewById(R.id.tvRoomInfo);
        tvLastCleaned = findViewById(R.id.tvLastCleaned);
        tvTotalRequests = findViewById(R.id.tvTotalRequests);
        tvActiveStatus = findViewById(R.id.tvActiveStatus);
        btnNewRequest = findViewById(R.id.btnNewRequest);
        btnProfile = findViewById(R.id.btnProfile);
        rvHistory = findViewById(R.id.rvHistory);
        activeCleaningCard = findViewById(R.id.activeCleaningCard);
        tvActiveStaffName = findViewById(R.id.tvActiveStaffName);
        tvActiveCleaningType = findViewById(R.id.tvActiveCleaningType);
        tvActiveTimer = findViewById(R.id.tvActiveTimer);
        btnCancelActive = findViewById(R.id.btnCancelActive);
        btnViewHistory = findViewById(R.id.btnViewHistory);

        requestHistory = new ArrayList<>();
        adapter = new RequestHistoryAdapter(requestHistory, request -> {
            if ("in_progress".equals(request.getStatus()) || "assigned".equals(request.getStatus())) {
                Intent intent = new Intent(this, CleaningProgressActivity.class);
                intent.putExtra("requestId", request.getRequestId());
                startActivity(intent);
            } else if ("completed".equals(request.getStatus())) {
                Intent intent = new Intent(this, FeedbackActivity.class);
                intent.putExtra("requestId", request.getRequestId());
                startActivity(intent);
            }
        });
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
    }

    private void showActiveCleaningCard(String staffName, String cleaningType, int minutes) {
        activeCleaningCard.setVisibility(View.VISIBLE);
        tvActiveStaffName.setText(staffName);
        tvActiveCleaningType.setText(cleaningType);

        // Cancel any existing timer
        if (countDownTimer != null)
            countDownTimer.cancel();

        // Parse the scheduled date and time from the active request
        long scheduledTimeMs = getScheduledTimeMs();

        if (scheduledTimeMs > 0) {
            long now = System.currentTimeMillis();
            long cleaningDurationMs = minutes * 60 * 1000L;

            if (now < scheduledTimeMs) {
                // Scheduled time hasn't arrived yet — show countdown to start
                String statusMsg = "Scheduled";
                if (activeRequest != null) {
                    if ("assigned".equals(activeRequest.getStatus())) {
                        statusMsg = "Staff Assigned - Starts at " + activeRequest.getTimeSlot();
                    } else if ("pending".equals(activeRequest.getStatus())) {
                        statusMsg = "Awaiting Staff Assignment";
                    } else {
                        statusMsg = "Scheduled for " + activeRequest.getTimeSlot();
                    }
                }
                tvActiveStatus.setText(statusMsg);

                long msUntilStart = scheduledTimeMs - now;
                tvActiveTimer.setText(formatTime(msUntilStart));

                // Countdown until the scheduled start time
                countDownTimer = new CountDownTimer(msUntilStart, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        tvActiveTimer.setText(formatTime(millisUntilFinished));
                    }

                    @Override
                    public void onFinish() {
                        tvActiveStatus.setText("Cleaning In Progress");
                        startCleaningTimer(cleaningDurationMs);
                    }
                };
                countDownTimer.start();
            } else { //ensure that the app doesn't always show the scheduled time
                // Scheduled time has passed — calculate remaining cleaning time (whenver the student open the app next time he could see the updated time)
                long elapsed = now - scheduledTimeMs; //ensures that even if the student open the app after the scheduled time it will show the correct remaining time
                long remaining = cleaningDurationMs - elapsed;

                if (remaining > 0) { //if the remaining time is greater than 0 it means the cleaning is not completed yet
                    tvActiveStatus.setText("Cleaning In Progress");
                    startCleaningTimer(remaining);
                } else { //if the remaining time is 0 it means the cleaning is completed
                    tvActiveStatus.setText("Cleaning Time Completed");
                    tvActiveTimer.setText("00:00");
                }
            }
        } else {
            // Could not parse scheduled time — fall back
            if (activeRequest != null && "pending".equals(activeRequest.getStatus())) {
                tvActiveStatus.setText("Request Pending - Awaiting Assignment");
                tvActiveTimer.setText("--:--");
            } else {
                tvActiveStatus.setText("Cleaning In Progress");
                startCleaningTimer(minutes * 60 * 1000L);
            }
        }  //(If for some reason the date/time string from the database is corrupted or missing)
    }

    private long getScheduledTimeMs() { //to get the scheduled time in milliseconds 
        if (activeRequest == null)  
            return -1;

        String dateStr = activeRequest.getDate();
        String timeSlot = activeRequest.getTimeSlot();

        if (dateStr == null || timeSlot == null || dateStr.isEmpty() || timeSlot.isEmpty()) {
            return -1;
        }

        String combined = dateStr + " " + timeSlot;
        String[] dateFormats = {
                "dd MMM yyyy hh:mm a",
                "MMM dd, yyyy hh:mm a", //timestamps are in this format
                "dd-MM-yyyy hh:mm a",
                "yyyy-MM-dd hh:mm a"
        };

        for (String format : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                Date parsed = sdf.parse(combined);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (ParseException ignored) {
            }
        }

        return -1;
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void startCleaningTimer(long durationMs) {
        if (countDownTimer != null)
            countDownTimer.cancel();
        countDownTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvActiveTimer.setText(formatTime(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                tvActiveTimer.setText("00:00");
                tvActiveStatus.setText("Cleaning Time Completed");
                Toast.makeText(StudentDashboardActivity.this, "Cleaning time completed!", Toast.LENGTH_SHORT).show();
            }
        };
        countDownTimer.start();
    }

    private void hideActiveCleaningCard() { //if the cleaning is completed then hide the active cleaning card
        activeCleaningCard.setVisibility(View.GONE); // hide the active cleaning card
        if (countDownTimer != null)
            countDownTimer.cancel(); // cancel the timer
    }

    private void cancelActiveRequest() {
        if (activeRequest != null && activeRequest.getRequestId() != null && supabaseClient != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "cancelled");

            supabaseClient.update(this, "cleaning_requests",
                    "request_id=eq." + activeRequest.getRequestId(), updates, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> Toast.makeText(StudentDashboardActivity.this,
                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(Call call, Response response) {
                            runOnUiThread(() -> {
                                hideActiveCleaningCard();
                                tvActiveStatus.setText(getString(R.string.no_active_request));
                                Toast.makeText(StudentDashboardActivity.this, "Request cancelled", Toast.LENGTH_SHORT)
                                        .show();
                                loadRequestHistory();
                            });
                        }
                    });
        }
    }

    private void loadUserData() {
        if (supabaseClient == null || !supabaseClient.isAuthenticated())
            return;

        String uid = supabaseClient.getUserId();
        supabaseClient.select(this, "students", "id=eq." + uid, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading user data: " + e.getMessage());
                    tvUserName.setText("Student");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                runOnUiThread(() -> {
                    try {
                        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                        if (arr.size() > 0) {
                            JsonObject user = arr.get(0).getAsJsonObject();
                            String name = getStr(user, "full_name");
                            String room = getStr(user, "room_number");
                            String hostel = getStr(user, "hostel_id");
                            String phone = getStr(user, "phone");
                            String email = getStr(user, "email");
                            String profileUrl = getStr(user, "profile_image_url");

                            // Update UI
                            tvUserName.setText(name.isEmpty() ? "Student" : name);
                            tvRoomInfo.setText("Room no. " + room + ", " + hostel);

                            if (!profileUrl.isEmpty() && btnProfile != null) {
                                com.bumptech.glide.Glide.with(StudentDashboardActivity.this)
                                        .load(android.net.Uri.parse(profileUrl))
                                        .circleCrop()
                                        .into(btnProfile);
                            }

                            // Update SupabaseClient cached name
                            if (!name.isEmpty())
                                supabaseClient.setUserName(name);

                            // Sync to LocalDataManager so EditProfile and other screens work
                            com.example.cleanify_application.models.User localUser = new com.example.cleanify_application.models.User();
                            localUser.setUid(uid);
                            localUser.setFullName(name);
                            localUser.setEmail(email);
                            localUser.setPhone(phone);
                            localUser.setRoomNumber(room);
                            localUser.setHostelId(hostel);
                            localUser.setProfileImageUrl(profileUrl);
                            localUser.setRole("student");
                            com.example.cleanify_application.utils.LocalDataManager
                                    .getInstance(StudentDashboardActivity.this).saveUser(localUser);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse error: " + e.getMessage());
                        tvUserName.setText("Student");
                    }
                });
            }
        });
    }

    private void loadRequestHistory() {
        if (supabaseClient == null || !supabaseClient.isAuthenticated())
            return;

        String uid = supabaseClient.getUserId();
        supabaseClient.select(this, "cleaning_requests",
                "student_id=eq." + uid + "&order=created_at.desc", new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Error loading history: " + e.getMessage());
                            tvTotalRequests.setText("0");
                            tvLastCleaned.setText("N/A");
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "[]";
                        runOnUiThread(() -> {
                            try {
                                JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                                requestHistory.clear();
                                int totalCount = 0;
                                boolean hasActive = false;
                                String lastCleanedText = "N/A";

                                for (int i = 0; i < arr.size(); i++) {
                                    JsonObject obj = arr.get(i).getAsJsonObject();
                                    CleaningRequest request = parseCleaningRequest(obj);
                                    totalCount++;

                                    String status = request.getStatus() != null ? request.getStatus().toLowerCase()
                                            : "";
                                    if (!"completed".equals(status) && !"cancelled".equals(status)) {
                                        hasActive = true;
                                        activeRequest = request;
                                        String staffName;
                                        if ("pending".equals(status)) {
                                            staffName = "Awaiting Staff Assignment";
                                        } else {
                                            staffName = request.getAssignedStaffName() != null
                                                    ? request.getAssignedStaffName()
                                                    : "Staff Member";
                                        }
                                        String type = request.getCleaningType() != null ? request.getCleaningType()
                                                : "Cleaning";
                                        showActiveCleaningCard(staffName, type,
                                                request.getEstimatedMinutes() > 0 ? request.getEstimatedMinutes() : 25);
                                        btnCancelActive.setText(R.string.cancel_request);
                                        btnCancelActive.setOnClickListener(v -> cancelActiveRequest());
                                    } else {
                                        requestHistory.add(request);
                                    }

                                    if ("completed".equals(status) && "N/A".equals(lastCleanedText)) {
                                        lastCleanedText = request.getDate() != null ? request.getDate() : "N/A";
                                    }
                                }

                                if (!hasActive) {
                                    tvActiveStatus.setText(getString(R.string.no_active_request));
                                    hideActiveCleaningCard();
                                }
                                tvTotalRequests.setText(String.valueOf(totalCount));
                                tvLastCleaned.setText(lastCleanedText);
                                adapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                Log.e(TAG, "Parse error: " + e.getMessage());
                            }
                        });
                    }
                });
    }

    private CleaningRequest parseCleaningRequest(JsonObject obj) {
        CleaningRequest req = new CleaningRequest();
        req.setRequestId(getStr(obj, "request_id"));
        req.setStudentId(getStr(obj, "student_id"));
        req.setStudentName(getStr(obj, "student_name"));
        req.setHostel(getStr(obj, "hostel"));
        req.setRoomNumber(getStr(obj, "room_number"));
        req.setFloorNumber(getStr(obj, "floor_number"));
        req.setCleaningType(getStr(obj, "cleaning_type"));
        req.setDate(getStr(obj, "date"));
        req.setTimeSlot(getStr(obj, "time_slot"));
        req.setAdditionalNotes(getStr(obj, "additional_notes"));
        req.setStatus(getStr(obj, "status"));
        req.setAssignedStaffName(getStr(obj, "assigned_staff_name"));
        if (obj.has("estimated_minutes") && !obj.get("estimated_minutes").isJsonNull()) {
            req.setEstimatedMinutes(obj.get("estimated_minutes").getAsInt());
        }
        if (obj.has("rating") && !obj.get("rating").isJsonNull()) {
            req.setRating(obj.get("rating").getAsInt());
        }
        if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
            try {
                // Supabase returns ISO 8601 strings usually, but if it's a long we handle it
                String createdAtStr = obj.get("created_at").getAsString();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date = sdf.parse(createdAtStr);
                if (date != null)
                    req.setCreatedAt(date.getTime());
            } catch (Exception e) {
                try {
                    req.setCreatedAt(obj.get("created_at").getAsLong());
                } catch (Exception ignored) {
                }
            }
        }
        return req;
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
}
