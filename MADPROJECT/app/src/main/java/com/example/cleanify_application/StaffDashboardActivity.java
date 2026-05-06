package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
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
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StaffDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StaffDashboard";
    private TextView tvActiveRoom, tvActiveType, tvActiveTime, tvActiveEst, tvSeeAll;
    private Button btnMarkComplete;
    private ImageView btnProfile;
    private RecyclerView rvUpcomingTasks;
    private SupabaseClient supabaseClient;
    private List<CleaningRequest> upcomingTasks;
    private UpcomingTaskAdapter adapter;
    private CleaningRequest activeRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        supabaseClient = SupabaseClient.getInstance(this);

        initViews();

        if (!supabaseClient.isAuthenticated()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        loadTasks();

        btnMarkComplete.setOnClickListener(v -> markAsComplete());

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });

        tvSeeAll.setOnClickListener(v -> {
            startActivity(new Intent(this, StaffHistoryActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void initViews() {
        try {
            tvActiveRoom = findViewById(R.id.tvActiveRoom);
            tvActiveType = findViewById(R.id.tvActiveType);
            tvActiveTime = findViewById(R.id.tvActiveTime);
            tvActiveEst = findViewById(R.id.tvActiveEst);
            tvSeeAll = findViewById(R.id.tvSeeAll);
            btnMarkComplete = findViewById(R.id.btnMarkComplete);
            btnProfile = findViewById(R.id.btnProfile);
            rvUpcomingTasks = findViewById(R.id.rvUpcomingTasks);

            if (tvActiveRoom == null || tvActiveType == null || tvActiveTime == null || 
                tvActiveEst == null || tvSeeAll == null || btnMarkComplete == null || 
                btnProfile == null || rvUpcomingTasks == null) {
                Log.e(TAG, "One or more views not found in layout");
                Toast.makeText(this, "Error loading dashboard layout", Toast.LENGTH_LONG).show();
                return;
            }

            upcomingTasks = new ArrayList<>();
            adapter = new UpcomingTaskAdapter(upcomingTasks, task -> {
                // Navigate to task details if needed
            });
            rvUpcomingTasks.setLayoutManager(new LinearLayoutManager(this));
            rvUpcomingTasks.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Error initializing dashboard", Toast.LENGTH_LONG).show();
        }
    }

    // Removed loadDemoData

    private void loadTasks() {
        try {
            // Fetch JWT payload or rely on a stored staff ID
            // For simplicity here, we assume a hardcoded staff ID or we get it from a JWT decoder
            String staffId = supabaseClient.getUserId();
            if (staffId == null) {
                if (tvActiveRoom != null) {
                    tvActiveRoom.setText("Error: User not authenticated properly.");
                }
                Log.e(TAG, "Staff ID is null");
                return;
            }
            
            Log.d(TAG, "Loading tasks for staff ID: " + staffId);
        
        // Fetch tasks assigned to this staff member
            supabaseClient.select(StaffDashboardActivity.this, "cleaning_requests", "assigned_staff_id=eq." + staffId + "&status=in.(assigned,in_progress,pending)&limit=10", new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Error loading tasks", e);
                        if (tvActiveRoom != null) {
                            tvActiveRoom.setText("Error loading tasks");
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String jsonStr = response.body().string();
                            Log.d(TAG, "Tasks response: " + jsonStr);
                            JsonArray jsonArray = JsonParser.parseString(jsonStr).getAsJsonArray();
                            
                            runOnUiThread(() -> {
                                if (upcomingTasks != null) {
                                    upcomingTasks.clear();
                                }
                                activeRequest = null;

                                for (int i = 0; i < jsonArray.size(); i++) {
                                    JsonObject obj = jsonArray.get(i).getAsJsonObject();
                                    CleaningRequest request = new CleaningRequest();
                                    request.setRequestId(obj.has("request_id") && !obj.get("request_id").isJsonNull() ? obj.get("request_id").getAsString() : null);
                                    request.setRoomNumber(obj.has("room_number") && !obj.get("room_number").isJsonNull() ? obj.get("room_number").getAsString() : null);
                                    request.setHostel(obj.has("hostel") && !obj.get("hostel").isJsonNull() ? obj.get("hostel").getAsString() : null);
                                    request.setCleaningType(obj.has("cleaning_type") && !obj.get("cleaning_type").isJsonNull() ? obj.get("cleaning_type").getAsString() : null);
                                    request.setTimeSlot(obj.has("time_slot") && !obj.get("time_slot").isJsonNull() ? obj.get("time_slot").getAsString() : null);
                                    request.setEstimatedMinutes(obj.has("estimated_minutes") && !obj.get("estimated_minutes").isJsonNull() ? obj.get("estimated_minutes").getAsInt() : 25);
                                    String status = obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : null;
                                    request.setStatus(status);

                                    if (("assigned".equals(status) || "in_progress".equals(status)) && activeRequest == null) {
                                        activeRequest = request;
                                        updateActiveTaskCard(request);
                                    } else if ("assigned".equals(status) || "pending".equals(status)) {
                                        if (upcomingTasks != null) {
                                            upcomingTasks.add(request);
                                        }
                                    }
                                }

                                if (tvActiveRoom != null && tvActiveType != null && tvActiveTime != null && tvActiveEst != null) {
                                    if (activeRequest == null) {
                                        tvActiveRoom.setText("No active task");
                                        tvActiveType.setText("");
                                        tvActiveTime.setText("");
                                        tvActiveEst.setText("");
                                    }
                                }
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        } else {
                            Log.e(TAG, "Unsuccessful response: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                        runOnUiThread(() -> {
                            if (tvActiveRoom != null) {
                                tvActiveRoom.setText("Error parsing data");
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadTasks", e);
            if (tvActiveRoom != null) {
                tvActiveRoom.setText("Error loading tasks");
            }
        }
    }

    private void updateActiveTaskCard(CleaningRequest request) {
        try {
            String room = request.getRoomNumber() != null ? request.getRoomNumber() : "";
            String hostel = request.getHostel() != null ? request.getHostel() : "";
            
            if (tvActiveRoom != null) {
                tvActiveRoom.setText("Room " + room + ", Hostel " + hostel);
            }
            if (tvActiveType != null) {
                tvActiveType.setText(request.getCleaningType() != null ? request.getCleaningType() : "Cleaning");
            }
            if (tvActiveTime != null) {
                tvActiveTime.setText(request.getTimeSlot() != null ? request.getTimeSlot() : "");
            }
            if (tvActiveEst != null) {
                tvActiveEst.setText(request.getEstimatedMinutes() + " min est.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating active task card", e);
        }
    }

    private void markAsComplete() {
        if (activeRequest == null) {
            Toast.makeText(this, "No active task", Toast.LENGTH_SHORT).show();
            return;
        }

        // Demo mode removed

        if (activeRequest.getRequestId() == null) {
            Toast.makeText(this, "No active task", Toast.LENGTH_SHORT).show();
            return;
        }

        String qrCode = UUID.randomUUID().toString();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "awaiting_verification");
        updates.put("qr_code", qrCode);

        supabaseClient.update(StaffDashboardActivity.this, "cleaning_requests", "request_id=eq." + activeRequest.getRequestId(), updates, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StaffDashboardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Update the worker's status back to free
                    String staffId = supabaseClient.getUserId();
                    if (staffId != null) {
                        Map<String, Object> statusUpdate = new HashMap<>();
                        statusUpdate.put("status", "free");
                        supabaseClient.update(StaffDashboardActivity.this, "users", "id=eq." + staffId, statusUpdate, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {}
                            @Override
                            public void onResponse(Call call, Response response) throws IOException {}
                        });
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(StaffDashboardActivity.this, "QR Code Generated!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(StaffDashboardActivity.this, QRCodeActivity.class);
                        intent.putExtra("requestId", activeRequest.getRequestId());
                        intent.putExtra("qrCode", qrCode);
                        startActivity(intent);
                        activeRequest = null;
                        loadTasks();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(StaffDashboardActivity.this, "Failed to update task", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
