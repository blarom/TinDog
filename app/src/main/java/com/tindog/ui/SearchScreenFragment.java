package com.tindog.ui;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import com.tindog.R;
import com.tindog.adapters.DogsListRecycleViewAdapter;
import com.tindog.adapters.FamiliesListRecycleViewAdapter;
import com.tindog.adapters.FoundationsListRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.MapMarker;
import com.tindog.data.TinDogUser;
import com.tindog.resources.ImageSyncAsyncTaskLoader;
import com.tindog.resources.TinDogLocationListener;
import com.tindog.resources.Utilities;
import com.tindog.ui.MapActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


public class SearchScreenFragment extends Fragment implements
        DogsListRecycleViewAdapter.DogsListItemClickHandler,
        FamiliesListRecycleViewAdapter.FamiliesListItemClickHandler,
        FoundationsListRecycleViewAdapter.FoundationsListItemClickHandler,
        FirebaseDao.FirebaseOperationsHandler,
        TinDogLocationListener.LocationListenerHandler,
        LoaderManager.LoaderCallbacks<String>,
        ImageSyncAsyncTaskLoader.OnImageSyncOperationsHandler {


    //regionParameters
    private static final String DEBUG_TAG = "TinDog Search Screen";
    private static final int LIST_MAIN_IMAGES_SYNC_LOADER = 3698;
    @BindView(R.id.search_screen_magnifying_glass_image) ImageView mImageViewMagnifyingGlass;
    @BindView(R.id.search_screen_loading_indicator) ProgressBar mProgressBarLoadingIndicator;
    @BindView(R.id.search_screen_profile_selection_recycler_view) RecyclerView mRecyclerViewProfileSelection;
    @BindView(R.id.search_screen_distance_edittext) EditText mEditTextDistance;
    @BindView(R.id.search_screen_show_in_map_button) Button mButtonShowInMap;
    private Unbinder mBinding;
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
    private LocationManager mLocationManager;
    private TinDogLocationListener mLocationListener;
    private List<Dog> mDogsAtDistance;
    private List<Family> mFamiliesAtDistance;
    private List<Foundation> mFoundationsAtDistance;
    private String mProfileType;
    private String mRequestedDogProfileUI;
    private String mRequestedFamilyProfileUI;
    private String mRequestedFoundationProfileUI;
    private boolean mFoundResults;
    private ImageSyncAsyncTaskLoader mImageSyncAsyncTaskLoader;
    //endregion


    public SearchScreenFragment() {
        // Required empty public constructor
    }


    //Lifecycle methods
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getExtras();
        initializeParameters();
        getUserInfoFromFirebase();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_screen, container, false);

        initializeViews(rootView);
        setupRecyclerViews();
        getListsFromFirebase();
        startListeningForUserLocation();

        return rootView;
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        onSearchScreenOperationsHandler = (OnSearchScreenOperationsHandler) context;
    }
    @Override public void onDestroy() {
        super.onDestroy();
        mFirebaseDao.removeListeners();
    }
    @Override public void onDetach() {
        super.onDetach();
        onSearchScreenOperationsHandler = null;
        mFirebaseDao.removeListeners();
    }


    //Functionality methods
    private void getExtras() {
        mRequestedDogProfileUI = "";
        mRequestedFamilyProfileUI = "";
        mRequestedFoundationProfileUI = "";
        if (getArguments() != null) {
            mProfileType = getArguments().getString(getString(R.string.profile_type));
            mRequestedDogProfileUI = getArguments().getString(getString(R.string.requested_specific_dog_profile));
            mRequestedFamilyProfileUI = getArguments().getString(getString(R.string.requested_specific_family_profile));
            mRequestedFoundationProfileUI = getArguments().getString(getString(R.string.requested_specific_foundation_profile));
        }
    }
    private void initializeParameters() {
        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        mUser = new TinDogUser();
        mFirebaseDbReference = firebaseDb.getReference();
        mFirebaseDao = new FirebaseDao(getContext(), this);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentFirebaseUser = mFirebaseAuth.getCurrentUser();
        hasLocationPermissions = Utilities.checkLocationPermission(getContext());
        mDistance = 0;
        mFoundResults = false;

        mUserLongitude = 0.0;
        mUserLatitude = 0.0;
        if (getContext()!=null) {
            mUserLongitude = Utilities.getAppPreferenceUserLongitude(getContext());
            mUserLatitude = Utilities.getAppPreferenceUserLatitude(getContext());
        }
    }
    private void initializeViews(View rootView) {

        mBinding = ButterKnife.bind(this, rootView);

        mEditTextDistance.setText("100");
        mEditTextDistance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    getListsFromFirebase();
                }
                return false;
            }
        });
        mButtonShowInMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFoundResults) {

                    Intent intent = new Intent(getContext(), MapActivity.class);
                    if (mDogsAtDistance!=null && mProfileType.equals(getString(R.string.dog_profile))) {
                        intent.putParcelableArrayListExtra(getString(R.string.search_results_dogs_list), new ArrayList<>(mDogsAtDistance));
                        startActivity(intent);
                    }
                    else if (mFamiliesAtDistance!=null && mProfileType.equals(getString(R.string.family_profile))) {
                        intent.putParcelableArrayListExtra(getString(R.string.search_results_families_list), new ArrayList<>(mFamiliesAtDistance));
                        startActivity(intent);
                    }
                    else if (mFoundationsAtDistance!=null && mProfileType.equals(getString(R.string.foundation_profile))) {
                        intent.putParcelableArrayListExtra(getString(R.string.search_results_foundations_list), new ArrayList<>(mFoundationsAtDistance));
                        startActivity(intent);
                    }
                }
                else {
                    Toast.makeText(getContext(), R.string.please_wait_while_results_loaded, Toast.LENGTH_SHORT).show();
                }
            }
        });

        Uri imageUri = Uri.fromFile(new File("//android_asset/magnifying_glass.png"));
        Picasso.with(getContext())
                .load(imageUri)
                .error(R.drawable.ic_image_not_available)
                .into(mImageViewMagnifyingGlass);

        mImageViewMagnifyingGlass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getListsFromFirebase();
            }
        });
    }
    private void showLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.VISIBLE);
    }
    private void hideLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.INVISIBLE);
    }
    private void startListeningForUserLocation() {
        if (getContext()!=null) {
            mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            mLocationListener = new TinDogLocationListener(getContext(), this);
            if (mLocationManager!=null && Utilities.checkLocationPermission(getContext())) {
                if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 1.0f, mLocationListener);
                }
                else if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1.0f, mLocationListener);
                }
            }
        }
    }
    private void getUserInfoFromFirebase() {
        if (mCurrentFirebaseUser != null) {
            // Name, email address, and profile photo Url
            mNameFromFirebase = mCurrentFirebaseUser.getDisplayName();
            mEmailFromFirebase = mCurrentFirebaseUser.getEmail();
            mPhotoUriFromFirebase = mCurrentFirebaseUser.getPhotoUrl();

            mFirebaseUid = mCurrentFirebaseUser.getUid();

            mUser.setUI(mFirebaseUid);
            mFirebaseDao.getUniqueObjectFromFirebaseDbOrCreateIt(mUser, true);
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

        if (mProfileType.equals(getString(R.string.dog_profile))) {
            if (mDogsListRecycleViewAdapter==null) mDogsListRecycleViewAdapter = new DogsListRecycleViewAdapter(getContext(), this, null);
            mRecyclerViewProfileSelection.setAdapter(mDogsListRecycleViewAdapter);
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            if (mFamiliesListRecycleViewAdapter==null) mFamiliesListRecycleViewAdapter = new FamiliesListRecycleViewAdapter(getContext(), this, null);
            mRecyclerViewProfileSelection.setAdapter(mFamiliesListRecycleViewAdapter);
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            if (mFoundationsListRecycleViewAdapter==null) mFoundationsListRecycleViewAdapter = new FoundationsListRecycleViewAdapter(getContext(), this, null);
            mRecyclerViewProfileSelection.setAdapter(mFoundationsListRecycleViewAdapter);
        }
    }
    private void getListsFromFirebase() {

        //Setting up the item lists (results are received through the FirebaseDao interface, see methods below)
        if (mProfileType.equals(getString(R.string.dog_profile))) {
            if (TextUtils.isEmpty(mRequestedDogProfileUI)) mFirebaseDao.getFullObjectsListFromFirebaseDb(new Dog(), true);
            else mFirebaseDao.getUniqueObjectFromFirebaseDbOrCreateIt(new Dog(mRequestedDogProfileUI), true);
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            if (TextUtils.isEmpty(mRequestedFamilyProfileUI)) mFirebaseDao.getFullObjectsListFromFirebaseDb(new Family(), true);
            else mFirebaseDao.getUniqueObjectFromFirebaseDbOrCreateIt(new Family(mRequestedFamilyProfileUI), true);
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            if (TextUtils.isEmpty(mRequestedFoundationProfileUI)) mFirebaseDao.getFullObjectsListFromFirebaseDb(new Foundation(), true);
            else mFirebaseDao.getUniqueObjectFromFirebaseDbOrCreateIt(new Foundation(mRequestedFoundationProfileUI), true);
        }
        else return;

        showLoadingIndicator();
    }
    private void updateRecyclerView() {

        if (mProfileType.equals(getString(R.string.dog_profile))) {
            mDogsListRecycleViewAdapter.setContents(mDogsAtDistance);
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            mFamiliesListRecycleViewAdapter.setContents(mFamiliesAtDistance);
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            mFoundationsListRecycleViewAdapter.setContents(mFoundationsAtDistance);
        }
        hideLoadingIndicator();
    }
    private void startImageSyncThread() {

        Log.i(DEBUG_TAG, "Called startImageSyncThread");
        if (getActivity()!=null) {
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> imageSyncAsyncTaskLoader = loaderManager.getLoader(LIST_MAIN_IMAGES_SYNC_LOADER);
            if (imageSyncAsyncTaskLoader == null) {
                loaderManager.initLoader(LIST_MAIN_IMAGES_SYNC_LOADER, null, this);
            }
            else {
                if (mImageSyncAsyncTaskLoader!=null) {
                    //The asynctask is called twice: once on fragment start, and then when the user location is found
                    //In order to avoid performing background image syncs twice on the same images, we stop the asynctask operation here if the loader is being restarted
                    mImageSyncAsyncTaskLoader.stopUpdatingImagesForObjects();
                    mImageSyncAsyncTaskLoader.cancelLoadInBackground();
                    mImageSyncAsyncTaskLoader = null;
                }
                loaderManager.restartLoader(LIST_MAIN_IMAGES_SYNC_LOADER, null, this);
            }
        }

    }
    public void stopImageSyncThread() {
        if (mImageSyncAsyncTaskLoader!=null) {
            mImageSyncAsyncTaskLoader.stopUpdatingImagesForObjects();
            getLoaderManager().destroyLoader(LIST_MAIN_IMAGES_SYNC_LOADER);
        }
    }
    private void createFakeDogsForTesting() {
        //*********Special code designed to create dogs near the user, used for testing purposes only************
        mFirebaseDao.populateFirebaseDbWithDummyData(getContext(), mUserLatitude, mUserLongitude);
    }


    //Location methods
    private void updateObjectListAccordingToDistance() {
        getObjectsAtDistance();
        startImageSyncThread();
        updateRecyclerView();
        sendObjectsAtDistanceToInterface();
    }
    private void getObjectsAtDistance() {

        mDistance = getRequestedDistanceFromUserInput();

        if (mProfileType.equals(getString(R.string.dog_profile))) {
            mDogsAtDistance = (List<Dog>) getObjectsWithinDistance(mDogsList, mDistance);
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            mFamiliesAtDistance = (List<Family>) getObjectsWithinDistance(mFamiliesList, mDistance);
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            mFoundationsAtDistance = (List<Foundation>) getObjectsWithinDistance(mFoundationsList, mDistance);
        }
    }
    private void sendObjectsAtDistanceToInterface() {
        if (mProfileType.equals(getString(R.string.dog_profile))) {
            onSearchScreenOperationsHandler.onDogsFound(mDogsAtDistance);
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            onSearchScreenOperationsHandler.onFamiliesFound(mFamiliesAtDistance);
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            onSearchScreenOperationsHandler.onFoundationsFound(mFoundationsAtDistance);
        }
    }
    private Object getObjectsWithinDistance(Object object, int distanceMeters) {

        if (!(object instanceof List)) return object;
        List<Object> objectsList = (List<Object>) object;

        if (objectsList.size() > 0) {
            if (objectsList.get(0) instanceof Dog) {
                List<Dog> dogsNearby = new ArrayList<>();
                for (int i=0; i<objectsList.size(); i++) {
                    Dog dog = (Dog) objectsList.get(i);
                    boolean isNearby = checkIfObjectIsNearby(
                            Utilities.getAddressStringFromComponents(dog.getStN(), dog.getSt(), dog.getCt(), dog.getSe(), dog.getCn()),
                            dog.getCn(),
                            dog.getGaC(),
                            dog.getGaLt(),
                            dog.getGaLg(),
                            distanceMeters);
                    if (isNearby) dogsNearby.add(dog);
                }
                return dogsNearby;
            }
            else if (objectsList.get(0) instanceof Family) {
                List<Family> familiesNearby = new ArrayList<>();
                for (int i=0; i<objectsList.size(); i++) {
                    Family family = (Family) objectsList.get(i);
                    boolean isNearby = checkIfObjectIsNearby(
                            Utilities.getAddressStringFromComponents(null, family.getSt(), family.getCt(), family.getSe(), family.getCn()),
                            family.getCn(),
                            family.getGaC(),
                            family.getGaLt(),
                            family.getGaLg(),
                            distanceMeters);
                    if (isNearby) familiesNearby.add(family);
                }
                return familiesNearby;
            }
            else if (objectsList.get(0) instanceof Foundation) {
                List<Foundation> foundationsNearby = new ArrayList<>();
                for (int i=0; i<objectsList.size(); i++) {
                    Foundation foundation = (Foundation) objectsList.get(i);
                    boolean isNearby = checkIfObjectIsNearby(
                            Utilities.getAddressStringFromComponents(foundation.getStN(), foundation.getSt(), foundation.getCt(), foundation.getSe(), foundation.getCn()),
                            foundation.getCn(),
                            foundation.getGaC(),
                            foundation.getGaLt(),
                            foundation.getGaLg(),
                            distanceMeters);
                    if (isNearby) foundationsNearby.add(foundation);
                }
                return foundationsNearby;
            }
        }
        return objectsList;
    }
    private boolean checkIfObjectIsNearby(String addressString, String country, String geoAddressCountry, String latitudeAsString, String longitudeAsString, int distanceMeters) {

        //If the city value is empty, return true anyway since the object may be relevant
        if (TextUtils.isEmpty(addressString)) return true;

        double geoAddressLatitude;
        double geoAddressLongitude;

        if (!TextUtils.isEmpty(latitudeAsString)) geoAddressLatitude = Double.parseDouble(latitudeAsString);
        else geoAddressLatitude = 0.0;
        if (!TextUtils.isEmpty(longitudeAsString)) geoAddressLongitude = Double.parseDouble(longitudeAsString);
        else geoAddressLongitude = 0.0;

        //If the device can obtain valid up-to-date geolocation data for the object's registered address, use it instead of the stored values,
        // since these may possibly be have been updated when the user last saved the object's profile
        Address address = Utilities.getAddressObjectFromAddressString(getContext(), addressString);
        if (address!=null) {
            geoAddressCountry = address.getCountryCode();
            geoAddressLatitude = address.getLatitude();
            geoAddressLongitude = address.getLongitude();
        }

        //If valid data is available, then check if the object is nearby. If it is, then add the object to the Nearby list
        if (!TextUtils.isEmpty(geoAddressCountry) && !(geoAddressLatitude==0.0 && geoAddressLongitude==0.0)) {
            return isWithinDistance(geoAddressLatitude, geoAddressLongitude, distanceMeters) && isInCountry(geoAddressCountry, country);
        }
        return false;
    }
    private boolean isWithinDistance(double latitude, double longitude, int distanceMeters) {
        if (!(latitude == 0.0 && longitude == 0.0)) {
            float[] objectDistance = new float[1];
            Location.distanceBetween(mUserLatitude, mUserLongitude, latitude, longitude, objectDistance);
            boolean isWithinDistance = objectDistance[0] < distanceMeters;
            return isWithinDistance;
        }
        return false;
    }
    private boolean isInCountry(String code, String objectCountry) {
        if (mUser.getLC()) {
            Locale locale = new Locale("", code);
            return locale.getDisplayCountry().equals(objectCountry);
        }
        else return true;
    }


    //Communication with other classes:

    //Communication with RecyclerView adapters
    @Override public void onDogsListItemClick(int clickedItemIndex) {
        onSearchScreenOperationsHandler.onProfileSelected(clickedItemIndex);
    }
    @Override public void onFamiliesListItemClick(int clickedItemIndex) {
        onSearchScreenOperationsHandler.onProfileSelected(clickedItemIndex);
    }
    @Override public void onFoundationsListItemClick(int clickedItemIndex) {
        onSearchScreenOperationsHandler.onProfileSelected(clickedItemIndex);
    }

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {
        if (getContext()==null) return; //Prevents the code from continuing to work with a null context if the user exited the fragment too fast
        mDogsList = dogsList;
        mFoundResults = true;

        //If the user's coordinates are unknown, do nothing (and wait for the coordinates to be found in onLocalCoordinatesFound)
        if (mUserLongitude==0.0 && mUserLatitude==0.0) return;

        //If the user requested a dogs list, then show the list at the requested distance
        if (TextUtils.isEmpty(mRequestedDogProfileUI)) updateObjectListAccordingToDistance();

        //If the user requested a specific dog, then update its index in the list for the parent activity.
        else {
            if (mDogsList!=null && mDogsList.size()>0) {
                onSearchScreenOperationsHandler.onDogsFound(mDogsList);
                onSearchScreenOperationsHandler.onProfileSelected(0);
            }
        }
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
        if (getContext()==null) return; //Prevents the code from continuing to work with a null context if the user exited the fragment too fast
        mFamiliesList = familiesList;
        mFoundResults = true;

        //If the user's coordinates are unknown, do nothing (and wait for the coordinates to be found in onLocalCoordinatesFound)
        if (mUserLongitude==0.0 && mUserLatitude==0.0) return;

        //If the user requested a family list, then show the list at the requested distance
        if (TextUtils.isEmpty(mRequestedFamilyProfileUI)) updateObjectListAccordingToDistance();

        //If the user requested a specific family, then update its index in the list for the parent activity.
        else {
            if (mFamiliesList!=null && mFamiliesList.size()>0) {
                onSearchScreenOperationsHandler.onFamiliesFound(mFamiliesList);
                onSearchScreenOperationsHandler.onProfileSelected(0);
            }
        }
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {
        if (getContext()==null) return; //Prevents the code from continuing to work with a null context if the user exited the fragment too fast
        mFoundationsList = foundationsList;
        mFoundResults = true;

        //If the user's coordinates are unknown, do nothing (and wait for the coordinates to be found in onLocalCoordinatesFound)
        if (mUserLongitude==0.0 && mUserLatitude==0.0) return;

        //If the user requested a foundations list, then show the list at the requested distance
        if (TextUtils.isEmpty(mRequestedFoundationProfileUI)) updateObjectListAccordingToDistance();

        //If the user requested a specific foundation, then update its index in the list for the parent activity.
        else {
            if (mFoundationsList!=null && mFoundationsList.size()>0) {
                onSearchScreenOperationsHandler.onFoundationsFound(mFoundationsList);
                onSearchScreenOperationsHandler.onProfileSelected(0);
            }
        }
    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {
        if (getContext()==null) return; //Prevents the code from continuing to work with a null context if the user exited the fragment too fast
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
    @Override public void onMapMarkerListFound(List<MapMarker> markersList) {

    }
    @Override public void onImageAvailable(Uri imageUri, String currentImageName) {
    }
    @Override public void onImageUploaded(List<String> uploadTimes) {

    }

    //Communication with Location handler
    @Override public void onLocalCoordinatesFound(double longitude, double latitude) {
        mUserLongitude = longitude;
        mUserLatitude = latitude;
        Utilities.setAppPreferenceUserLongitude(getContext(), longitude);
        Utilities.setAppPreferenceUserLatitude(getContext(), latitude);

        createFakeDogsForTesting(); //TODO: ***********Remove this in regular app

        if (!(mUserLongitude == 0.0 && mUserLatitude == 0.0) && mLocationManager!=null) {
            hideLoadingIndicator();
            mLocationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
            mLocationManager = null;
        }
        if (getContext()==null) return; //Prevents the code from continuing to work with a null context if the user exited the fragment too fast
        updateObjectListAccordingToDistance();
    }

    //Communication with Loader
    @NonNull @Override public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {

        if (id == LIST_MAIN_IMAGES_SYNC_LOADER && mImageSyncAsyncTaskLoader==null) {
            mImageSyncAsyncTaskLoader =  new ImageSyncAsyncTaskLoader(getContext(), getString(R.string.task_sync_list_main_images),
                    mProfileType, mDogsAtDistance, mFamiliesAtDistance, mFoundationsAtDistance, this);
            return mImageSyncAsyncTaskLoader;
        }
        return new ImageSyncAsyncTaskLoader(getContext(), "", null, null, null, null, this);
    }
    @Override public void onLoadFinished(@NonNull Loader<String> loader, String data) {
        if (loader.getId() == LIST_MAIN_IMAGES_SYNC_LOADER) {
            if (getContext()!=null) updateRecyclerView();
            stopImageSyncThread();
        }
    }
    @Override public void onLoaderReset(@NonNull Loader<String> loader) {

    }

    //Communication with ImageSyncAsyncTaskLoader
    @Override public void onDisplayRefreshRequested() {
        if (getContext()!=null) updateRecyclerView();
    }

    //Communication with parent activity
    private OnSearchScreenOperationsHandler onSearchScreenOperationsHandler;
    public interface OnSearchScreenOperationsHandler {
        void onProfileSelected(int clickedItemIndex);
        void onDogsFound(List<Dog> dogList);
        void onFamiliesFound(List<Family> familyList);
        void onFoundationsFound(List<Foundation> foundationList);
    }
    public void reloadDataAfterSuccessfulSignIn() {
        getUserInfoFromFirebase();
        getListsFromFirebase();
    }
}
