package com.example.cleanify_application;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

public class FeedbackActivity extends AppCompatActivity {

    private ImageView star1, star2, star3, star4, star5, ivStaffPhoto;
    private TextView tvStaffName;
    private EditText etFeedback;
    private Button btnSubmitFeedback;
    private int selectedRating = 0;
    private String requestId;
    private String assignedStaffId;
    private SupabaseClient supabaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        supabaseClient = SupabaseClient.getInstance(this);
        requestId = getIntent().getStringExtra("requestId");

        initViews();
        loadRequestData();

        setupStarRating();
        btnSubmitFeedback.setOnClickListener(v -> submitFeedback());
    }

    private void initViews() {
        star1 = findViewById(R.id.star1);
        star2 = findViewById(R.id.star2);
        star3 = findViewById(R.id.star3);
        star4 = findViewById(R.id.star4);
        star5 = findViewById(R.id.star5);
        ivStaffPhoto = findViewById(R.id.ivStaffPhoto);
        tvStaffName = findViewById(R.id.tvStaffName);
        etFeedback = findViewById(R.id.etFeedback);
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);
    }

    private void loadRequestData() {
        if (requestId == null) return;

        supabaseClient.select(FeedbackActivity.this, "cleaning_requests", "request_id=eq." + requestId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvStaffName.setText("Staff Member"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    JsonArray jsonArray = JsonParser.parseString(jsonStr).getAsJsonArray();
                    if (jsonArray.size() > 0) {
                        JsonObject doc = jsonArray.get(0).getAsJsonObject();
                        String staffName = doc.has("assigned_staff_name") && !doc.get("assigned_staff_name").isJsonNull() ? doc.get("assigned_staff_name").getAsString() : "Staff Member";
                        assignedStaffId = doc.has("assigned_staff_id") && !doc.get("assigned_staff_id").isJsonNull() ? doc.get("assigned_staff_id").getAsString() : null;
                        runOnUiThread(() -> tvStaffName.setText(staffName));
                    }
                }
            }
        });
    }

    private void setupStarRating() {
        ImageView[] stars = {star1, star2, star3, star4, star5};

        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedRating = rating;
                updateStarDisplay(stars, rating);
            });
        }
    }

    private void updateStarDisplay(ImageView[] stars, int rating) {
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.ic_star_filled);
            } else {
                stars[i].setImageResource(R.drawable.ic_star_empty);
            }
        }
    }

    private void submitFeedback() {
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String feedback = etFeedback.getText().toString().trim();

        if (requestId == null) return;

        btnSubmitFeedback.setEnabled(false);
        btnSubmitFeedback.setText("Submitting...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("rating", selectedRating);
        updates.put("feedback", feedback);

        supabaseClient.update(FeedbackActivity.this, "cleaning_requests", "request_id=eq." + requestId, updates, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(FeedbackActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmitFeedback.setEnabled(true);
                    btnSubmitFeedback.setText(R.string.submit_feedback);
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful() && assignedStaffId != null) {
                    updateWorkerRating();
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(FeedbackActivity.this, "Feedback submitted! Thank you!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
        });
    }

    private void updateWorkerRating() {
        supabaseClient.select(FeedbackActivity.this, "users", "id=eq." + assignedStaffId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                finishActivityWithSuccess();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonStr = response.body().string();
                        JsonArray jsonArray = JsonParser.parseString(jsonStr).getAsJsonArray();
                        if (jsonArray.size() > 0) {
                            JsonObject user = jsonArray.get(0).getAsJsonObject();
                            float currentRating = user.has("rating") && !user.get("rating").isJsonNull() ? user.get("rating").getAsFloat() : 5.0f;
                            int totalFeedbacks = user.has("total_feedbacks") && !user.get("total_feedbacks").isJsonNull() ? user.get("total_feedbacks").getAsInt() : 0;
                            
                            float newRating = ((currentRating * totalFeedbacks) + selectedRating) / (totalFeedbacks + 1);
                            
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("rating", newRating);
                            updates.put("total_feedbacks", totalFeedbacks + 1);
                            
                            supabaseClient.update(FeedbackActivity.this, "users", "id=eq." + assignedStaffId, updates, new Callback() {
                                @Override
                                public void onFailure(Call c, IOException e) { finishActivityWithSuccess(); }
                                @Override
                                public void onResponse(Call c, Response r) { finishActivityWithSuccess(); }
                            });
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                finishActivityWithSuccess();
            }
        });
    }

    private void finishActivityWithSuccess() {
        runOnUiThread(() -> {
            Toast.makeText(FeedbackActivity.this, "Feedback submitted! Thank you!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
