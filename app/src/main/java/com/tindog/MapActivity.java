package com.tindog;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.Foundation;
import com.tindog.resources.TinDogLocationListener;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnInfoWindowClickListener,
        TinDogLocationListener.LocationListenerHandler{

    private static final int MAPS_ZOOM_IN_PADDING_IN_PIXELS = 50;
    private GoogleMap mMap;
    private double[] mUserCoordinates;
    private ArrayList<Dog> mDogsArrayList;
    private ArrayList<Family> mFamiliesArrayList;
    private ArrayList<Foundation> mFoundationsArrayList;
    private List<Marker> mMarkers;

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
        mMarkers = new ArrayList<>();
        Marker currentMarker;
        MarkerOptions currentMarkerOptions;


        // Add a marker for the user coordinates and center the map on them
        if (mUserCoordinates!=null) {
            LatLng userCoords = new LatLng(mUserCoordinates[0], mUserCoordinates[1]);
            currentMarkerOptions = new MarkerOptions()
                    .position(userCoords)
                    .title("My location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            currentMarker = mMap.addMarker(currentMarkerOptions);
            mMarkers.add(currentMarker);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(userCoords));
        }


        // Add the object markers
        LatLng coords;
        double latitude;
        double longitude;
        if (mDogsArrayList!=null) {
            for (Dog dog : mDogsArrayList) {
                latitude = Double.parseDouble(dog.getGaLt());
                longitude = Double.parseDouble(dog.getGaLg());
                coords = new LatLng(latitude, longitude);
                currentMarkerOptions = new MarkerOptions().position(coords).title(dog.getNm());
                currentMarker = mMap.addMarker(currentMarkerOptions);
                currentMarker.setTag(dog.getUI());
                mMarkers.add(currentMarker);
            }
        }
        if (mFamiliesArrayList!=null) {
            for (Family family : mFamiliesArrayList) {
                latitude = Double.parseDouble(family.getGaLt());
                longitude = Double.parseDouble(family.getGaLg());
                coords = new LatLng(latitude, longitude);
                currentMarkerOptions = new MarkerOptions().position(coords).title(family.getPn());
                currentMarker = mMap.addMarker(currentMarkerOptions);
                currentMarker.setTag(family.getUI());
                mMarkers.add(currentMarker);
            }
        }
        if (mFoundationsArrayList!=null) {
            for (Foundation foundation : mFoundationsArrayList) {
                latitude = Double.parseDouble(foundation.getGaLt());
                longitude = Double.parseDouble(foundation.getGaLg());
                coords = new LatLng(latitude, longitude);
                currentMarkerOptions = new MarkerOptions().position(coords).title(foundation.getNm());
                currentMarker = mMap.addMarker(currentMarkerOptions);
                currentMarker.setTag(foundation.getUI());
                mMarkers.add(currentMarker);
            }
        }

        mMap.setOnMapLoadedCallback(this);
        mMap.setOnInfoWindowClickListener(this);
    }
    @Override public void onInfoWindowClick(Marker marker) {
        if (marker == null) return;

        String id = (String) marker.getTag();
        if (id != null) {
            if (mDogsArrayList!=null) {
                Intent intent = new Intent(MapActivity.this, SearchResultsActivity.class);
                intent.putExtra(getString(R.string.profile_type), getString(R.string.dog_profile));
                intent.putExtra(getString(R.string.requested_specific_dog_profile), id);
                startActivity(intent);
            }
            else if (mFamiliesArrayList!=null) {
                Intent intent = new Intent(MapActivity.this, SearchResultsActivity.class);
                intent.putExtra(getString(R.string.profile_type), getString(R.string.family_profile));
                intent.putExtra(getString(R.string.requested_specific_family_profile), id);
                startActivity(intent);
            }
            else if (mFoundationsArrayList!=null) {
                Intent intent = new Intent(MapActivity.this, SearchResultsActivity.class);
                intent.putExtra(getString(R.string.profile_type), getString(R.string.foundation_profile));
                intent.putExtra(getString(R.string.requested_specific_foundation_profile), id);
                startActivity(intent);
            }
        }

    }
    @Override public void onMapLoaded() {

        //Setting the map bounds (inspired by: https://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers)
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : mMarkers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, MAPS_ZOOM_IN_PADDING_IN_PIXELS);
        mMap.animateCamera(cu);

        Toast.makeText(this, R.string.click_on_pin_to_show_profile, Toast.LENGTH_SHORT).show();
    }


    //Functional methods
    private void getExtras() {

        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.search_results_map_user_coordinates))) {
            mUserCoordinates = intent.getDoubleArrayExtra(getString(R.string.search_results_map_user_coordinates));
        }
        if (intent.hasExtra(getString(R.string.search_results_dogs_list))) {
            mDogsArrayList = intent.getParcelableArrayListExtra(getString(R.string.search_results_dogs_list));
        }
        if (intent.hasExtra(getString(R.string.search_results_families_list))) {
            mFamiliesArrayList = intent.getParcelableArrayListExtra(getString(R.string.search_results_families_list));
        }
        if (intent.hasExtra(getString(R.string.search_results_foundations_list))) {
            mFoundationsArrayList = intent.getParcelableArrayListExtra(getString(R.string.search_results_foundations_list));
        }
    }


    //Location methods
    @Override public void onLocalCoordinatesFound(double longitude, double latitude) {
        //TODO: update the user's coordinates if they wern't available yet
        //TODO: create marker creation methods
    }
}
