package com.example.parkingfinder;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hide action bar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Delay and then decide which activity to start
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!OnboardingManager.isOnboardingComplete(this)) {
                // Onboarding not completed - show onboarding
                Intent intent = new Intent(SplashActivity.this, FeatureOnboardingActivity.class);
                startActivity(intent);
            } else {
                // Onboarding already completed - go to main activity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            }
            finish();
        }, SPLASH_DELAY);
    }
}