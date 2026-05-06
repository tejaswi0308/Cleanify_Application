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
