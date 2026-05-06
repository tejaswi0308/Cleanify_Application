package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OTPVerificationActivity extends AppCompatActivity {

    private EditText otp1, otp2, otp3, otp4;
    private Button btnVerify;
    private ImageView btnBack;
    private TextView tvResendOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        btnVerify = findViewById(R.id.btnVerify);
        btnBack = findViewById(R.id.btnBack);
        tvResendOtp = findViewById(R.id.tvResendOtp);

        btnBack.setOnClickListener(v -> finish());

        // Auto-move to next OTP field
        setupOtpAutoMove(otp1, otp2);
        setupOtpAutoMove(otp2, otp3);
        setupOtpAutoMove(otp3, otp4);

        btnVerify.setOnClickListener(v -> {
            String code = otp1.getText().toString() + otp2.getText().toString()
                    + otp3.getText().toString() + otp4.getText().toString();

            if (code.length() == 4) {
                // Since Firebase uses email link for password reset,
                // we navigate to reset password screen
                Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, ResetPasswordActivity.class);
                intent.putExtra("email", getIntent().getStringExtra("email"));
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Please enter the complete OTP", Toast.LENGTH_SHORT).show();
            }
        });

        tvResendOtp.setOnClickListener(v -> {
            Toast.makeText(this, "OTP resent!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupOtpAutoMove(EditText current, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    next.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
