package com.example.parkingfinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int APP_SETTINGS_REQUEST_CODE = 101;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 102;

    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        continueButton = findViewById(R.id.continueButton);
        continueButton.setOnClickListener(v -> checkAndRequestPermissions());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check location settings every time the activity resumes
        checkLocationEnabled();
    }

    private void checkAndRequestPermissions() {
        // First check if location is enabled
        if (!isLocationEnabled()) {
            promptEnableLocation();
            return;
        }

        // Then check and request permissions
        requestPermissions();
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void checkLocationEnabled() {
        if (!isLocationEnabled()) {
            promptEnableLocation();
        }
    }

    private void promptEnableLocation() {
        new AlertDialog.Builder(this)
                .setTitle("Location Services Required")
                .setMessage("Please enable location services to use Parking Finder")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open location settings
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) ->
                        Toast.makeText(this, "App requires location services to function properly",
                                Toast.LENGTH_LONG).show())
                .setCancelable(false)
                .create()
                .show();
    }

    private boolean arePermissionsGranted() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSIONS_REQUEST_CODE);
        } else {
            // All permissions already granted
            startMainActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            boolean shouldShowRationale = false;

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;

                    // Check if we should show rationale for this permission
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        shouldShowRationale = true;
                    }
                }
            }

            if (allGranted) {
                // All permissions granted
                startMainActivity();
            } else if (shouldShowRationale) {
                // Some permissions denied, but we can ask again
                showPermissionExplanationDialog();
            } else {
                // User checked "Don't ask again", direct them to settings
                showSettingsDialog();
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app needs location and storage permissions to help you find your parked car.")
                .setPositiveButton("Try Again", (dialog, which) -> requestPermissions())
                .setNegativeButton("Cancel", (dialog, which) -> Toast.makeText(this,
                        "App cannot function without required permissions", Toast.LENGTH_LONG).show())
                .setCancelable(false)
                .create()
                .show();
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app needs location and storage permissions. Please enable them in app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, APP_SETTINGS_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> Toast.makeText(this,
                        "App cannot function without required permissions", Toast.LENGTH_LONG).show())
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == APP_SETTINGS_REQUEST_CODE) {
            // Check if permissions were granted in settings
            if (arePermissionsGranted()) {
                if (!isLocationEnabled()) {
                    promptEnableLocation();
                } else {
                    startMainActivity();
                }
            } else {
                Toast.makeText(this,
                        "App cannot function without required permissions",
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            // Check if location was enabled
            if (isLocationEnabled()) {
                requestPermissions();
            } else {
                Toast.makeText(this,
                        "Location services must be enabled to use this app",
                        Toast.LENGTH_LONG).show();
                // Keep showing the prompt until location is enabled
                promptEnableLocation();
            }
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}