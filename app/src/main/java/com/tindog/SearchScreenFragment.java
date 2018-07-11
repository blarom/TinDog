package com.tindog;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tindog.adapters.DogsListRecycleViewAdapter;
import com.tindog.adapters.FamiliesListRecycleViewAdapter;
import com.tindog.adapters.FoundationsListRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.TinDogUser;
import com.tindog.resources.SharedMethods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SearchScreenFragment extends Fragment implements
        DogsListRecycleViewAdapter.DogsListItemClickHandler,
        FamiliesListRecycleViewAdapter.FamiliesListItemClickHandler,
        FoundationsListRecycleViewAdapter.FoundationsListItemClickHandler,
        FirebaseDao.FirebaseOperationsHandler,
        LocationListener {

    //regionParameters
    private static final String DEBUG_TAG = "TinDog Search Screen";

    @BindView(R.id.search_screen_magnifying_glass_image) ImageView mImageViewMagnifyingGlass;
    @BindView(R.id.search_screen_profile_selection_recycler_view) RecyclerView mRecyclerViewProfileSelection;
    @BindView(R.id.search_screen_list_selection_tab_layout) TabLayout mTabLayoutListSelection;
    @BindView(R.id.search_screen_distance_edittext) EditText mEditTextDistance;

    private TinDogUser mUser;
    private FirebaseDao mFirebaseDao;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentFirebaseUser;
    private DogsListRecycleViewAdapter mDogsListRecycleViewAdapter;
    private FamiliesListRecycleViewAdapter mFamiliesListRecycleViewAdapter;
    private FoundationsListRecycleViewAdapter mFoundationsListRecycleViewAdapter;
    private DatabaseReference mFirebaseDbReference;
    private int mDistance;
    private double mUserLongitude;
    private double mUserLatitude;
    private List<Dog> mDogsList;
    private List<Family> mFamiliesList;
    private List<Foundation> mFoundationsList;
    private String mNameFromFirebase;
    private String mEmailFromFirebase;
    private Uri mPhotoUriFromFirebase;
    private String mFirebaseUid;
    private boolean hasLocationPermissions;
    //endregion


    public SearchScreenFragment() {
        // Required empty public constructor
    }


    //Lifecycle methods
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeParameters();
        getUserInfoFromFirebase();
        getListsFromFirebase();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_screen, container, false);

        initializeViews(rootView);
        setupRecyclerViews();
        updateListsAccordingToDistance();

        return rootView;
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        onProfileSelectedListener = (OnProfileSelectedListener) context;
    }
    @Override public void onDetach() {
        super.onDetach();
        onProfileSelectedListener = null;
    }


    //Functionality methods
    private void initializeParameters() {
        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        mUser = new TinDogUser();
        mFirebaseDbReference = firebaseDb.getReference();
        mFirebaseDao = new FirebaseDao(getContext(), this);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentFirebaseUser = mFirebaseAuth.getCurrentUser();
        hasLocationPermissions = checkLocationPermission();

        if (getContext()!=null) {
            LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            LocationListener locationListener = new LocationListener() {

                @Override public void onLocationChanged(Location location) {
                    //inspired by: https://stackoverflow.com/questions/1513485/how-do-i-get-the-current-gps-location-programmatically-in-android

                    mUserLongitude = location.getLongitude();
                    mUserLatitude = location.getLatitude();
                    Log.v(DEBUG_TAG, "Longitude:" + mUserLongitude);
                    Log.v(DEBUG_TAG, "Latitude:" + mUserLatitude);

                    String cityName;
                    Geocoder gcd = new Geocoder(getContext(), Locale.getDefault());
                    List<Address> addresses;
                    try {
                        addresses = gcd.getFromLocation(mUserLatitude, mUserLongitude, 1);
                        if (addresses.size() > 0) {
                            cityName = addresses.get(0).getLocality();
                            Log.v(DEBUG_TAG, cityName);
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                @Override public void onStatusChanged(String s, int i, Bundle bundle) {

                }
                @Override public void onProviderEnabled(String s) {

                }
                @Override public void onProviderDisabled(String s) {

                }
            };
            if (locationListener!=null && locationListener!=null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1.0f, locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 1.0f, locationListener);
            }
        }

    }
    private void initializeViews(View rootView) {

        ButterKnife.bind(this, rootView);

        mEditTextDistance.setText("100");
        mEditTextDistance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    updateListsAccordingToDistance();
                }
                return false;
            }
        });

        mImageViewMagnifyingGlass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateListsAccordingToDistance();
            }
        });
    }
    private void getUserInfoFromFirebase() {
        if (mCurrentFirebaseUser != null) {
            // Name, email address, and profile photo Url
            mNameFromFirebase = mCurrentFirebaseUser.getDisplayName();
            mEmailFromFirebase = mCurrentFirebaseUser.getEmail();
            mPhotoUriFromFirebase = mCurrentFirebaseUser.getPhotoUrl();

            mFirebaseUid = mCurrentFirebaseUser.getUid();

            mUser.setUniqueIdentifier(mFirebaseUid);
            mFirebaseDao.getUniqueObjectFromFirebaseDb(mUser);
        }
    }
    private int getRequestedDistanceFromUserInput() {
        String userInput = mEditTextDistance.getText().toString();
        if (hasLocationPermissions) {
            if (TextUtils.isEmpty(userInput)) return 0;
            else return Integer.parseInt(userInput)*1000;
        }
        else return 40000000;
    }
    private void setupRecyclerViews() {

        //Setting up the RecyclerView adapters
        mRecyclerViewProfileSelection.setLayoutManager(new LinearLayoutManager(getContext()));
        if (mDogsListRecycleViewAdapter==null) mDogsListRecycleViewAdapter = new DogsListRecycleViewAdapter(getContext(), this, null);
        if (mFamiliesListRecycleViewAdapter==null) mFamiliesListRecycleViewAdapter = new FamiliesListRecycleViewAdapter(getContext(), this, null);
        if (mFoundationsListRecycleViewAdapter==null) mFoundationsListRecycleViewAdapter = new FoundationsListRecycleViewAdapter(getContext(), this, null);
        mRecyclerViewProfileSelection.setAdapter(mDogsListRecycleViewAdapter);

        mTabLayoutListSelection.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        mRecyclerViewProfileSelection.setAdapter(mDogsListRecycleViewAdapter);
                        break;
                    case 1:
                        mRecyclerViewProfileSelection.setAdapter(mFamiliesListRecycleViewAdapter);
                        break;
                    case 2:
                        mRecyclerViewProfileSelection.setAdapter(mFoundationsListRecycleViewAdapter);
                        break;
                    default:
                        mRecyclerViewProfileSelection.setAdapter(mDogsListRecycleViewAdapter);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }
    private void getListsFromFirebase() {
        //Setting up the item lists (results are received through the FirebaseDao interface, see methods below)
        mFirebaseDao.getFullObjectsListFromFirebaseDb(new Dog());
        mFirebaseDao.getFullObjectsListFromFirebaseDb(new Family());
        mFirebaseDao.getFullObjectsListFromFirebaseDb(new Foundation());
    }
    private void updateListsAccordingToDistance() {
        mDistance = getRequestedDistanceFromUserInput();
        updateDogsListAccordingToDistance();
        updateFamiliesListAccordingToDistance();
        updateFoundationsListAccordingToDistance();
    }
    private void updateDogsListAccordingToDistance() {
        List<Dog> dogsAtDistance = (List<Dog>) getObjectsWithinDistance(mDogsList, mDistance);
        mDogsListRecycleViewAdapter.setContents(dogsAtDistance);
    }
    private void updateFamiliesListAccordingToDistance() {
        List<Family> familiesAtDistance = (List<Family>) getObjectsWithinDistance(mFamiliesList, mDistance);
        mFamiliesListRecycleViewAdapter.setContents(familiesAtDistance);
    }
    private void updateFoundationsListAccordingToDistance() {
        List<Foundation> foundationsAtDistance = (List<Foundation>) getObjectsWithinDistance(mFoundationsList, mDistance);
        mFoundationsListRecycleViewAdapter.setContents(foundationsAtDistance);
    }
    private boolean checkLocationPermission() {
        if (getContext()!=null && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        else return false;
    }


    //Location listener methods
    @Override public void onLocationChanged(Location location) {
        //inspired by: https://stackoverflow.com/questions/1513485/how-do-i-get-the-current-gps-location-programmatically-in-android

        mUserLongitude = location.getLongitude();
        mUserLatitude = location.getLatitude();
        Log.v(DEBUG_TAG, "Longitude:" + mUserLongitude);
        Log.v(DEBUG_TAG, "Latitude:" + mUserLatitude);

        String cityName;
        Geocoder gcd = new Geocoder(getContext(), Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = gcd.getFromLocation(mUserLatitude, mUserLongitude, 1);
            if (addresses.size() > 0) {
                cityName = addresses.get(0).getLocality();
                Log.v(DEBUG_TAG, cityName);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Override public void onStatusChanged(String s, int i, Bundle bundle) {

    }
    @Override public void onProviderEnabled(String s) {

    }
    @Override public void onProviderDisabled(String s) {

    }
    private Object getObjectsWithinDistance(Object object, int distanceMeters) {

        Address address;
        String city;
        if (!(object instanceof List)) return object;
        List<Object> objectsList = (List<Object>) object;

        if (objectsList.size() > 0) {
            if (objectsList.get(0) instanceof Dog) {
                List<Dog> dogsNearby = new ArrayList<>();
                for (int i=0; i<objectsList.size(); i++) {
                    Dog dog = (Dog) objectsList.get(i);
                    city = dog.getCity();
                    if (TextUtils.isEmpty(city)) dogsNearby.add(dog);
                    else {
                        address = SharedMethods.getAddressFromCity(getContext(), dog.getCity());
                        if (isNearby(address, distanceMeters)) dogsNearby.add(dog);
                    }
                }
                return dogsNearby;
            }
            else if (objectsList.get(0) instanceof Family) {
                List<Family> familiesNearby = new ArrayList<>();
                for (int i=0; i<objectsList.size(); i++) {
                    Family family = (Family) objectsList.get(i);
                    city = family.getCity();
                    if (TextUtils.isEmpty(city)) familiesNearby.add(family);
                    else {
                        address = SharedMethods.getAddressFromCity(getContext(), family.getCity());
                        if (isNearby(address, distanceMeters)) familiesNearby.add(family);
                    }
                }
                return familiesNearby;
            }
            else if (objectsList.get(0) instanceof Foundation) {
                List<Foundation> foundationsNearby = new ArrayList<>();
                for (int i=0; i<objectsList.size(); i++) {
                    Foundation foundation = (Foundation) objectsList.get(i);
                    city = foundation.getCity();
                    if (TextUtils.isEmpty(city)) foundationsNearby.add(foundation);
                    else {
                        address = SharedMethods.getAddressFromCity(getContext(), foundation.getCity());
                        if (isNearby(address, distanceMeters)) foundationsNearby.add(foundation);
                    }
                }
                return foundationsNearby;
            }
        }
        return objectsList;
    }
    private boolean isNearby(Address address, int distanceMeters) {
        if (address!=null) {
            float[] objectDistance = new float[1];
            Location.distanceBetween(mUserLatitude, mUserLongitude, address.getLatitude(), address.getLongitude(), objectDistance);
            boolean isWithinDistance = objectDistance[0] < distanceMeters;
            return isWithinDistance;
        }
        return false;
    }


    //Communication with other activities/fragments:

    //Communication with RecyclerView adapters
    private OnProfileSelectedListener onProfileSelectedListener;
    public interface OnProfileSelectedListener {
        void onProfileSelected(String profile, int clickedItemIndex);
    }
    @Override public void onDogsListItemClick(int clickedItemIndex) {
        onProfileSelectedListener.onProfileSelected(getString(R.string.dog_profile), clickedItemIndex);
    }
    @Override public void onFamiliesListItemClick(int clickedItemIndex) {
        onProfileSelectedListener.onProfileSelected(getString(R.string.family_profile), clickedItemIndex);
    }
    @Override public void onFoundationsListItemClick(int clickedItemIndex) {
        onProfileSelectedListener.onProfileSelected(getString(R.string.foundation_profile), clickedItemIndex);
    }

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {
        mDogsList = dogsList;
        updateDogsListAccordingToDistance();
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
        mFamiliesList = familiesList;
        updateFamiliesListAccordingToDistance();
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {
        mFoundationsList = foundationsList;
        updateFoundationsListAccordingToDistance();
    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {
        if (usersList.size()==1) {
            if (usersList.get(0) != null) {
                mUser = usersList.get(0);
            }
        }
        else if (usersList.size()>1) {
            mUser = usersList.get(0);
            Log.i(DEBUG_TAG, "Warning! Multiple users found for the same Uid.");
        }
        else {
            Toast.makeText(getContext(), "Sorry, an error occurred while fetching your preferences. Searches may be incorrect.", Toast.LENGTH_SHORT).show();
        }

    }
    @Override public void onImageAvailable(Uri imageUri, String imageName) {

    }

    //Communication with parent activity
    public void reloadDataAfterSuccessfulSignIn() {
        getUserInfoFromFirebase();
        getListsFromFirebase();
    }
}
