package com.tindog.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tindog.R;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.MapMarker;
import com.tindog.data.TinDogUser;
import com.tindog.resources.TinDogLocationListener;
import com.tindog.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class MapActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnInfoWindowClickListener,
        TinDogLocationListener.LocationListenerHandler,
        FirebaseDao.FirebaseOperationsHandler,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnCameraIdleListener {


    //region Parameters
    private static final String DOG_TAG = "dog----";
    private static final String FAMILY_TAG = "family-";
    private static final String FOUNDATION_TAG = "found--";
    private static final String PLACE_TAG = "place--";
    private static final int MAP_ZOOM_IN_PADDING_IN_PIXELS = 200;
    private GoogleMap mMap;
    private double[] mUserCoordinates;
    private ArrayList<Dog> mDogsArrayList;
    private ArrayList<Family> mFamiliesArrayList;
    private ArrayList<Foundation> mFoundationsArrayList;
    private List<Marker> mMarkersForBoundsCalculation;
    private Unbinder mBinding;
    private LocationManager mLocationManager;
    private TinDogLocationListener mLocationListener;
    private List<MapMarker> mMapMarkerList;
    private DatabaseReference mFirebaseDbReference;
    private FirebaseDao mFirebaseDao;
    private FirebaseUser mCurrentFirebaseUser;
    private FirebaseAuth mFirebaseAuth;
    private String mFirebaseUid;
    private Menu mMenu;
    private boolean mAlreadyShowingMapMarkersFromFirebase;
    private LatLngBounds mBounds;
    private Bundle mSavedInstanceState;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mSavedInstanceState = savedInstanceState;
        getExtras();
        initializeParameters();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
    @Override protected void onResume() {
        super.onResume();
        if (!mAlreadyShowingMapMarkersFromFirebase) getMapMarkersFromFirebase();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        removeListeners();
        mBinding.unbind();
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        mMenu = menu;
        if (mCurrentFirebaseUser==null) Utilities.updateSignInMenuItem(mMenu, this, false);
        else Utilities.updateSignInMenuItem(mMenu, this, true);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_signin:
                if (mCurrentFirebaseUser==null) {
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                    Utilities.showSignInScreen(MapActivity.this);
                }
                else {
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), false);
                    mFirebaseAuth.signOut();
                    Utilities.updateSignInMenuItem(mMenu, this, false);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Utilities.FIREBASE_SIGN_IN_KEY) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                getMapMarkersFromFirebase();
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(getString(R.string.map_activity_got_map_markers), mAlreadyShowingMapMarkersFromFirebase);
        outState.putParcelableArrayList(getString(R.string.map_activity_map_markers), new ArrayList<>(mMapMarkerList));
    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState!=null) {
            mAlreadyShowingMapMarkersFromFirebase = savedInstanceState.getBoolean(getString(R.string.map_activity_got_map_markers));
            mMapMarkerList = savedInstanceState.getParcelableArrayList(getString(R.string.map_activity_map_markers));

        }
    }


    //GoogleMap methods
    @Override public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMarkersForBoundsCalculation = new ArrayList<>();

        addUserLocationMarkerToMap();
        addObjectLocationMarkersToMap();

        if (mSavedInstanceState!=null) {
            addMapMarkersToMap(mMapMarkerList);
            readjustMapBoundsToIncludeMarkers();
        }

        mMap.setOnMapLoadedCallback(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnCameraIdleListener(this);
    }
    @Override public void onInfoWindowClick(Marker marker) {
        if (marker == null) return;

        String tag = (String) marker.getTag();
        if (tag != null && tag.length()>7) {
            String id = tag.substring(7,tag.length());
            if (tag.substring(0,7).equals(PLACE_TAG)) {
                showMarkerInfoDialog(marker);
            }
            else if (mDogsArrayList!=null && tag.substring(0,7).equals(DOG_TAG)) {
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

        Toast.makeText(this, R.string.click_on_pin_to_show_profile, Toast.LENGTH_LONG).show();
        readjustMapBoundsToIncludeMarkers();

    }
    @Override public void onMarkerDragStart(Marker marker) {

    }
    @Override public void onMarkerDrag(Marker marker) {

    }
    @Override public void onMarkerDragEnd(Marker marker) {
        //Position is updated automatically once the methods are implemented
        updateMarkerInFirebase(marker);
        //See also: https://stackoverflow.com/questions/14829195/google-maps-error-markers-position-is-not-updated-after-drag
    }
    @Override public void onCameraIdle() {
        mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
    }


    //Functional methods
    private void getExtras() {

        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.search_results_dogs_at_distance_list))) {
            mDogsArrayList = intent.getParcelableArrayListExtra(getString(R.string.search_results_dogs_at_distance_list));
        }
        if (intent.hasExtra(getString(R.string.search_results_families_at_distance_list))) {
            mFamiliesArrayList = intent.getParcelableArrayListExtra(getString(R.string.search_results_families_at_distance_list));
        }
        if (intent.hasExtra(getString(R.string.search_results_foundations_at_distance_list))) {
            mFoundationsArrayList = intent.getParcelableArrayListExtra(getString(R.string.search_results_foundations_at_distance_list));
        }
    }
    private void initializeParameters() {

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.map);
        }

        mUserCoordinates = new double[]{Utilities.getAppPreferenceUserLatitude(this), Utilities.getAppPreferenceUserLongitude(this)};
        mMapMarkerList = new ArrayList<>();
        mAlreadyShowingMapMarkersFromFirebase = false;

        mBinding = ButterKnife.bind(this);

        FirebaseDatabase firebaseDb = Utilities.getDatabase();
        mFirebaseDbReference = firebaseDb.getReference();
        mFirebaseDao = new FirebaseDao(this, this);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mCurrentFirebaseUser!=null) mFirebaseUid = mCurrentFirebaseUser.getUid();

        startListeningForUserLocation();
    }
    private void addUserLocationMarkerToMap() {
        if (mUserCoordinates!=null && !(mUserCoordinates[0] == 0.0 && mUserCoordinates[1] == 0.0)) {
            LatLng userCoords = new LatLng(mUserCoordinates[0], mUserCoordinates[1]);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(userCoords)
                    .title("My location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            Marker marker = mMap.addMarker(markerOptions);
            marker.setTag(PLACE_TAG+"userLocation");
            mMarkersForBoundsCalculation.add(marker);
            readjustMapBoundsToIncludeMarkers();
        }
    }
    private void addNewMarkerToMap(String title, String description, String address) {
        if (mMap!=null) {

            double[] markerCoordinates = Utilities.getGeoCoordinatesFromAddressString(this, address);
            // Add a marker for the user coordinates and center the map on them
            if (markerCoordinates!=null && !(markerCoordinates[0]==0.0 && markerCoordinates[1]==0.0)) {

                //Creating the marker
                LatLng userCoords = new LatLng(markerCoordinates[0], markerCoordinates[1]);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(userCoords)
                        .title(title)
                        .snippet(description)
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

                //Adding the marker to the map and to the Db
                Marker marker = mMap.addMarker(markerOptions);
                marker.setTag(PLACE_TAG + markerOptions.getTitle());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userCoords));

                //Creating the marker in Firebase
                MapMarker mapMarker = new MapMarker();
                mapMarker.setLt(Double.toString(markerCoordinates[0]));
                mapMarker.setLg(Double.toString(markerCoordinates[1]));
                mapMarker.setTt(title);
                mapMarker.setSn(description);
                mapMarker.setCl("orange");
                mapMarker.setOI(mFirebaseUid);
                String key = mFirebaseDao.addObjectToFirebaseDb(mapMarker);
                mapMarker.setUI(key);
                mMapMarkerList.add(mapMarker);

                marker.setTag(PLACE_TAG + key);
                mMarkersForBoundsCalculation.add(marker);
            }
        }
    }
    private void addObjectLocationMarkersToMap() {
        LatLng coords;
        double latitude;
        double longitude;
        Marker currentMarker;
        MarkerOptions currentMarkerOptions;
        if (mDogsArrayList!=null) {
            for (Dog dog : mDogsArrayList) {
                latitude = Double.parseDouble(dog.getGaLt());
                longitude = Double.parseDouble(dog.getGaLg());
                coords = new LatLng(latitude, longitude);
                currentMarkerOptions = new MarkerOptions().position(coords).title(dog.getNm());
                currentMarker = mMap.addMarker(currentMarkerOptions);
                currentMarker.setTag(DOG_TAG + dog.getUI());
                mMarkersForBoundsCalculation.add(currentMarker);
            }
        }
        if (mFamiliesArrayList!=null) {
            for (Family family : mFamiliesArrayList) {
                latitude = Double.parseDouble(family.getGaLt());
                longitude = Double.parseDouble(family.getGaLg());
                coords = new LatLng(latitude, longitude);
                currentMarkerOptions = new MarkerOptions().position(coords).title(family.getPn());
                currentMarker = mMap.addMarker(currentMarkerOptions);
                currentMarker.setTag(FAMILY_TAG + family.getUI());
                mMarkersForBoundsCalculation.add(currentMarker);
            }
        }
        if (mFoundationsArrayList!=null) {
            for (Foundation foundation : mFoundationsArrayList) {
                latitude = Double.parseDouble(foundation.getGaLt());
                longitude = Double.parseDouble(foundation.getGaLg());
                coords = new LatLng(latitude, longitude);
                currentMarkerOptions = new MarkerOptions().position(coords).title(foundation.getNm());
                currentMarker = mMap.addMarker(currentMarkerOptions);
                currentMarker.setTag(FOUNDATION_TAG + foundation.getUI());
                mMarkersForBoundsCalculation.add(currentMarker);
            }
        }
    }
    private void addMapMarkersToMap(List<MapMarker> mapMarkerList) {
        LatLng coords;
        double latitude;
        double longitude;
        Marker currentMarker;
        MarkerOptions currentMarkerOptions;
        if (mapMarkerList !=null) {
            for (MapMarker mapMarker : mapMarkerList) {
                latitude = Double.parseDouble(mapMarker.getLt());
                longitude = Double.parseDouble(mapMarker.getLg());
                coords = new LatLng(latitude, longitude);
                currentMarkerOptions = new MarkerOptions()
                        .position(coords)
                        .title(mapMarker.getTt())
                        .snippet(mapMarker.getSn())
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)); //TODO: change color according to color registered in Db
                currentMarker = mMap.addMarker(currentMarkerOptions);
                String tag = PLACE_TAG + mapMarker.getUI();
                currentMarker.setTag(tag);
                mMarkersForBoundsCalculation.add(currentMarker);
            }
        }
    }
    private void startListeningForUserLocation() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new TinDogLocationListener(this, this);
        if (mLocationManager!=null && Utilities.checkLocationPermission(this)) {
            if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 1.0f, mLocationListener);
            }
            else if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1.0f, mLocationListener);
            }
        }
    }
    private void showMarkerInfoDialog(final Marker marker) {

        //Get the dialog view
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_show_marker_info, null);
        final TextView descriptionTV = dialogView.findViewById(R.id.dialog_marker_show_info_description);
        final TextView addressTV = dialogView.findViewById(R.id.dialog_marker_show_info_address);

        //Building the dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        String title = marker.getTitle();
        String description = marker.getSnippet();
        LatLng position = marker.getPosition();
        String[] address = Utilities.getExactAddressFromGeoCoordinates(this, position.latitude, position.longitude);

        builder.setTitle(title);
        descriptionTV.setText(description);
        if (address!=null) {
            addressTV.setText(Utilities.getAddressStringFromComponents(null, address[0], address[1], address[2], address[3]));
        }

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.remove, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                showMarkerDeleteDialog(marker);
                dialog.dismiss();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) builder.setView(dialogView);
        else builder.setMessage(R.string.device_version_too_low);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void showMarkerDeleteDialog(final Marker marker) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.are_you_sure_delete_marker);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String tag = (String) marker.getTag();
                if (tag != null && tag.length()>7) {
                    String uniqueId = tag.substring(7,tag.length());
                    if (tag.substring(0,7).equals(PLACE_TAG)) {
                        for (MapMarker mapMarker : mMapMarkerList) { //TODO: make search more efficient (currently linear)
                            if (mapMarker.getUI().equals(uniqueId)) {
                                mMapMarkerList.remove(mapMarker);
                                mFirebaseDao.deleteObjectFromFirebaseDb(mapMarker);
                                break;
                            }
                        }
                        marker.remove();
                        mMarkersForBoundsCalculation.remove(marker);
                        //readjustMapBoundsToIncludeMarkers();
                    }
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void getMapMarkersFromFirebase() {
        if (mCurrentFirebaseUser != null && !TextUtils.isEmpty(mFirebaseUid)) {
            //mFirebaseDao.getFullObjectsListFromFirebaseDb(new MapMarker());
            mFirebaseDao.getObjectsByKeyValuePairFromFirebaseDb(new MapMarker(), "oi", mFirebaseUid, false);
        }
        else {
            Toast.makeText(this, R.string.please_sign_in_to_see_map_markers, Toast.LENGTH_SHORT).show();
        }
    }
    private void readjustMapBoundsToIncludeMarkers() {

        //Setting the map bounds (inspired by: https://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers)
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        if (mMarkersForBoundsCalculation == null || mMarkersForBoundsCalculation.size()==0) return;

        for (Marker marker : mMarkersForBoundsCalculation) {
            builder.include(marker.getPosition());
        }
        mBounds = builder.build();

        if (mMarkersForBoundsCalculation.size()==1) {
            int width = 1000; //map fragment(view) width;
            int height = 1000; //map fragment(view) height;
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mBounds, width, height, MAP_ZOOM_IN_PADDING_IN_PIXELS));
            //mMap.moveCamera(CameraUpdateFactory.newLatLng(userCoords));
        }
        else if (mMarkersForBoundsCalculation.size()>1){
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(mBounds, MAP_ZOOM_IN_PADDING_IN_PIXELS);
            mMap.animateCamera(cu);
        }

    }
    private void updateMarkerInFirebase(Marker marker) {

        String tag = (String) marker.getTag();
        if (tag != null && tag.length()>7) {
            String uniqueId = tag.substring(7, tag.length());
            if (tag.substring(0,7).equals(PLACE_TAG)) {
                for (MapMarker mapMarker : mMapMarkerList) { //TODO: make search more efficient (currently linear)
                    if (mapMarker.getUI().equals(uniqueId)) {
                        mapMarker.setLt(Double.toString(marker.getPosition().latitude));
                        mapMarker.setLg(Double.toString(marker.getPosition().longitude));
                        mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mapMarker, true);
                        break;
                    }
                }
            }
        }
    }
    private void removeListeners() {
        mFirebaseDao.removeListeners();
    }


    //View click listeners
    @OnClick(R.id.map_add_fab) public void showMarkerCreationDialog() {

        //Get the dialog view
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_enter_marker_info, null);
        final EditText title = dialogView.findViewById(R.id.dialog_marker_info_input_text_title);
        final EditText subtitle = dialogView.findViewById(R.id.dialog_marker_info_input_text_subtitle);
        final EditText country = dialogView.findViewById(R.id.dialog_marker_info_input_text_country);
        final EditText state = dialogView.findViewById(R.id.dialog_marker_info_input_text_state);
        final EditText city = dialogView.findViewById(R.id.dialog_marker_info_input_text_city);
        final EditText street = dialogView.findViewById(R.id.dialog_marker_info_input_text_street);

        //Building the dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        builder.setTitle(R.string.please_enter_marker_info);
        title.setText(""); title.setEnabled(true);
        subtitle.setText(""); subtitle.setEnabled(true);
        country.setText(""); country.setEnabled(true);
        state.setText(""); state.setEnabled(true);
        city.setText(""); city.setEnabled(true);
        street.setText(""); street.setEnabled(true);

        LatLng currentMapCenter = mMap.getCameraPosition().target;
        if (currentMapCenter!=null) {
            String[] addressArray = Utilities.getExactAddressFromGeoCoordinates(this, currentMapCenter.latitude, currentMapCenter.longitude);

            if (addressArray!=null) {
                country.setText(addressArray[3]);
                state.setText(addressArray[2]);
                city.setText(addressArray[1]);
                street.setText(addressArray[0]);
            }
        }

        final String address = Utilities.getAddressStringFromComponents(
                null,
                street.getText().toString(),
                city.getText().toString(),
                state.getText().toString(),
                country.getText().toString());

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                addNewMarkerToMap(title.getText().toString(), subtitle.getText().toString(), address);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) builder.setView(dialogView);
        else builder.setMessage(R.string.device_version_too_low);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }


    //Communication with other classes:

    //Communication with Location listener
    @Override public void onLocalCoordinatesFound(double longitude, double latitude) {

        double[] oldCoordinates = new double[]{
                Utilities.getAppPreferenceUserLatitude(this),
                Utilities.getAppPreferenceUserLongitude(this)
        };
        mUserCoordinates = new double[]{latitude, longitude};
        Utilities.setAppPreferenceUserLatitude(this, latitude);
        Utilities.setAppPreferenceUserLongitude(this, longitude);

        if (!(mUserCoordinates[0]==0.0 && mUserCoordinates[1]==0.0) && mLocationManager!=null) {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
            mLocationManager = null;
        }

        //This line is mosly relevant when first running the activity after installing the app from scratch, so that the user won't see wrong coordinates in the map
        if (oldCoordinates[0] == 0.0 && oldCoordinates[1] == 0.0 && mMap!=null) {
            addUserLocationMarkerToMap();
        }
    }

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {
    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onMapMarkerListFound(List<MapMarker> mapMarkerList) {
        if (!mAlreadyShowingMapMarkersFromFirebase) addMapMarkersToMap(mapMarkerList);
        mAlreadyShowingMapMarkersFromFirebase = true;

        //TODO: make the following lists merge more efficient
        boolean found = false;
        List<MapMarker> extrasList = new ArrayList<>();
        for (MapMarker fbMapMarker : mapMarkerList) {
            for (MapMarker mapMarker : mMapMarkerList) {
                if (fbMapMarker.getUI().equals(mapMarker.getUI())) {
                    found = true;
                    break;
                }
            }
            if (!found) extrasList.add(fbMapMarker);
        }
        mMapMarkerList.addAll(extrasList);
    }
    @Override public void onImageAvailable(android.net.Uri imageUri, String imageName) {

    }
    @Override public void onImageUploaded(List<String> uploadTimes) {

    }
}
