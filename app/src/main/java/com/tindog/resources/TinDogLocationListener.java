package com.tindog.resources;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class TinDogLocationListener implements LocationListener {

    private static final String DEBUG_TAG = "TinDog Location";
    private final Context mContext;
    private double mUserLongitude;
    private double mUserLatitude;
    private String mLocalCityName;


    public TinDogLocationListener(Context context, LocationListenerHandler locationListenerHandler) {
        this.mContext = context;
        this.locationListenerHandler = locationListenerHandler;
    }


    //Location Listener methods
    @Override public void onLocationChanged(Location location) {

        //inspired by: https://stackoverflow.com/questions/1513485/how-do-i-get-the-current-gps-location-programmatically-in-android

        mUserLongitude = location.getLongitude();
        mUserLatitude = location.getLatitude();
        locationListenerHandler.onLocalCoordinatesFound(mUserLongitude, mUserLatitude);

    }
    @Override public void onStatusChanged(String s, int i, Bundle bundle) {

    }
    @Override public void onProviderEnabled(String s) {

    }
    @Override public void onProviderDisabled(String s) {

    }


    //Functional methods


    //Communication with other activities/fragments:
    final private LocationListenerHandler locationListenerHandler;
    public interface LocationListenerHandler {
        void onLocalCoordinatesFound(double longitude, double latitude);
    }
}
