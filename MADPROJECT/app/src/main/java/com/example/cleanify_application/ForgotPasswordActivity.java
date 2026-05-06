package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmailPhone;
    private Button btnSubmit;
    private ImageView btnBack;
    private TextView tvBackToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        etEmailPhone = findViewById(R.id.etEmailPhone);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnBack = findViewById(R.id.btnBack);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnBack.setOnClickListener(v -> finish());
        tvBackToLogin.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String email = etEmailPhone.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                etEmailPhone.setError("Enter your email");
                return;
            }

            btnSubmit.setEnabled(false);
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
                            // Navigate to OTP verification (simulated)
                            Intent intent = new Intent(this, OTPVerificationActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        btnSubmit.setEnabled(true);
                    });
        });
    }
}
