package com.example.cleanify_application;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class QRCodeActivity extends AppCompatActivity {

    private ImageView ivQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        ivQrCode = findViewById(R.id.ivQrCode);

        String requestId = getIntent().getStringExtra("requestId");
        String qrCode = getIntent().getStringExtra("qrCode");

        String qrContent = "cleanify://verify?request=" + requestId + "&code=" + qrCode;

        try {
            Bitmap bitmap = generateQRCode(qrContent, 600, 600);
            ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }

        startStatusPolling(requestId);
    }

    private android.os.Handler pollingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pollingRunnable;

    private void startStatusPolling(String requestId) {
        if (requestId == null) return;
        
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                checkRequestStatus(requestId);
                pollingHandler.postDelayed(this, 3000); // Poll every 3 seconds
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    private void checkRequestStatus(String requestId) {
        com.example.cleanify_application.network.SupabaseClient client = com.example.cleanify_application.network.SupabaseClient.getInstance(this);
        if (client == null) return;
        
        client.select(this, "cleaning_requests", "request_id=eq." + requestId, new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {}

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                try (okhttp3.Response res = response) {
                    if (res.isSuccessful()) {
                        String body = res.body().string();
                        com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(body).getAsJsonArray();
                        if (arr.size() > 0) {
                            String status = arr.get(0).getAsJsonObject().get("status").getAsString();
                            if ("completed".equals(status)) {
                                runOnUiThread(() -> {
                                    Toast.makeText(QRCodeActivity.this, "Verification Successful!", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    private Bitmap generateQRCode(String content, int width, int height) throws WriterException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, width, height);

        int matrixWidth = bitMatrix.getWidth();
        int matrixHeight = bitMatrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < matrixWidth; x++) {
            for (int y = 0; y < matrixHeight; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.primary) : 0xFFFFFFFF);
            }
        }
        return bitmap;
    }
}
