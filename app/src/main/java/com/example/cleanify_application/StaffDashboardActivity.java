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

import com.example.cleanify_application.adapters.UpcomingTaskAdapter;
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

public class StaffDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StaffDashboard";
    private TextView tvActiveRoom, tvActiveType, tvActiveTime, tvActiveEst;
    private TextView tvStaffName, tvStaffInfo;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnHistory;
    private LinearLayout activeTaskCard;
    private TextView tvNoActiveTask;
    private Button btnMarkComplete;
    private RecyclerView rvUpcomingTasks;
    private SupabaseClient supabaseClient;
    private List<CleaningRequest> upcomingTasks;
    private UpcomingTaskAdapter adapter;
    private CleaningRequest activeRequest;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        try {
            supabaseClient = SupabaseClient.getInstance(this);
        } catch (Exception e) { /* not configured */ }

        initViews();

        if (supabaseClient == null || !supabaseClient.isAuthenticated()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        loadTasks();

        btnMarkComplete.setOnClickListener(v -> markAsComplete());

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, StaffHistoryActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (supabaseClient != null && supabaseClient.isAuthenticated()) {
            loadTasks();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void initViews() {
        activeTaskCard = findViewById(R.id.activeTaskCard);
        tvActiveRoom = findViewById(R.id.tvActiveRoom);
        tvActiveType = findViewById(R.id.tvActiveType);
        tvActiveTime = findViewById(R.id.tvActiveTime);
        tvActiveEst = findViewById(R.id.tvActiveEst);
        tvStaffName = findViewById(R.id.tvStaffName);
        tvStaffInfo = findViewById(R.id.tvStaffInfo);
        btnHistory = findViewById(R.id.btnHistory);
        btnMarkComplete = findViewById(R.id.btnMarkComplete);
        rvUpcomingTasks = findViewById(R.id.rvUpcomingTasks);

        android.widget.ImageView btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v -> {
            // Logout logic
            if (supabaseClient != null) supabaseClient.logout();
            com.example.cleanify_application.utils.LocalDataManager.getInstance(this).setLoggedInUser(null);
            
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        tvNoActiveTask = new TextView(this);
        tvNoActiveTask.setText("No active task");
        tvNoActiveTask.setTextColor(getResources().getColor(R.color.text_secondary));
        tvNoActiveTask.setTextSize(14);
        tvNoActiveTask.setPadding(0, 32, 0, 32);
        tvNoActiveTask.setGravity(android.view.Gravity.CENTER);
        tvNoActiveTask.setVisibility(View.GONE);

        LinearLayout parent = (LinearLayout) activeTaskCard.getParent();
        int index = parent.indexOfChild(activeTaskCard) + 1;
        parent.addView(tvNoActiveTask, index);

        upcomingTasks = new ArrayList<>();
        adapter = new UpcomingTaskAdapter(upcomingTasks, task -> {
            // Navigate to task details if needed
        });
        rvUpcomingTasks.setLayoutManager(new LinearLayoutManager(this));
        rvUpcomingTasks.setAdapter(adapter);
    }

    private void loadTasks() {
        if (supabaseClient == null || !supabaseClient.isAuthenticated()) return;

        loadUserProfile();

        // Fetch tasks that are pending OR assigned to this staff member
        supabaseClient.select(this, "cleaning_requests",
                "status=in.(pending,assigned,in_progress)&order=created_at.asc", new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Log.e(TAG, "Error loading tasks: " + e.getMessage()));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try (Response res = response) {
                            String body = res.body() != null ? res.body().string() : "[]";
                            runOnUiThread(() -> {
                                try {
                                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                                    upcomingTasks.clear();
                                    activeRequest = null;
                                    if (countDownTimer != null) countDownTimer.cancel();

                                    long nextRefreshMs = -1;
                                    long now = System.currentTimeMillis();
                                    String uid = supabaseClient.getUserId();

                                    for (int i = 0; i < arr.size(); i++) {
                                        JsonObject obj = arr.get(i).getAsJsonObject();
                                        CleaningRequest req = parseCleaningRequest(obj);

                                        String status = req.getStatus() != null ? req.getStatus().toLowerCase() : "";
                                        String assignedId = req.getAssignedStaffId();
                                        String room = req.getRoomNumber();

                                        // Move static rooms 502 and 503 to history if found
                                        if (("502".equals(room) || "503".equals(room)) && !"completed".equals(status) && !"cancelled".equals(status)) {
                                            updateTaskStatus(req.getRequestId(), "completed");
                                            continue; // Don't show in dashboard
                                        }

                                        // Only show unassigned pending, or assigned to ME
                                        if ("pending".equals(status) || (uid != null && uid.equalsIgnoreCase(assignedId))) {
                                            long scheduledTimeMs = getScheduledTimeMs(req);
                                            
                                            if (activeRequest == null && (scheduledTimeMs <= 0 || now >= scheduledTimeMs)) {
                                                activeRequest = req;
                                            } else {
                                                upcomingTasks.add(req);
                                                // Keep track of the soonest upcoming task to refresh the UI when it starts
                                                if (scheduledTimeMs > now) {
                                                    if (nextRefreshMs == -1 || scheduledTimeMs < nextRefreshMs) {
                                                        nextRefreshMs = scheduledTimeMs;
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Auto-refresh when the next task is supposed to start
                                    if (nextRefreshMs != -1) {
                                        long delay = nextRefreshMs - now;
                                        if (delay > 0) {
                                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(StaffDashboardActivity.this::loadTasks, delay + 1000);
                                        }
                                    }

                                    if (activeRequest != null) {
                                        activeTaskCard.setVisibility(View.VISIBLE);
                                        tvNoActiveTask.setVisibility(View.GONE);

                                        String roomStr = activeRequest.getRoomNumber() != null ? activeRequest.getRoomNumber() : "Unknown";
                                        String studentName = activeRequest.getStudentName() != null ? activeRequest.getStudentName() : "Student";
                                        tvActiveRoom.setText("Room " + roomStr + " (" + studentName + ")");
                                        tvActiveType.setText(activeRequest.getCleaningType() != null ? activeRequest.getCleaningType() : "Cleaning");

                                        long scheduledTimeMs = getScheduledTimeMs(activeRequest);

                                        if (scheduledTimeMs > 0) {
                                            if (now < scheduledTimeMs) {
                                                tvActiveTime.setText("Scheduled: " + activeRequest.getTimeSlot());
                                                long msUntilStart = scheduledTimeMs - now;
                                                tvActiveEst.setText("Starts in: " + formatTime(msUntilStart));

                                                countDownTimer = new CountDownTimer(msUntilStart, 1000) {
                                                    @Override
                                                    public void onTick(long millisUntilFinished) {
                                                        tvActiveEst.setText("Starts in: " + formatTime(millisUntilFinished));
                                                    }

                                                    @Override
                                                    public void onFinish() {
                                                        updateTaskStatus(activeRequest.getRequestId(), "in_progress");
                                                    }
                                                };
                                                countDownTimer.start();
                                            } else {
                                                tvActiveTime.setText("Started at: " + activeRequest.getTimeSlot());
                                                long durationMs = activeRequest.getEstimatedMinutes() * 60 * 1000L;
                                                long elapsed = now - scheduledTimeMs;
                                                long remaining = durationMs - elapsed;

                                                if (remaining > 0) {
                                                    if ("pending".equals(activeRequest.getStatus()) || "assigned".equals(activeRequest.getStatus())) {
                                                        updateTaskStatus(activeRequest.getRequestId(), "in_progress");
                                                    }
                                                    startCleaningTimer(remaining);
                                                } else {
                                                    tvActiveEst.setText("Completed");
                                                }
                                            }
                                        } else {
                                            tvActiveTime.setText("Time: " + (activeRequest.getTimeSlot() != null ? activeRequest.getTimeSlot() : "Now"));
                                            startCleaningTimer(activeRequest.getEstimatedMinutes() * 60 * 1000L);
                                        }
                                    } else {
                                        activeTaskCard.setVisibility(View.GONE);
                                        tvNoActiveTask.setVisibility(View.VISIBLE);
                                    }

                                    adapter.notifyDataSetChanged();
                                } catch (Exception e) {
                                    Log.e(TAG, "Parse error: " + e.getMessage());
                                }
                            });
                        }
                    }
                });
    }

    private void loadUserProfile() {
        if (supabaseClient == null || !supabaseClient.isAuthenticated()) return;

        String uid = supabaseClient.getUserId();
        supabaseClient.select(this, "staff", "id=eq." + uid, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvStaffName.setText("Staff Member"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response res = response) {
                    String body = res.body() != null ? res.body().string() : "[]";
                    runOnUiThread(() -> {
                        try {
                            JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                            if (arr.size() > 0) {
                                JsonObject user = arr.get(0).getAsJsonObject();
                                String name = getStr(user, "full_name");
                                tvStaffName.setText(name.isEmpty() ? "Staff Member" : name);
                                String hostel = getStr(user, "assigned_hostel");
                                String phone = getStr(user, "phone");
                                String email = getStr(user, "email");
                                tvStaffInfo.setText("Assigned Hostel: " + (hostel.isEmpty() ? "KCH" : hostel));

                                // Update cached name
                                if (!name.isEmpty()) supabaseClient.setUserName(name);

                                // Sync to LocalDataManager so EditProfile works
                                com.example.cleanify_application.models.User localUser =
                                        new com.example.cleanify_application.models.User();
                                localUser.setUid(uid);
                                localUser.setFullName(name);
                                localUser.setEmail(email);
                                localUser.setPhone(phone);
                                localUser.setAssignedHostel(hostel);
                                localUser.setProfileImageUrl(getStr(user, "profile_image_url"));
                                localUser.setRole("staff");
                                com.example.cleanify_application.utils.LocalDataManager
                                        .getInstance(StaffDashboardActivity.this).saveUser(localUser);

                                ImageView btnProfile = findViewById(R.id.btnProfile);
                                String profileUrl = getStr(user, "profile_image_url");
                                if (!profileUrl.isEmpty() && btnProfile != null) {
                                    com.bumptech.glide.Glide.with(StaffDashboardActivity.this)
                                            .load(android.net.Uri.parse(profileUrl))
                                            .circleCrop()
                                            .into(btnProfile);
                                }
                                if (btnProfile != null) {
                                    btnProfile.setOnClickListener(v -> {
                                        Toast.makeText(StaffDashboardActivity.this, "Profile viewing only for staff", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        } catch (Exception e) {
                            tvStaffName.setText("Staff Member");
                        }
                    });
                }
            }
        });
    }

    private void updateTaskStatus(String reqId, String status) {
        if (supabaseClient == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        if ("in_progress".equals(status)) {
            updates.put("assigned_staff_id", supabaseClient.getUserId());
            updates.put("start_time", com.example.cleanify_application.utils.DateTimeUtils.getCurrentIsoTimestamp());
        } else if ("completed".equals(status)) {
            updates.put("completed_at", com.example.cleanify_application.utils.DateTimeUtils.getCurrentIsoTimestamp());
            if (updates.get("assigned_staff_id") == null) {
                updates.put("assigned_staff_id", supabaseClient.getUserId());
            }
        }

        supabaseClient.update(this, "cleaning_requests", "request_id=eq." + reqId, updates, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response res = response) {
                    if (res.isSuccessful()) {
                        runOnUiThread(() -> loadTasks()); // Reload to get fresh state
                    }
                }
            }
        });
    }

    private void markAsComplete() {
        if (activeRequest != null) {
            Intent intent = new Intent(this, QRCodeActivity.class);
            intent.putExtra("requestId", activeRequest.getRequestId());
            intent.putExtra("qrCode", "VERIFY_" + activeRequest.getRequestId().substring(0, Math.min(6, activeRequest.getRequestId().length())));
            startActivity(intent);
        }
    }

    private void startCleaningTimer(long durationMs) {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvActiveEst.setText("Time Left: " + formatTime(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                tvActiveEst.setText("Time Left: 00:00");
                Toast.makeText(StaffDashboardActivity.this, "Cleaning duration complete!", Toast.LENGTH_SHORT).show();
            }
        };
        countDownTimer.start();
    }

    private long getScheduledTimeMs(CleaningRequest request) {
        if (request == null) return -1;

        String dateStr = request.getDate();
        String timeSlot = request.getTimeSlot();

        if (dateStr == null || timeSlot == null || dateStr.isEmpty() || timeSlot.isEmpty()) {
            return -1;
        }

        String combined = dateStr + " " + timeSlot;
        String[] dateFormats = {
                "dd MMM yyyy hh:mm a",
                "MMM dd, yyyy hh:mm a",
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
            } catch (ParseException ignored) { }
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
        req.setAssignedStaffId(getStr(obj, "assigned_staff_id"));
        req.setAssignedStaffName(getStr(obj, "assigned_staff_name"));
        if (obj.has("estimated_minutes") && !obj.get("estimated_minutes").isJsonNull()) {
            req.setEstimatedMinutes(obj.get("estimated_minutes").getAsInt());
        }
        if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
            try {
                String createdAtStr = obj.get("created_at").getAsString();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date = sdf.parse(createdAtStr);
                if (date != null) req.setCreatedAt(date.getTime());
            } catch (Exception e) {
                try {
                    req.setCreatedAt(obj.get("created_at").getAsLong());
                } catch (Exception ignored) {}
            }
        }
        return req;
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
}
