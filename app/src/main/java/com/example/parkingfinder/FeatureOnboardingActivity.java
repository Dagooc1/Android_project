package com.example.parkingfinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class FeatureOnboardingActivity extends AppCompatActivity implements OnboardingAdapter.OnboardingListener {

    private ViewPager2 viewPager;
    private OnboardingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_onboarding);

        // Hide action bar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize ViewPager
        viewPager = findViewById(R.id.onboardingViewPager);
        adapter = new OnboardingAdapter(this, this);
        viewPager.setAdapter(adapter);

        // Disable swiping if needed
        // viewPager.setUserInputEnabled(false);

        // Add page change callback if needed
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // You can add animation or other effects here
            }
        });
    }

    @Override
    public void onNextPage(int position) {
        // Scroll to the next page
        viewPager.setCurrentItem(position, true);
    }

    @Override
    public void onFinishOnboarding() {
        // Mark onboarding as complete
        OnboardingManager.setOnboardingComplete(this, true);

        // Proceed to the permissions onboarding screen
        Intent intent = new Intent(FeatureOnboardingActivity.this, OnboardingActivity.class);
        startActivity(intent);
        finish();
    }
}