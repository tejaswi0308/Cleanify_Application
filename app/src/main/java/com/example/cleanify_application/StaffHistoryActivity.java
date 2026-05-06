package com.example.cleanify_application;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cleanify_application.adapters.CompletedTaskAdapter;
import com.example.cleanify_application.models.CleaningRequest;
import com.example.cleanify_application.network.SupabaseClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class StaffHistoryActivity extends AppCompatActivity {

    private static final String TAG = "StaffHistory";
    private TextView tvPerformanceCount;
    private ProgressBar progressBar;
    private RecyclerView rvCompletedTasks;
    private ImageView btnBack;
    private SupabaseClient supabaseClient;
    private List<CleaningRequest> completedTasks;
    private CompletedTaskAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_history);

        try {
            supabaseClient = SupabaseClient.getInstance(this);
        } catch (Exception e) { /* not configured */ }

        initViews();
        loadCompletedTasks();
    }

    private void initViews() {
        tvPerformanceCount = findViewById(R.id.tvPerformanceCount);
        progressBar = findViewById(R.id.progressBar);
        rvCompletedTasks = findViewById(R.id.rvCompletedTasks);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        completedTasks = new ArrayList<>();
        adapter = new CompletedTaskAdapter(completedTasks, task -> {
            android.content.Intent intent = new android.content.Intent(this, FeedbackActivity.class);
            intent.putExtra("requestId", task.getRequestId());
            startActivity(intent);
        });
        rvCompletedTasks.setLayoutManager(new LinearLayoutManager(this));
        rvCompletedTasks.setAdapter(adapter);
    }

    private void loadCompletedTasks() {
        if (supabaseClient == null || !supabaseClient.isAuthenticated()) {
            tvPerformanceCount.setText("0");
            progressBar.setVisibility(android.view.View.GONE);
            return;
        }

        String uid = supabaseClient.getUserId();
        // Load all tasks for this staff to calculate performance
        supabaseClient.select(this, "cleaning_requests",
                "assigned_staff_id=eq." + uid + "&order=completed_at.desc",
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Error: " + e.getMessage());
                            tvPerformanceCount.setText("0");
                            progressBar.setVisibility(android.view.View.GONE);
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "[]";
                        runOnUiThread(() -> {
                            try {
                                com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(body).getAsJsonArray();
                                completedTasks.clear();
                                
                                int totalToday = 0;
                                int completedToday = 0;
                                
                                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                                String today = sdf.format(new java.util.Date());

                                for (int i = 0; i < arr.size(); i++) {
                                    com.google.gson.JsonObject obj = arr.get(i).getAsJsonObject();
                                    CleaningRequest task = new CleaningRequest();
                                    task.setRequestId(getStr(obj, "request_id"));
                                    task.setRoomNumber(getStr(obj, "room_number"));
                                    task.setDate(getStr(obj, "date"));
                                    task.setTimeSlot(getStr(obj, "time_slot"));
                                    task.setStatus(getStr(obj, "status"));
                                    
                                    if (today.equals(task.getDate())) {
                                        totalToday++;
                                        if ("completed".equals(task.getStatus())) {
                                            completedToday++;
                                        }
                                    }

                                    if ("completed".equals(task.getStatus())) {
                                        completedTasks.add(task);
                                    }
                                }

                                tvPerformanceCount.setText(String.valueOf(completedToday));
                                if (totalToday > 0) {
                                    progressBar.setVisibility(android.view.View.VISIBLE);
                                    progressBar.setMax(totalToday);
                                    progressBar.setProgress(completedToday);
                                } else {
                                    progressBar.setVisibility(android.view.View.GONE);
                                }

                                adapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                Log.e(TAG, "Parse error: " + e.getMessage());
                            }
                        });
                    }
                });
    }

    private String getStr(com.google.gson.JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
}
