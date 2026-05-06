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
import com.example.cleanify_application.utils.NullSafety;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StudentDashboard";
    private TextView tvUserName, tvRoomInfo, tvLastCleaned, tvTotalRequests, tvActiveStatus;
    private Button btnNewRequest, btnCancelActive;
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

        supabaseClient = SupabaseClient.getInstance(this);

        initViews();

        if (!supabaseClient.isAuthenticated()) {
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
            startActivity(new Intent(this, EditProfileActivity.class));
        });

        btnCancelActive.setOnClickListener(v -> cancelActiveRequest());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        loadRequestHistory();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
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

        requestHistory = new ArrayList<>();
        adapter = new RequestHistoryAdapter(requestHistory, request -> {
            if ("in_progress".equals(request.getStatus()) || "assigned".equals(request.getStatus()) || "awaiting_verification".equals(request.getStatus())) {
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
        tvActiveStatus.setText("Cleaning In Progress");

        // Start countdown timer
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(minutes * 60 * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int min = (int) (millisUntilFinished / 1000 / 60);
                int sec = (int) (millisUntilFinished / 1000 % 60);
                tvActiveTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                tvActiveTimer.setText("00:00");
                Toast.makeText(StudentDashboardActivity.this, "Cleaning time completed!", Toast.LENGTH_SHORT).show();
            }
        };
        countDownTimer.start();
    }

    private void hideActiveCleaningCard() {
        activeCleaningCard.setVisibility(View.GONE);
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void cancelActiveRequest() {
        if (activeRequest != null && NullSafety.orEmpty(activeRequest.getRequestId()) != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "cancelled");
            supabaseClient.update(StudentDashboardActivity.this, "cleaning_requests", "request_id=eq." + activeRequest.getRequestId(), updates, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(StudentDashboardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> {
                        hideActiveCleaningCard();
                        tvActiveStatus.setText(getString(R.string.no_active_request));
                        Toast.makeText(StudentDashboardActivity.this, "Request cancelled", Toast.LENGTH_SHORT).show();
                        loadRequestHistory();
                    });
                }
            });
        }
    }

    // Removed loadDemoData

    private void loadUserData() {
        String userId = supabaseClient.getUserId();
        if (userId == null) return;
        supabaseClient.select(StudentDashboardActivity.this, "users", "id=eq." + userId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvUserName.setText("Student"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    JsonArray jsonArray = JsonParser.parseString(jsonStr).getAsJsonArray();
                    if (jsonArray.size() > 0) {
                        JsonObject obj = jsonArray.get(0).getAsJsonObject();
                        String name = obj.has("full_name") && !obj.get("full_name").isJsonNull() ? obj.get("full_name").getAsString() : "Student";
                        String room = obj.has("room_number") && !obj.get("room_number").isJsonNull() ? obj.get("room_number").getAsString() : "";
                        String hostel = obj.has("hostel_id") && !obj.get("hostel_id").isJsonNull() ? obj.get("hostel_id").getAsString() : "";
                        runOnUiThread(() -> {
                            tvUserName.setText(name);
                            tvRoomInfo.setText("Room no. " + room + ", " + hostel);
                        });
                    }
                }
            }
        });
    }

    private void loadRequestHistory() {
        String userId = supabaseClient.getUserId();
        if (userId == null) return;
        supabaseClient.select(StudentDashboardActivity.this, "cleaning_requests", "student_id=eq." + userId + "&limit=20&order=created_at.desc", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    tvTotalRequests.setText("0");
                    tvLastCleaned.setText("N/A");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    JsonArray jsonArray = JsonParser.parseString(jsonStr).getAsJsonArray();

                    runOnUiThread(() -> {
                        requestHistory.clear();
                        int totalCount = 0;
                        boolean hasActive = false;
                        String lastCleanedText = "N/A";

                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject obj = jsonArray.get(i).getAsJsonObject();
                            CleaningRequest request = new CleaningRequest();
                            request.setRequestId(obj.has("request_id") && !obj.get("request_id").isJsonNull() ? obj.get("request_id").getAsString() : null);
                            request.setRoomNumber(obj.has("room_number") && !obj.get("room_number").isJsonNull() ? obj.get("room_number").getAsString() : null);
                            request.setHostel(obj.has("hostel") && !obj.get("hostel").isJsonNull() ? obj.get("hostel").getAsString() : null);
                            request.setCleaningType(obj.has("cleaning_type") && !obj.get("cleaning_type").isJsonNull() ? obj.get("cleaning_type").getAsString() : null);
                            request.setTimeSlot(obj.has("time_slot") && !obj.get("time_slot").isJsonNull() ? obj.get("time_slot").getAsString() : null);
                            request.setEstimatedMinutes(obj.has("estimated_minutes") && !obj.get("estimated_minutes").isJsonNull() ? obj.get("estimated_minutes").getAsInt() : 25);
                            request.setDate(obj.has("date") && !obj.get("date").isJsonNull() ? obj.get("date").getAsString() : null);
                            String status = obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : null;
                            request.setStatus(status);
                            request.setAssignedStaffName(obj.has("assigned_staff_name") && !obj.get("assigned_staff_name").isJsonNull() ? obj.get("assigned_staff_name").getAsString() : null);

                            totalCount++;

                            if (("in_progress".equals(status) || "assigned".equals(status)) && !hasActive) {
                                hasActive = true;
                                activeRequest = request;
                                String staffName = request.getAssignedStaffName() != null ? request.getAssignedStaffName() : "Staff";
                                String type = request.getCleaningType() != null ? request.getCleaningType() : "Cleaning";
                                showActiveCleaningCard(staffName, type, request.getEstimatedMinutes() > 0 ? request.getEstimatedMinutes() : 25);
                            } else if ("awaiting_verification".equals(status) && !hasActive) {
                                hasActive = true;
                                activeRequest = request;
                                activeCleaningCard.setVisibility(View.VISIBLE);
                                tvActiveStaffName.setText(request.getAssignedStaffName() != null ? request.getAssignedStaffName() : "Staff");
                                tvActiveCleaningType.setText("Cleaning Done!");
                                tvActiveTimer.setText("Tap to Scan QR");
                                tvActiveStatus.setText("Ready for Verification");
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
                    });
                }
            }
        });
    }
}
