package com.example.cleanify_application;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cleanify_application.adapters.CompletedTaskAdapter;
import com.example.cleanify_application.models.CleaningRequest;
import com.example.cleanify_application.network.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;

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

        supabaseClient = SupabaseClient.getInstance(this);

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
        adapter = new CompletedTaskAdapter(completedTasks);
        rvCompletedTasks.setLayoutManager(new LinearLayoutManager(this));
        rvCompletedTasks.setAdapter(adapter);
    }

    // Removed loadDemoData

    private void loadCompletedTasks() {
        String staffId = supabaseClient.getUserId();
        if (staffId == null) return;
        supabaseClient.select(StaffHistoryActivity.this, "cleaning_requests", "assigned_staff_id=eq." + staffId + "&status=eq.completed&order=created_at.desc", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error: " + e.getMessage());
                    tvPerformanceCount.setText("0/10");
                    progressBar.setProgress(0);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    JsonArray jsonArray = JsonParser.parseString(jsonStr).getAsJsonArray();

                    runOnUiThread(() -> {
                        completedTasks.clear();
                        int count = 0;

                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject obj = jsonArray.get(i).getAsJsonObject();
                            CleaningRequest request = new CleaningRequest();
                            request.setRoomNumber(obj.has("room_number") && !obj.get("room_number").isJsonNull() ? obj.get("room_number").getAsString() : null);
                            request.setDate(obj.has("date") && !obj.get("date").isJsonNull() ? obj.get("date").getAsString() : null);
                            request.setTimeSlot(obj.has("time_slot") && !obj.get("time_slot").isJsonNull() ? obj.get("time_slot").getAsString() : null);
                            request.setStatus("completed");
                            completedTasks.add(request);
                            count++;
                        }

                        int total = 10; // Target could be dynamic
                        tvPerformanceCount.setText(count + "/" + total);
                        progressBar.setMax(total);
                        progressBar.setProgress(Math.min(count, total));

                        adapter.notifyDataSetChanged();
                    });
                } else {
                    runOnUiThread(() -> {
                        tvPerformanceCount.setText("0/10");
                        progressBar.setProgress(0);
                    });
                }
            }
        });
    }
}
