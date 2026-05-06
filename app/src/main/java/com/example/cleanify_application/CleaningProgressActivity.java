package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.network.SupabaseClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CleaningProgressActivity extends AppCompatActivity {

    private TextView tvTimer, tvStartTime, tvRequestInfo, tvStaffName, tvStaffStatus;
    private Button tvScanQr;
    private Button btnCancelRequest;
    private ImageView btnBack;
    private SupabaseClient supabaseClient;
    private String requestId;
    private CountDownTimer countDownTimer;

    // QR Scanner Launcher
    private final androidx.activity.result.ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> barcodeLauncher = registerForActivityResult(new com.journeyapps.barcodescanner.ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_SHORT).show();
                    String reqId = requestId;
                    
                    if (requestId != null && supabaseClient != null) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "completed");
                        updates.put("completed_at", com.example.cleanify_application.utils.DateTimeUtils.getCurrentIsoTimestamp());
                        
                        supabaseClient.update(this, "cleaning_requests", "request_id=eq." + requestId, updates, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                runOnUiThread(() -> navigateToFeedback(reqId));
                            }
                            @Override
                            public void onResponse(Call call, Response response) {
                                try (Response res = response) {
                                    if (res.isSuccessful()) {
                                        runOnUiThread(() -> navigateToFeedback(reqId));
                                    } else {
                                        runOnUiThread(() -> navigateToFeedback(reqId));
                                    }
                                } catch (Exception e) {
                                    runOnUiThread(() -> navigateToFeedback(reqId));
                                }
                            }
                        });
                    } else {
                        navigateToFeedback(reqId);
                    }
                }
            });

    private void navigateToFeedback(String reqId) {
        Intent intent = new Intent(CleaningProgressActivity.this, FeedbackActivity.class);
        intent.putExtra("requestId", reqId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cleaning_progress);

        try {
            supabaseClient = SupabaseClient.getInstance(this);
        } catch (Exception e) { /* not configured */ }
        requestId = getIntent().getStringExtra("requestId");

        initViews();
        loadRequestData();
    }

    private void initViews() {
        tvTimer = findViewById(R.id.tvTimer);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvRequestInfo = findViewById(R.id.tvRequestInfo);
        tvStaffName = findViewById(R.id.tvStaffName);
        tvStaffStatus = findViewById(R.id.tvStaffStatus);
        tvScanQr = findViewById(R.id.tvScanQr);
        btnCancelRequest = findViewById(R.id.btnCancelRequest);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnCancelRequest.setOnClickListener(v -> {
            cancelRequest();
        });

        tvScanQr.setOnClickListener(v -> {
            com.journeyapps.barcodescanner.ScanOptions options = new com.journeyapps.barcodescanner.ScanOptions();
            options.setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE);
            options.setPrompt(getString(R.string.scan_prompt));
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(true);
            barcodeLauncher.launch(options);
        });
    }

    private void handleTimers(com.example.cleanify_application.models.CleaningRequest req, int durationMinutes) {
        long scheduledTimeMs = getScheduledTimeMs(req);
        long now = System.currentTimeMillis();
        long cleaningDurationMs = durationMinutes * 60 * 1000L;

        if (scheduledTimeMs > 0 && now < scheduledTimeMs) {
            // Wait for start time
            tvStartTime.setText(getString(R.string.scheduled_for_format, req.getTimeSlot()));
            tvScanQr.setVisibility(android.view.View.GONE);
            btnCancelRequest.setVisibility(android.view.View.VISIBLE);
            tvStaffStatus.setText(getString(R.string.status_awaiting_start));
            
            if (countDownTimer != null) countDownTimer.cancel();
            countDownTimer = new CountDownTimer(scheduledTimeMs - now, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long totalSeconds = millisUntilFinished / 1000;
                    long hours = totalSeconds / 3600;
                    long min = (totalSeconds % 3600) / 60;
                    long sec = totalSeconds % 60;
                    
                    if (hours > 0) {
                        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, min, sec));
                    } else {
                        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
                    }
                }

                @Override
                public void onFinish() {
                    // Start cleaning time
                    tvStartTime.setText(getString(R.string.started_at_format, req.getTimeSlot()));
                    tvStaffStatus.setText(getString(R.string.status_in_progress));
                    req.setStatus("in_progress");
                    tvScanQr.setVisibility(android.view.View.VISIBLE);
                    startTimer(durationMinutes);
                }
            };
            countDownTimer.start();
        } else {
            tvStartTime.setText(getString(R.string.started_at_format, (req.getTimeSlot() != null ? req.getTimeSlot() : "now")));
            if (!"completed".equals(req.getStatus())) {
                req.setStatus("in_progress");
                tvStaffStatus.setText(getString(R.string.status_in_progress));
            }
            tvScanQr.setVisibility(android.view.View.VISIBLE);
            
            long remaining = cleaningDurationMs;
            if (scheduledTimeMs > 0) {
                long elapsed = now - scheduledTimeMs;
                remaining = cleaningDurationMs - elapsed;
            }
            if (remaining > 0) {
                startTimerMs(remaining);
            } else {
                tvTimer.setText("00:00");
                btnCancelRequest.setVisibility(android.view.View.GONE);
            }
        }
    }

    private void loadRequestData() {
        if (requestId == null || supabaseClient == null) return;

        supabaseClient.select(this, "cleaning_requests", "request_id=eq." + requestId, new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                runOnUiThread(() -> tvStaffName.setText("Unable to load"));
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                try (okhttp3.Response res = response) {
                    String body = res.body() != null ? res.body().string() : "[]";
                    runOnUiThread(() -> {
                        try {
                            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(body).getAsJsonArray();
                            if (arr.size() > 0) {
                                com.google.gson.JsonObject obj = arr.get(0).getAsJsonObject();
                                String staffName = getStr(obj, "assigned_staff_name");
                                String status = getStr(obj, "status");
                                String floor = getStr(obj, "floor_number");
                                int estimatedMin = obj.has("estimated_minutes") && !obj.get("estimated_minutes").isJsonNull()
                                        ? obj.get("estimated_minutes").getAsInt() : 25;

                                com.example.cleanify_application.models.CleaningRequest parsedReq = parseCleaningRequest(obj);
                                String shortId = requestId.length() > 5 ? requestId.substring(0, 5) : requestId;
                                tvRequestInfo.setText(getString(R.string.request_info_format, shortId, "Floor " + (floor.isEmpty() ? "N/A" : floor)));
                                tvStaffName.setText(staffName.isEmpty() ? getString(R.string.awaiting_staff) : staffName);
                                tvStaffStatus.setText(getString(R.string.status_prefix, (status.isEmpty() ? "Pending" : status)));

                                handleTimers(parsedReq, estimatedMin);
                            }
                        } catch (Exception e) {
                            tvStaffName.setText("Unable to load");
                        }
                    });
                }
            }
        });
    }

    private void startTimer(int minutes) {
        startTimerMs(minutes * 60 * 1000L);
    }
    
    private void startTimerMs(long totalMillis) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(totalMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int min = (int) (millisUntilFinished / 1000 / 60);
                int sec = (int) (millisUntilFinished / 1000 % 60);
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
                
                if (tvScanQr.getVisibility() == android.view.View.VISIBLE) {
                    long elapsedMillis = totalMillis - millisUntilFinished;
                    if (elapsedMillis <= 5 * 60 * 1000L) {
                        btnCancelRequest.setVisibility(android.view.View.VISIBLE);
                    } else {
                        btnCancelRequest.setVisibility(android.view.View.GONE);
                    }
                }
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                Toast.makeText(CleaningProgressActivity.this, getString(R.string.cleaning_time_completed), Toast.LENGTH_SHORT).show();
            }
        };
        countDownTimer.start();
    }


    private void cancelRequest() {
        if (requestId == null || supabaseClient == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");

        supabaseClient.update(this, "cleaning_requests", "request_id=eq." + requestId, updates, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(CleaningProgressActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response res = response) {
                    runOnUiThread(() -> {
                        Toast.makeText(CleaningProgressActivity.this, getString(R.string.request_cancelled), Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
        });
    }

    private com.example.cleanify_application.models.CleaningRequest parseCleaningRequest(com.google.gson.JsonObject obj) {
        com.example.cleanify_application.models.CleaningRequest req = new com.example.cleanify_application.models.CleaningRequest();
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
        return req;
    }

    private String getStr(com.google.gson.JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private long getScheduledTimeMs(com.example.cleanify_application.models.CleaningRequest request) {
        if (request == null) return -1;
        String dateStr = request.getDate();
        String timeSlot = request.getTimeSlot();
        if (dateStr == null || timeSlot == null || dateStr.isEmpty() || timeSlot.isEmpty()) return -1;

        String combined = dateStr + " " + timeSlot;
        String[] dateFormats = {
                "dd MMM yyyy hh:mm a",
                "MMM dd, yyyy hh:mm a",
                "dd-MM-yyyy hh:mm a",
                "yyyy-MM-dd hh:mm a"
        };
        for (String format : dateFormats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format, Locale.getDefault());
                java.util.Date parsed = sdf.parse(combined);
                if (parsed != null) return parsed.getTime();
            } catch (java.text.ParseException ignored) {}
        }
        return -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
