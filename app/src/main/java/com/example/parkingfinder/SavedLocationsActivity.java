package com.example.parkingfinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class SavedLocationsActivity extends AppCompatActivity {

    private ListView savedLocationsListView;
    private TextView emptyTextView;
    private ParkingDatabase parkingDatabase;
    private List<ParkingLocation> parkingLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_locations);

        // Set up toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Saved Parking Locations");
        }

        // Initialize UI components
        savedLocationsListView = findViewById(R.id.savedLocationsListView);
        emptyTextView = findViewById(R.id.emptyTextView);

        // Initialize database
        parkingDatabase = new ParkingDatabase(this);

        // Load and display saved locations
        loadSavedLocations();

        // Set item click listener
        savedLocationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ParkingLocation selectedLocation = parkingLocations.get(position);
                returnSelectedLocation(selectedLocation);
            }
        });

        // Set item long click listener for deletion
        savedLocationsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ParkingLocation selectedLocation = parkingLocations.get(position);
                showDeleteConfirmationDialog(selectedLocation);
                return true;
            }
        });
    }

    private void loadSavedLocations() {
        try {
            // Get all saved locations from database
            parkingLocations = parkingDatabase.getAllParkingLocations();

            if (parkingLocations.isEmpty()) {
                savedLocationsListView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                savedLocationsListView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.GONE);

                // Create list of location names for display
                List<String> locationNames = new ArrayList<>();
                for (ParkingLocation location : parkingLocations) {
                    locationNames.add(location.getName());
                }

                // Set up adapter
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        locationNames);
                savedLocationsListView.setAdapter(adapter);
            }
        } catch (Exception e) {
            Log.e("SavedLocationsActivity", "Error loading saved locations", e);
            Toast.makeText(this, "Error loading saved locations", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog(final ParkingLocation location) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Location")
                .setMessage("Are you sure you want to delete this saved location?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteLocation(location);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteLocation(ParkingLocation location) {
        try {
            // Fix: No need to capture a return value since the method returns void
            parkingDatabase.deleteParkingLocation(location.getId());

            // Assume deletion was successful and update the UI
            Toast.makeText(this, "Location deleted", Toast.LENGTH_SHORT).show();
            loadSavedLocations(); // Refresh the list
        } catch (Exception e) {
            Log.e("SavedLocationsActivity", "Error deleting location", e);
            Toast.makeText(this, "Error deleting location", Toast.LENGTH_SHORT).show();
        }
    }

    private void returnSelectedLocation(ParkingLocation location) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_parking_id", location.getId());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}