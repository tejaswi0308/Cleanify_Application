package com.example.cleanify_application;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.network.SupabaseClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class FeedbackActivity extends AppCompatActivity {

    private ImageView star1, star2, star3, star4, star5;
    private ImageView ivStaffPhoto;
    private TextView tvStaffName;
    private EditText etFeedback;
    private Button btnSubmitFeedback;
    private SupabaseClient supabaseClient;
    private String requestId;
    private int selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        try {
            supabaseClient = SupabaseClient.getInstance(this);
        } catch (Exception e) { /* not configured */ }
        requestId = getIntent().getStringExtra("requestId");

        initViews();

        // Setup interactivity FIRST — data loading may lock it down right after
        setupStarRating();
        btnSubmitFeedback.setOnClickListener(v -> submitFeedback());

        loadRequestData();

        // Role check
        String uid = com.example.cleanify_application.utils.LocalDataManager.getInstance(this).getLoggedInUserId();
        com.example.cleanify_application.models.User user = com.example.cleanify_application.utils.LocalDataManager.getInstance(this).getUserById(uid);
        if (user != null && "staff".equals(user.getRole())) {
            disableEditing();
        }
    }

    private void disableEditing() {
        btnSubmitFeedback.setVisibility(android.view.View.GONE);
        etFeedback.setEnabled(false);
        etFeedback.setFocusable(false);
        
        ImageView[] stars = {star1, star2, star3, star4, star5};
        for (ImageView star : stars) {
            star.setOnClickListener(null);
            star.setClickable(false);
        }
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
        if (requestId == null || supabaseClient == null) {
            tvStaffName.setText(getString(R.string.staff_member));
            return;
        }

        supabaseClient.select(this, "cleaning_requests", "request_id=eq." + requestId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvStaffName.setText(getString(R.string.staff_member)));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response res = response) {
                    String body = res.body() != null ? res.body().string() : "[]";
                    runOnUiThread(() -> {
                        try {
                            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(body).getAsJsonArray();
                            if (arr.size() > 0) {
                                com.google.gson.JsonObject obj = arr.get(0).getAsJsonObject();
                                String staffName = obj.has("assigned_staff_name") && !obj.get("assigned_staff_name").isJsonNull()
                                        ? obj.get("assigned_staff_name").getAsString() : getString(R.string.staff_member);
                                tvStaffName.setText(staffName);

                                if (obj.has("rating") && !obj.get("rating").isJsonNull()) {
                                    int rating = obj.get("rating").getAsInt();
                                    String feedback = obj.has("feedback") && !obj.get("feedback").isJsonNull() ? obj.get("feedback").getAsString() : "";
                                    if (rating > 0) {
                                        populateExistingFeedback(rating, feedback);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            tvStaffName.setText(getString(R.string.staff_member));
                        }
                    });
                }
            }
        });
    }

    private void populateExistingFeedback(int rating, String feedback) {
        selectedRating = rating;
        ImageView[] stars = {star1, star2, star3, star4, star5};
        updateStarDisplay(stars, rating);
        
        for (ImageView star : stars) {
            star.setOnClickListener(null);
            star.setClickable(false);
        }
        
        etFeedback.setText(feedback != null ? feedback : "");
        etFeedback.setEnabled(false);
        etFeedback.setFocusable(false);
        
        btnSubmitFeedback.setVisibility(android.view.View.GONE);
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
            Toast.makeText(this, getString(R.string.please_select_rating), Toast.LENGTH_SHORT).show();
            return;
        }

        String feedback = etFeedback.getText().toString().trim();

        if (requestId == null || supabaseClient == null) return;

        btnSubmitFeedback.setEnabled(false);
        btnSubmitFeedback.setText(getString(R.string.submitting));

        Map<String, Object> updates = new HashMap<>();
        updates.put("rating", selectedRating);
        updates.put("feedback", feedback);
        updates.put("status", "completed");
        updates.put("completed_at", com.example.cleanify_application.utils.DateTimeUtils.getCurrentIsoTimestamp());

        supabaseClient.update(this, "cleaning_requests", "request_id=eq." + requestId, updates, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(FeedbackActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmitFeedback.setEnabled(true);
                    btnSubmitFeedback.setText(getString(R.string.submit_feedback));
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response res = response) {
                    if (res.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(FeedbackActivity.this, getString(R.string.feedback_submitted), Toast.LENGTH_SHORT).show();
                            navigateToDashboard();
                        });
                    } else {
                        String errorBody = res.body() != null ? res.body().string() : "Unknown error";
                        runOnUiThread(() -> {
                            Toast.makeText(FeedbackActivity.this, "Submission Failed: " + res.code(), Toast.LENGTH_SHORT).show();
                            android.util.Log.e("FeedbackActivity", "Error: " + errorBody);
                            btnSubmitFeedback.setEnabled(true);
                            btnSubmitFeedback.setText(getString(R.string.submit_feedback));
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        btnSubmitFeedback.setEnabled(true);
                        btnSubmitFeedback.setText(getString(R.string.submit_feedback));
                    });
                }
            }
        });
    }

    private void navigateToDashboard() {
        android.content.Intent intent = new android.content.Intent(this, StudentDashboardActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
