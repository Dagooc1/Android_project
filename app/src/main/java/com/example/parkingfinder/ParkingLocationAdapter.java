package com.example.parkingfinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParkingLocationAdapter extends ArrayAdapter<ParkingLocation> {

    private Context context;
    private List<ParkingLocation> parkingLocations;

    public ParkingLocationAdapter(Context context, List<ParkingLocation> parkingLocations) {
        super(context, R.layout.parking_location_item, parkingLocations);
        this.context = context;
        this.parkingLocations = parkingLocations;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;

        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.parking_location_item, parent, false);
        }

        ParkingLocation currentLocation = parkingLocations.get(position);

        TextView noteTextView = listItem.findViewById(R.id.locationNote);
        TextView coordinatesTextView = listItem.findViewById(R.id.locationCoordinates);
        TextView timestampTextView = listItem.findViewById(R.id.locationTimestamp);

        noteTextView.setText(currentLocation.getName());
        coordinatesTextView.setText(String.format(Locale.US, "%.6f, %.6f",
                currentLocation.getLatitude(), currentLocation.getLongitude()));

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(currentLocation.getTimestamp()));
        timestampTextView.setText(formattedDate);

        return listItem;
    }
}