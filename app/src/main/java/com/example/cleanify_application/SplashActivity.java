package com.example.cleanify_application;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cleanify_application.network.SupabaseClient;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkSession();
        }, SPLASH_DELAY);
    }

    private void checkSession() {
        // Force login every time as requested by user
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
