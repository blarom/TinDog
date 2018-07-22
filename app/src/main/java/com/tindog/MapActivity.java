package com.tindog;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.Foundation;

import java.util.ArrayList;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private double[] mUserCoordinates;
    private ArrayList<Dog> mDogsArrayList;
    private ArrayList<Family> mFamiliesArrayList;
    private ArrayList<Foundation> mFoundationsArrayList;

    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        getExtras();
        mapFragment.getMapAsync(this);
    }


    //Maps methods
    @Override public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker for the user coordinates and centering the map on them
        LatLng userCoords = new LatLng(mUserCoordinates[0], mUserCoordinates[1]);
        mMap.addMarker(new MarkerOptions()
                .position(userCoords)
                .title("Your location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(userCoords));

        LatLng coords;
        double latitude;
        double longitude;
        if (mDogsArrayList!=null) {
            for (Dog dog : mDogsArrayList) {
                latitude = Double.parseDouble(dog.getGaLt());
                longitude = Double.parseDouble(dog.getGaLg());
                coords = new LatLng(latitude, longitude);
                mMap.addMarker(new MarkerOptions().position(coords).title(dog.getNm()));
            }
        }
        if (mFamiliesArrayList!=null) {
            for (Family family : mFamiliesArrayList) {
                latitude = Double.parseDouble(family.getGaLt());
                longitude = Double.parseDouble(family.getGaLg());
                coords = new LatLng(latitude, longitude);
                mMap.addMarker(new MarkerOptions().position(coords).title(family.getPn()));
            }
        }
        if (mFoundationsArrayList!=null) {
            for (Foundation foundation : mFoundationsArrayList) {
                latitude = Double.parseDouble(foundation.getGaLt());
                longitude = Double.parseDouble(foundation.getGaLg());
                coords = new LatLng(latitude, longitude);
                mMap.addMarker(new MarkerOptions().position(coords).title(foundation.getNm()));
            }
        }
    }


    //Functional methods
    private void getExtras() {

        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.search_results_map_user_coordinates))) {
            mUserCoordinates = intent.getDoubleArrayExtra(getString(R.string.search_results_map_user_coordinates));
        }
        if (intent.hasExtra(getString(R.string.search_results_dogs_list))) {
            ArrayList<Parcelable> parcelableArrayList = intent.getParcelableArrayListExtra(getString(R.string.search_results_dogs_list));
            mDogsArrayList = new ArrayList<>();
            for (Parcelable parcelable : parcelableArrayList) {
                mDogsArrayList.add((Dog) parcelable);
            }
        }
        if (intent.hasExtra(getString(R.string.search_results_families_list))) {
            mFamiliesArrayList = intent.getParcelableArrayListExtra(getString(R.string.search_results_families_list));
        }
        if (intent.hasExtra(getString(R.string.search_results_foundations_list))) {
            mFoundationsArrayList = intent.getParcelableArrayListExtra(getString(R.string.search_results_foundations_list));
        }
    }
}
