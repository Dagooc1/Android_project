package com.example.parkingfinder;

public class ParkingLocationManager {
    private static ParkingLocation selectedLocation;
    private static ParkingLocation locationToDelete;

    public static void setSelectedLocation(ParkingLocation location) {
        selectedLocation = location;
    }

    public static ParkingLocation getSelectedLocation() {
        return selectedLocation;
    }

    public static void clearSelectedLocation() {
        selectedLocation = null;
    }

    public static void setLocationToDelete(ParkingLocation location) {
        locationToDelete = location;
    }

    public static ParkingLocation getLocationToDelete() {
        return locationToDelete;
    }

    public static void clearLocationToDelete() {
        locationToDelete = null;
    }
}