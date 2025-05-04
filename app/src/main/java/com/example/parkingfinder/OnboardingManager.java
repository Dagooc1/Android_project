package com.example.parkingfinder;

import android.content.Context;
import android.content.SharedPreferences;

public class OnboardingManager {
    private static final String PREF_NAME = "onboarding_prefs";
    private static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";

    public static boolean isOnboardingComplete(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false);
    }

    public static void setOnboardingComplete(Context context, boolean complete) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply();
    }
}