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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import android.net.Uri;

public class CleaningProgressActivity extends AppCompatActivity {

    private TextView tvTimer, tvStartTime, tvRequestInfo, tvStaffName, tvStaffStatus, tvScanQr;
    private Button btnCancelRequest;
    private ImageView btnBack;
    private SupabaseClient supabaseClient;
    private String requestId;
    private CountDownTimer countDownTimer;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    verifyQRCode(result.getContents());
                } else {
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cleaning_progress);

        supabaseClient = SupabaseClient.getInstance(this);
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

        btnCancelRequest.setOnClickListener(v -> cancelRequest());

        tvScanQr.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan the staff's QR Code");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(false);
            barcodeLauncher.launch(options);
        });
    }

    // Removed loadDemoData

    private void loadRequestData() {
        if (requestId == null) return;

        supabaseClient.select(CleaningProgressActivity.this, "cleaning_requests", "request_id=eq." + requestId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvStaffName.setText("Unable to load"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    JsonArray jsonArray = JsonParser.parseString(jsonStr).getAsJsonArray();
                    if (jsonArray.size() > 0) {
                        JsonObject doc = jsonArray.get(0).getAsJsonObject();
                        
                        String staffName = doc.has("assigned_staff_name") && !doc.get("assigned_staff_name").isJsonNull() ? doc.get("assigned_staff_name").getAsString() : "Pending assignment";
                        String status = doc.has("status") && !doc.get("status").isJsonNull() ? doc.get("status").getAsString() : "Pending";
                        String floor = doc.has("floor_number") && !doc.get("floor_number").isJsonNull() ? doc.get("floor_number").getAsString() : "N/A";
                        int minutes = doc.has("estimated_minutes") && !doc.get("estimated_minutes").isJsonNull() ? doc.get("estimated_minutes").getAsInt() : 25;

                        runOnUiThread(() -> {
                            tvRequestInfo.setText("Request #" + requestId.substring(0, Math.min(5, requestId.length())) +
                                    " • Floor " + floor);
                            tvStaffName.setText(staffName);
                            
                            if ("awaiting_verification".equals(status)) {
                                tvStaffStatus.setText("✅ Cleaning Complete - Scan QR to Verify");
                                tvTimer.setText("SCAN QR");
                                tvScanQr.setText("📷 Tap here to scan staff's QR code");
                                btnCancelRequest.setVisibility(android.view.View.GONE);
                            } else {
                                tvStaffStatus.setText("Status: " + status);
                                startTimer(minutes);
                            }
                        });
                    }
                }
            }
        });
    }

    private void verifyQRCode(String content) {
        try {
            Uri uri = Uri.parse(content);
            String requestQuery = uri.getQueryParameter("request");
            String codeQuery = uri.getQueryParameter("code");

            if (requestQuery != null && requestQuery.equals(requestId) && codeQuery != null) {
                // Verify against DB
                supabaseClient.select(CleaningProgressActivity.this, "cleaning_requests", "request_id=eq." + requestId + "&qr_code=eq." + codeQuery, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Toast.makeText(CleaningProgressActivity.this, "Network error", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String jsonStr = response.body().string();
                            JsonArray jsonArray = JsonParser.parseString(jsonStr).getAsJsonArray();
                            if (jsonArray.size() > 0) {
                                // Update status to completed on student side too
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("status", "completed");
                                supabaseClient.update(CleaningProgressActivity.this, "cleaning_requests", "request_id=eq." + requestId, updates, new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {}

                                    @Override
                                    public void onResponse(Call call, Response res) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(CleaningProgressActivity.this, "Cleaning verified! Please rate the service.", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(CleaningProgressActivity.this, FeedbackActivity.class);
                                            intent.putExtra("requestId", requestId);
                                            startActivity(intent);
                                            finish();
                                        });
                                    }
                                });
                            } else {
                                runOnUiThread(() -> Toast.makeText(CleaningProgressActivity.this, "Invalid QR code for this task", Toast.LENGTH_SHORT).show());
                            }
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Invalid QR format", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error parsing QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer(int minutes) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(minutes * 60 * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int min = (int) (millisUntilFinished / 1000 / 60);
                int sec = (int) (millisUntilFinished / 1000 % 60);
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                Toast.makeText(CleaningProgressActivity.this, "Cleaning time completed!", Toast.LENGTH_SHORT).show();
            }
        };
        countDownTimer.start();
    }

    private void cancelRequest() {
        if (requestId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");

        supabaseClient.update(CleaningProgressActivity.this, "cleaning_requests", "request_id=eq." + requestId, updates, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(CleaningProgressActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(CleaningProgressActivity.this, "Request cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
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
}
