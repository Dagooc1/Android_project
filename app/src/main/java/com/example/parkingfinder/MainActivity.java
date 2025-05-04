package com.example.parkingfinder;

import java.util.Iterator;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.location.LocationManager;
import org.json.JSONArray;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.Distance;
import org.osmdroid.views.overlay.OverlayWithIW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int SAVED_LOCATIONS_REQUEST_CODE = 1002;

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private CompassOverlay compassOverlay;
    private Button saveLocationButton;
    private Button findCarButton;
    private Button listSavedButton;
    private Button removeParkingButton;
    private TextView compassReadingText;
    private TextView distanceText;
    private TextView weatherTextView;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ParkingDatabase parkingDatabase;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    private Location savedParkingLocation;
    private ParkingLocation currentDbParking;

    private static final String WEATHER_API_KEY = "eab0aa3483fa4f49b0385759252504";
    private static final String WEATHER_API_URL = "https://api.weatherapi.com/v1/current.json?key=%s&q=%f,%f";

    // Routing
    private Polyline routeOverlay;
    private boolean isRoutingActive = false;
    private List<Marker> parkingMarkers = new ArrayList<>();
    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true";

    //private void fetchOpenMeteoWeather(double latitude, double longitude) {
    //    String url = String.format(OPEN_METEO_URL, latitude, longitude);
    //    new FetchWeatherTask().execute(url);
   // }

    // Modify your weather parsing to handle both APIs
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure OpenStreetMap
        Configuration.getInstance().load(getApplicationContext(),
                getPreferences(Context.MODE_PRIVATE));

        setContentView(R.layout.activity_main);

        // Initialize UI components
        mapView = findViewById(R.id.map);
        saveLocationButton = findViewById(R.id.saveLocationButton);
        findCarButton = findViewById(R.id.findCarButton);
        listSavedButton = findViewById(R.id.listSavedButton);
        removeParkingButton = findViewById(R.id.removeParkingButton);
        compassReadingText = findViewById(R.id.compassReading);
        distanceText = findViewById(R.id.distanceText);
        weatherTextView = findViewById(R.id.weatherTextView);

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize sensor services
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Initialize database
        parkingDatabase = new ParkingDatabase(this);

        // Check and request permissions if needed
        checkAndRequestPermissions();

        // Setup map view
        setupMapView();

        // Setup buttons
        setupButtons();

        // Setup location updates
        setupLocationUpdates();

        // Load saved parking locations
        loadSavedParkingLocations();

        // Check if there was a previous parking spot
        loadLastSavedParkingLocation();

        checkLocationEnabled();
    }

    private void loadLastSavedParkingLocation() {
        try {
            ParkingLocation lastLocation = parkingDatabase.getLastParkingLocation();

            if (lastLocation != null) {
                currentDbParking = lastLocation;
                savedParkingLocation = new Location("database");
                savedParkingLocation.setLatitude(lastLocation.getLatitude());
                savedParkingLocation.setLongitude(lastLocation.getLongitude());

                // Show remove button
                runOnUiThread(() -> removeParkingButton.setVisibility(View.VISIBLE));

                // Update distance text
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            updateLocationInfo(location);
                        }
                    });
                }
            } else {
                // No locations in database, ensure UI is reset
                clearCurrentParkingFromUI();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error loading last saved parking location", e);
            Toast.makeText(this, "Error loading saved parking location", Toast.LENGTH_SHORT).show();
            // Reset state if there's an error
            currentDbParking = null;
            savedParkingLocation = null;
            clearCurrentParkingFromUI();
        }
    }

    private void checkAndRequestPermissions() {
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
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void setupMapView() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);

        // Add location overlay
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        mapView.getOverlays().add(locationOverlay);

        // Add compass overlay
        compassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);
    }

    private void setupButtons() {
        saveLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentLocation();
            }
        });

        findCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToCar();
            }
        });

        listSavedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSavedLocations();
            }
        });

        removeParkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmRemoveParking();
            }
        });
    }

    private void setupLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationInfo(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void updateLocationInfo(Location location) {
        // Update distance to saved parking if available
        if (savedParkingLocation != null) {
            float distance = location.distanceTo(savedParkingLocation);
            distanceText.setText(String.format("Distance to vehicle: %.1f meters", distance));
            distanceText.setVisibility(View.VISIBLE);
        } else {
            distanceText.setVisibility(View.GONE);
        }
    }

    private void saveCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // Clear previous parking data first - THIS IS KEY
                    clearCurrentParkingFromUI();

                    // Now set the new location
                    savedParkingLocation = location;

                    // Save to database
                    long timestamp = System.currentTimeMillis();
                    ParkingLocation parkingLocation = new ParkingLocation(
                            0, // ID will be auto-generated
                            location.getLatitude(),
                            location.getLongitude(),
                            timestamp,
                            "Parking spot at " + Utils.formatTimestamp(timestamp)
                    );

                    try {
                        long id = parkingDatabase.insertParkingLocation(parkingLocation);
                        parkingLocation.setId(id);
                        currentDbParking = parkingLocation;

                        // Add marker to map
                        addParkingMarker(parkingLocation);

                        // Get weather for this location
                        fetchWeatherData(location.getLatitude(), location.getLongitude());

                        // Show remove button
                        removeParkingButton.setVisibility(View.VISIBLE);

                        Toast.makeText(MainActivity.this, "Parking spot saved!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error saving parking location", e);
                        Toast.makeText(MainActivity.this, "Error saving parking location", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Couldn't get current location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addParkingMarker(ParkingLocation parkingLocation) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(parkingLocation.getLatitude(), parkingLocation.getLongitude()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(parkingLocation.getName());
        marker.setSnippet("Saved on: " + Utils.formatTimestamp(parkingLocation.getTimestamp()));
        marker.setId(String.valueOf(parkingLocation.getId()));

        // Set marker click listener
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                showParkingOptions(parkingLocation);
                return true;
            }
        });

        mapView.getOverlays().add(marker);
        parkingMarkers.add(marker);
        mapView.invalidate();
    }

    private void showParkingOptions(ParkingLocation parkingLocation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(parkingLocation.getName())
                .setItems(new CharSequence[]{"Navigate to this spot", "Remove this spot"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Navigate
                            selectParkingLocation(parkingLocation);
                            break;
                        case 1: // Remove
                            removeParkingLocation(parkingLocation);
                            break;
                    }
                })
                .show();
    }

    private void selectParkingLocation(ParkingLocation parkingLocation) {
        currentDbParking = parkingLocation;
        savedParkingLocation = new Location("database");
        savedParkingLocation.setLatitude(parkingLocation.getLatitude());
        savedParkingLocation.setLongitude(parkingLocation.getLongitude());

        // Enable remove button
        removeParkingButton.setVisibility(View.VISIBLE);

        // Navigate
        navigateToCar();
    }

    private void removeParkingLocation(ParkingLocation parkingLocation) {
        try {
            if (parkingLocation == null) {
                Toast.makeText(this, "No parking location to remove", Toast.LENGTH_SHORT).show();
                return;
            }

            // Store ID for comparison
            long locationId = parkingLocation.getId();

            // First check if the location exists in the database
            ParkingLocation checkLocation = parkingDatabase.getParkingLocationById(locationId);
            if (checkLocation == null) {
                // The location doesn't exist
                Toast.makeText(this, "Parking location not found", Toast.LENGTH_SHORT).show();

                // If this was our current selected location, clear it from the UI
                if (currentDbParking != null && currentDbParking.getId() == locationId) {
                    clearCurrentParkingFromUI();
                }

                // Refresh the map
                loadSavedParkingLocations();
                return;
            }

            // IMPORTANT: We're NOT deleting from database anymore
            // Instead, just remove from the current view/UI

            // Remove marker - doing it safely
            boolean markerRemoved = false;
            List<Marker> markersToRemove = new ArrayList<>();

            for (Marker marker : parkingMarkers) {
                if (marker.getId() != null && marker.getId().equals(String.valueOf(locationId))) {
                    markersToRemove.add(marker);
                    markerRemoved = true;
                }
            }

            // Remove markers outside the loop to avoid ConcurrentModificationException
            for (Marker marker : markersToRemove) {
                mapView.getOverlays().remove(marker);
                parkingMarkers.remove(marker);
            }

            // Only clear current parking from UI if this was the active one
            if (currentDbParking != null && currentDbParking.getId() == locationId) {
                clearCurrentParkingFromUI();
            }

            mapView.invalidate();
            Toast.makeText(this, "Parking location removed from map", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("MainActivity", "Error removing parking location from map", e);
            Toast.makeText(this, "Error removing parking location from map", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmRemoveParking() {
        // First check if we have a current parking location at all
        if (currentDbParking == null) {
            // No active parking spot, hide the button and notify user
            removeParkingButton.setVisibility(View.GONE);
            Toast.makeText(this, "No active parking spot selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the parking location still exists in the database
        final ParkingLocation checkLocation = parkingDatabase.getParkingLocationById(currentDbParking.getId());

        if (checkLocation == null) {
            // The parking location no longer exists in the database
            Toast.makeText(this, "Parking location no longer exists", Toast.LENGTH_SHORT).show();
            // Ensure we clear the current parking state from UI
            clearCurrentParkingFromUI();
            // Make sure the button is hidden
            removeParkingButton.setVisibility(View.GONE);
            return;
        }

        // If we get here, we have a valid parking location to remove from map
        new AlertDialog.Builder(this)
                .setTitle("Remove From Map")
                .setMessage("Remove this parking location from the map? The data will be retained in your saved locations.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Create a local copy to prevent NullPointerException
                    ParkingLocation locationToRemove = new ParkingLocation(
                            checkLocation.getId(),
                            checkLocation.getLatitude(),
                            checkLocation.getLongitude(),
                            checkLocation.getTimestamp(),
                            checkLocation.getName()
                    );
                    removeParkingLocation(locationToRemove);
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Clears the parking location from the UI only, without affecting database
     */
    private void clearCurrentParkingFromUI() {
        // Clear route if it exists
        clearRoute();

        // We're not nullifying the reference to the database parking location
        // currentDbParking = null;  // Keep this reference for potential future use
        // savedParkingLocation = null; // Keep this reference for potential future use

        // Just hide UI elements
        runOnUiThread(() -> {
            distanceText.setVisibility(View.GONE);
            compassReadingText.setVisibility(View.GONE);
            weatherTextView.setVisibility(View.GONE);
            removeParkingButton.setVisibility(View.GONE);
        });
    }

    private void navigateToCar() {
        if (savedParkingLocation != null) {
            // First check if the parking location still exists in the database
            if (currentDbParking != null) {
                ParkingLocation checkLocation = parkingDatabase.getParkingLocationById(currentDbParking.getId());
                if (checkLocation == null) {
                    // Location no longer exists in database
                    Toast.makeText(this, "Parking location no longer exists", Toast.LENGTH_SHORT).show();
                    clearCurrentParkingFromUI();
                    return;
                }
            }

            GeoPoint parkingPoint = new GeoPoint(
                    savedParkingLocation.getLatitude(),
                    savedParkingLocation.getLongitude());

            mapView.getController().animateTo(parkingPoint);
            distanceText.setVisibility(View.VISIBLE);
            compassReadingText.setVisibility(View.VISIBLE);

            // Start routing
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        calculateRoute(new GeoPoint(location.getLatitude(), location.getLongitude()),
                                parkingPoint);
                    }
                });
            }

            // Get weather for this location
            fetchWeatherData(savedParkingLocation.getLatitude(), savedParkingLocation.getLongitude());

            // Show remove button
            removeParkingButton.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "No parking location selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedParkingLocations() {
        try {
            // Clear existing markers
            for (Marker marker : parkingMarkers) {
                mapView.getOverlays().remove(marker);
            }
            parkingMarkers.clear();

            // Load from database
            List<ParkingLocation> savedLocations = parkingDatabase.getAllParkingLocations();

            for (ParkingLocation location : savedLocations) {
                // Only add marker if this is the current selected location or if we're showing all
                if (currentDbParking == null || currentDbParking.getId() == location.getId()) {
                    addParkingMarker(location);
                }
            }

            mapView.invalidate();

            // If we have no saved locations being shown, ensure UI elements are hidden
            if (parkingMarkers.isEmpty()) {
                clearCurrentParkingFromUI();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error loading saved parking locations", e);
            Toast.makeText(this, "Error loading saved parking locations", Toast.LENGTH_SHORT).show();
            clearCurrentParkingFromUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        // Register sensor listeners
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Refresh markers from database
        loadSavedParkingLocations();
        checkLocationEnabled();
    }

    private void checkLocationEnabled() {
        if (!isLocationEnabled()) {
            // If location is disabled, redirect to OnboardingActivity
            Intent intent = new Intent(MainActivity.this, OnboardingActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();

        // Unregister listeners
        sensorManager.unregisterListener(this);

        // Stop location updates
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SAVED_LOCATIONS_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // Handle selected location from SavedLocationsActivity
                long parkingId = data.getLongExtra("selected_parking_id", -1);
                if (parkingId != -1) {
                    ParkingLocation location = parkingDatabase.getParkingLocationById(parkingId);
                    if (location != null) {
                        selectParkingLocation(location);
                    } else {
                        Toast.makeText(this, "Selected parking location not found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            // Refresh the map to ensure only the selected location is shown
            loadSavedParkingLocations();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // Permissions granted, start location updates
                setupLocationUpdates();
            } else {
                Toast.makeText(this, "Location permissions are required for this app",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
        }

        // Calculate orientation
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            // Convert radians to degrees
            float azimuthDegrees = (float) Math.toDegrees(orientationAngles[0]);
            if (azimuthDegrees < 0) {
                azimuthDegrees += 360;
            }

            // Update compass reading
            updateCompassReading(azimuthDegrees);
        }
    }

    private void updateCompassReading(float azimuth) {
        // Get direction name
        String direction = getDirectionName(azimuth);

        // If we have a saved parking location, calculate bearing
        if (savedParkingLocation != null &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location currentLocation) {
                    if (currentLocation != null) {
                        float bearing = currentLocation.bearingTo(savedParkingLocation);
                        if (bearing < 0) {
                            bearing += 360;
                        }

                        // Calculate relative direction (where to turn)
                        float relativeBearing = bearing - azimuth;
                        if (relativeBearing < 0) {
                            relativeBearing += 360;
                        }

                        String carDirection = getRelativeDirectionName(relativeBearing);
                        compassReadingText.setText(String.format("Compass: %s (%.1f°)\nVehicle is %s",
                                direction, azimuth, carDirection));
                    }
                }
            });
        } else {
            compassReadingText.setText(String.format("Compass: %s (%.1f°)", direction, azimuth));
        }
    }

    private String getDirectionName(float azimuth) {
        if (azimuth >= 337.5 || azimuth < 22.5) {
            return "N";
        } else if (azimuth >= 22.5 && azimuth < 67.5) {
            return "NE";
        } else if (azimuth >= 67.5 && azimuth < 112.5) {
            return "E";
        } else if (azimuth >= 112.5 && azimuth < 157.5) {
            return "SE";
        } else if (azimuth >= 157.5 && azimuth < 202.5) {
            return "S";
        } else if (azimuth >= 202.5 && azimuth < 247.5) {
            return "SW";
        } else if (azimuth >= 247.5 && azimuth < 292.5) {
            return "W";
        } else {
            return "NW";
        }
    }

    private String getRelativeDirectionName(float relativeBearing) {
        if (relativeBearing >= 337.5 || relativeBearing < 22.5) {
            return "straight ahead";
        } else if (relativeBearing >= 22.5 && relativeBearing < 67.5) {
            return "to your front-right";
        } else if (relativeBearing >= 67.5 && relativeBearing < 112.5) {
            return "to your right";
        } else if (relativeBearing >= 112.5 && relativeBearing < 157.5) {
            return "to your back-right";
        } else if (relativeBearing >= 157.5 && relativeBearing < 202.5) {
            return "behind you";
        } else if (relativeBearing >= 202.5 && relativeBearing < 247.5) {
            return "to your back-left";
        } else if (relativeBearing >= 247.5 && relativeBearing < 292.5) {
            return "to your left";
        } else {
            return "to your front-left";
        }
    }

    private void fetchWeatherData(double latitude, double longitude) {
        String url = String.format(WEATHER_API_URL, WEATHER_API_KEY, latitude, longitude);
        new FetchWeatherTask().execute(url);
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                return stringBuilder.toString();
            } catch (IOException e) {
                Log.e("WeatherAPI", "Error fetching weather data", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONObject location = jsonObject.getJSONObject("location");
                    JSONObject current = jsonObject.getJSONObject("current");

                    String city = location.getString("name");
                    double temp = current.getDouble("temp_c");
                    JSONObject condition = current.getJSONObject("condition");
                    String description = condition.getString("text");

                    String weatherText = String.format("%s: %.1f°C, %s", city, temp, description);
                    weatherTextView.setText(weatherText);
                    weatherTextView.setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    Log.e("WeatherAPI", "Error parsing weather data", e);
                    weatherTextView.setText("Weather data unavailable");
                }
            } else {
                weatherTextView.setText("Weather data unavailable");
            }
        }
    }

    private void calculateRoute(GeoPoint start, GeoPoint end) {
        // Clear previous route if exists
        clearRoute();

        // Create a new route overlay
        routeOverlay = new Polyline();
        routeOverlay.setColor(ContextCompat.getColor(this, R.color.purple_500));
        routeOverlay.setWidth(10f);

        // First try OpenRouteService
        new FetchRouteTask().execute(start, end);

        isRoutingActive = true;
    }

    private class FetchRouteTask extends AsyncTask<GeoPoint, Void, List<GeoPoint>> {
        private static final String OPENROUTE_API_KEY = "5b3ce3597851110001cf6248a606f77191d4416d8c7c3fe1279a8bd8";
        private static final String OPENROUTE_API_URL = "https://api.openrouteservice.org/v2/directions/foot-walking";
        private GeoPoint[] mPoints; // Store points as class member

        @Override
        protected List<GeoPoint> doInBackground(GeoPoint... points) {
            mPoints = points; // Store the points
            if (points.length < 2) return null;

            GeoPoint start = points[0];
            GeoPoint end = points[1];

            try {
                // Build the coordinates JSON array for the API
                JSONArray coordinates = new JSONArray();
                JSONArray startCoord = new JSONArray();
                startCoord.put(start.getLongitude());
                startCoord.put(start.getLatitude());

                JSONArray endCoord = new JSONArray();
                endCoord.put(end.getLongitude());
                endCoord.put(end.getLatitude());

                coordinates.put(startCoord);
                coordinates.put(endCoord);

                // Create request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("coordinates", coordinates);
                requestBody.put("format", "geojson");

                // Set up connection
                URL url = new URL(OPENROUTE_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", OPENROUTE_API_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000); // 10 second timeout
                connection.setReadTimeout(15000); // 15 second read timeout

                // Write request body
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                // Parse the GeoJSON response to extract route points
                List<GeoPoint> routePoints = new ArrayList<>();
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray features = jsonResponse.getJSONArray("features");
                if (features.length() > 0) {
                    JSONObject firstFeature = features.getJSONObject(0);
                    JSONObject geometry = firstFeature.getJSONObject("geometry");
                    if (geometry.getString("type").equals("LineString")) {
                        JSONArray coordinates2 = geometry.getJSONArray("coordinates");
                        for (int i = 0; i < coordinates2.length(); i++) {
                            JSONArray point = coordinates2.getJSONArray(i);
                            double lon = point.getDouble(0);
                            double lat = point.getDouble(1);
                            routePoints.add(new GeoPoint(lat, lon));
                        }
                    }
                }

                return routePoints;

            } catch (Exception e) {
                Log.e("FetchRouteTask", "Error fetching route: " + e.getMessage(), e);
                return null;
            }
        }


        @Override
        protected void onPostExecute(List<GeoPoint> routePoints) {
            if (routePoints != null && !routePoints.isEmpty()) {
                // OpenRouteService API call succeeded, draw the route
                routeOverlay.setPoints(routePoints);
                mapView.getOverlays().add(routeOverlay);
                BoundingBox boundingBox = BoundingBox.fromGeoPoints(routePoints);
                mapView.zoomToBoundingBox(boundingBox, true, 50);
                mapView.invalidate();
                Toast.makeText(MainActivity.this, "Route found!", Toast.LENGTH_SHORT).show();
            } else {
                // OpenRouteService API call failed, try GraphHopper as fallback
                new FetchGraphhopperRouteTask().execute(mPoints);
            }
        }
    }

    private void clearRoute() {
        if (routeOverlay != null) {
            mapView.getOverlays().remove(routeOverlay);
            mapView.invalidate();
            routeOverlay = null;
        }
        isRoutingActive = false;
    }

    private void openSavedLocations() {
        try {
            // Check if the SavedLocationsActivity actually exists before launching it
            Intent intent = new Intent(this, SavedLocationsActivity.class);
            if (intent.resolveActivity(getPackageManager()) != null) {
                // Pass only active locations to the activity
                List<ParkingLocation> activeLocations = new ArrayList<>();
                if (currentDbParking != null) {
                    ParkingLocation location = parkingDatabase.getParkingLocationById(currentDbParking.getId());
                    if (location != null) {
                        activeLocations.add(location);
                    }
                }

                // Alternatively, you could pass all locations and let the activity filter them
                intent.putExtra("active_location_id", currentDbParking != null ? currentDbParking.getId() : -1);
                startActivityForResult(intent, SAVED_LOCATIONS_REQUEST_CODE);
            } else {
                // If the activity doesn't exist, show the dialog
                showSavedLocationsDialog();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error opening saved locations", e);
            Toast.makeText(this, "Error opening saved locations", Toast.LENGTH_SHORT).show();
            showSavedLocationsDialog();
        }
    }

    private void showSavedLocationsDialog() {
        try {
            // Get all saved locations from database
            List<ParkingLocation> allLocations = parkingDatabase.getAllParkingLocations();

            if (allLocations.isEmpty()) {
                Toast.makeText(this, "No saved parking locations found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create lists for location names and IDs
            List<CharSequence> locationNames = new ArrayList<>();
            List<Long> locationIds = new ArrayList<>();

            for (ParkingLocation location : allLocations) {
                // Only include locations that are currently marked as active
                if (currentDbParking == null || currentDbParking.getId() == location.getId()) {
                    locationNames.add(location.getName());
                    locationIds.add(location.getId());
                }
            }

            if (locationNames.isEmpty()) {
                Toast.makeText(this, "No active parking locations found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert lists to arrays for the dialog
            final CharSequence[] namesArray = locationNames.toArray(new CharSequence[0]);
            final long[] idsArray = new long[locationIds.size()];
            for (int i = 0; i < locationIds.size(); i++) {
                idsArray[i] = locationIds.get(i);
            }

            // Show dialog with list of locations
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Saved Parking Locations")
                    .setItems(namesArray, (dialog, which) -> {
                        long selectedId = idsArray[which];
                        ParkingLocation selectedLocation = parkingDatabase.getParkingLocationById(selectedId);
                        if (selectedLocation != null) {
                            selectParkingLocation(selectedLocation);
                        } else {
                            Toast.makeText(this, "Error loading selected location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e("MainActivity", "Error showing saved locations dialog", e);
            Toast.makeText(this, "Error loading saved locations", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (parkingDatabase != null) {
            parkingDatabase.close();
        }
    }

    private class FetchGraphhopperRouteTask extends AsyncTask<GeoPoint, Void, List<GeoPoint>> {
        // Use OSRM as an alternative routing service (doesn't require API key)
        private static final String OSRM_API_URL = "https://router.project-osrm.org/route/v1/foot/";
        private GeoPoint[] mPoints;

        @Override
        protected List<GeoPoint> doInBackground(GeoPoint... points) {
            mPoints = points;
            if (points.length < 2) return null;

            GeoPoint start = points[0];
            GeoPoint end = points[1];

            try {
                // Build URL for OSRM
                StringBuilder urlBuilder = new StringBuilder(OSRM_API_URL);
                urlBuilder.append(start.getLongitude()).append(",").append(start.getLatitude()).append(";");
                urlBuilder.append(end.getLongitude()).append(",").append(end.getLatitude());
                urlBuilder.append("?overview=full&geometries=polyline");

                URL url = new URL(urlBuilder.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000); // 10 second timeout
                connection.setReadTimeout(15000); // 15 second read timeout

                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                // Parse JSON response
                List<GeoPoint> routePoints = new ArrayList<>();
                JSONObject jsonResponse = new JSONObject(response.toString());

                // Check if the route was found successfully
                if (jsonResponse.has("routes") && jsonResponse.getJSONArray("routes").length() > 0) {
                    JSONObject route = jsonResponse.getJSONArray("routes").getJSONObject(0);

                    // Get the encoded polyline
                    String geometry = route.getString("geometry");

                    // Decode the polyline
                    List<GeoPoint> decodedPoints = decodePolyline(geometry);
                    return decodedPoints;
                }

                return null;

            } catch (Exception e) {
                Log.e("FetchOSRMRoute", "Error fetching route: " + e.getMessage(), e);
                return null;
            }
        }

        // Method to decode Google's encoded polyline format
        private List<GeoPoint> decodePolyline(String encoded) {
            List<GeoPoint> poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                GeoPoint p = new GeoPoint((double) lat / 1E5, (double) lng / 1E5);
                poly.add(p);
            }
            return poly;
        }

        @Override
        protected void onPostExecute(List<GeoPoint> routePoints) {
            if (routePoints != null && !routePoints.isEmpty()) {
                // OSRM API call succeeded
                routeOverlay.setPoints(routePoints);
                mapView.getOverlays().add(routeOverlay);
                BoundingBox boundingBox = BoundingBox.fromGeoPoints(routePoints);
                mapView.zoomToBoundingBox(boundingBox, true, 50);
                mapView.invalidate();
                Toast.makeText(MainActivity.this, "Route found using backup service!", Toast.LENGTH_SHORT).show();
            } else {
                // All routing services failed, use a local routing approach
                try {
                    // Try using local direct routing with waypoints to avoid buildings
                    generateLocalRouteWithWaypoints(mPoints[0], mPoints[1]);
                } catch (Exception e) {
                    Log.e("LocalRouting", "Error in local routing: " + e.getMessage(), e);
                    // Last resort - show direct line with warning
                    routeOverlay.addPoint(mPoints[0]);
                    routeOverlay.addPoint(mPoints[1]);
                    mapView.getOverlays().add(routeOverlay);
                    mapView.invalidate();
                    Toast.makeText(MainActivity.this,
                            "Couldn't calculate detailed route, showing direct path. Consider enabling data connection.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
        private void generateLocalRouteWithWaypoints(GeoPoint start, GeoPoint end) {
            // Calculate the distance between start and end
            double distance = calculateDistance(start.getLatitude(), start.getLongitude(),
                    end.getLatitude(), end.getLongitude());

            // Only add waypoints if distance is significant
            if (distance > 100) { // More than 100m
                routeOverlay.addPoint(start);

                // Add some waypoints to simulate following streets
                // This is a simplified approach - in a real implementation,
                // you would use actual map data to determine street locations

                // Calculate midpoint with slight offset to simulate following roads
                double midLat = (start.getLatitude() + end.getLatitude()) / 2;
                double midLng = (start.getLongitude() + end.getLongitude()) / 2;

                // Add slight perpendicular offset to simulate road network
                double dx = end.getLongitude() - start.getLongitude();
                double dy = end.getLatitude() - start.getLatitude();
                double norm = Math.sqrt(dx * dx + dy * dy);

                // Perpendicular direction
                double perpDx = -dy / norm;
                double perpDy = dx / norm;

                // Add the waypoint with offset
                GeoPoint waypoint = new GeoPoint(
                        midLat + 0.0002 * perpDy, // Small offset
                        midLng + 0.0002 * perpDx
                );
                routeOverlay.addPoint(waypoint);

                routeOverlay.addPoint(end);
                mapView.getOverlays().add(routeOverlay);

                // Calculate bounds to include all points
                ArrayList<GeoPoint> points = new ArrayList<>();
                points.add(start);
                points.add(waypoint);
                points.add(end);

                BoundingBox boundingBox = BoundingBox.fromGeoPoints(points);
                mapView.zoomToBoundingBox(boundingBox, true, 50);
                mapView.invalidate();

                Toast.makeText(MainActivity.this,
                        "Using approximate route. For better accuracy, enable data connection.",
                        Toast.LENGTH_LONG).show();
            } else {
                // For short distances, direct line is reasonable
                routeOverlay.addPoint(start);
                routeOverlay.addPoint(end);
                mapView.getOverlays().add(routeOverlay);
                mapView.invalidate();
            }
        }
        private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
            final int R = 6371000; // Earth's radius in meters

            double latDistance = Math.toRadians(lat2 - lat1);
            double lonDistance = Math.toRadians(lon2 - lon1);

            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            return R * c; // distance in meters
        }
    }
}
