package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cleanify_application.adapters.RequestHistoryAdapter;
import com.example.cleanify_application.models.CleaningRequest;
import com.example.cleanify_application.network.SupabaseClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class StudentHistoryActivity extends AppCompatActivity {

    private static final String TAG = "StudentHistory";
    private RecyclerView rvHistory;
    private ImageView btnBack;
    private SupabaseClient supabaseClient;
    private List<CleaningRequest> requestHistory;
    private RequestHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_history);

        try {
            supabaseClient = SupabaseClient.getInstance(this);
        } catch (Exception e) { /* not configured */ }

        initViews();
        loadRequestHistory();
    }

    private void initViews() {
        rvHistory = findViewById(R.id.rvHistory);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

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

    private void loadRequestHistory() {
        String uid = com.example.cleanify_application.utils.LocalDataManager.getInstance(this).getLoggedInUserId();
        if (uid == null) return;

        supabaseClient.select(this, "cleaning_requests", "student_id=eq." + uid + "&select=*", new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Failed to load history: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (!response.isSuccessful()) return;
                String body = response.body().string();
                
                runOnUiThread(() -> {
                    try {
                        com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(body).getAsJsonArray();
                        requestHistory.clear();
                        for (int i = 0; i < arr.size(); i++) {
                            com.google.gson.JsonObject obj = arr.get(i).getAsJsonObject();
                            CleaningRequest req = parseRequest(obj);
                            if ("completed".equals(req.getStatus()) || "cancelled".equals(req.getStatus())) {
                                requestHistory.add(req);
                            }
                        }
                        Collections.sort(requestHistory, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e(TAG, "Parse error: " + e.getMessage());
                    }
                });
            }
        });
    }

    private CleaningRequest parseRequest(com.google.gson.JsonObject obj) {
        CleaningRequest req = new CleaningRequest();
        req.setRequestId(getStr(obj, "request_id"));
        req.setStudentId(getStr(obj, "student_id"));
        req.setAssignedStaffId(getStr(obj, "assigned_staff_id"));
        req.setAssignedStaffName(getStr(obj, "assigned_staff_name"));
        req.setRoomNumber(getStr(obj, "room_number"));
        req.setHostel(getStr(obj, "hostel"));
        req.setDate(getStr(obj, "date"));
        req.setTimeSlot(getStr(obj, "time_slot"));
        req.setCleaningType(getStr(obj, "cleaning_type"));
        req.setStatus(getStr(obj, "status"));
        req.setRating(obj.has("rating") && !obj.get("rating").isJsonNull() ? obj.get("rating").getAsInt() : 0);
        req.setFeedback(getStr(obj, "feedback"));
        req.setCreatedAt(obj.has("created_at") && !obj.get("created_at").isJsonNull() ? 
                com.example.cleanify_application.utils.DateTimeUtils.parseIsoToMillis(obj.get("created_at").getAsString()) : 0);
        return req;
    }

    private String getStr(com.google.gson.JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
}
