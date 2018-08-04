package com.tindog.ui;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.tindog.R;
import com.tindog.adapters.DogsListRecycleViewAdapter;
import com.tindog.adapters.FamiliesListRecycleViewAdapter;
import com.tindog.adapters.FoundationsListRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.Foundation;
import com.tindog.data.TinDogUser;
import com.tindog.resources.ImageSyncAsyncTaskLoader;
import com.tindog.resources.TinDogLocationListener;
import com.tindog.resources.Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


public class SearchScreenFragment extends Fragment implements
        DogsListRecycleViewAdapter.DogsListItemClickHandler,
        FamiliesListRecycleViewAdapter.FamiliesListItemClickHandler,
        FoundationsListRecycleViewAdapter.FoundationsListItemClickHandler,
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
    private Unbinder mBinding;
    private TinDogUser mUser;
    private DogsListRecycleViewAdapter mDogsListRecycleViewAdapter;
    private FamiliesListRecycleViewAdapter mFamiliesListRecycleViewAdapter;
    private FoundationsListRecycleViewAdapter mFoundationsListRecycleViewAdapter;
    private int mDistance;
    private double mUserLongitude;
    private double mUserLatitude;
    private List<Dog> mDogsList;
    private List<Family> mFamiliesList;
    private List<Foundation> mFoundationsList;
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
    private int mSelectedProfileIndex;
    private int mProfileSelectionRecyclerViewPosition;
    private CountDownTimer mTimer;
    private boolean mUpdatedRecyclerView;
    //endregion


    //Lifecycle methods
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        onSearchScreenOperationsHandler = (OnSearchScreenOperationsHandler) context;
    }
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getExtras();
        initializeParameters();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_screen, container, false);

        initializeViews(rootView);
        showLoadingIndicator();

        mUpdatedRecyclerView = false;
        mTimer = new CountDownTimer(10000, 500) {

            public void onTick(long millisUntilFinished) {
                if (mUpdatedRecyclerView && !(mDogsAtDistance.size()==0 && mFamiliesAtDistance.size()==0 && mFoundationsAtDistance.size()==0)) {
                    hideLoadingIndicator();
                    mTimer.cancel();
                }
            }

            public void onFinish() {
                if (!mUpdatedRecyclerView) {
                    hideLoadingIndicator();
                    Toast.makeText(getContext(), R.string.no_results_found,Toast.LENGTH_SHORT).show();
                }

            }
        }.start();

        setupRecyclerViews();
        updateRecyclerView();
        startListeningForUserLocation();

        return rootView;
    }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(getString(R.string.search_screen_fragment_rv_position), mProfileSelectionRecyclerViewPosition);
        outState.putParcelableArrayList(getString(R.string.search_results_dogs_list), new ArrayList<>(mDogsAtDistance));
        outState.putParcelableArrayList(getString(R.string.search_results_families_list), new ArrayList<>(mFamiliesAtDistance));
        outState.putParcelableArrayList(getString(R.string.search_results_foundations_list), new ArrayList<>(mFoundationsAtDistance));
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }
    @Override public void onDestroy() {
        super.onDestroy();
        stopListeningForLocation();
    }
    @Override public void onDetach() {
        super.onDetach();
        stopListeningForLocation();
        onSearchScreenOperationsHandler = null;
    }


    //Functionality methods
    private void getExtras() {
        mRequestedDogProfileUI = "";
        mRequestedFamilyProfileUI = "";
        mRequestedFoundationProfileUI = "";
        mDogsAtDistance = new ArrayList<>();
        mFamiliesAtDistance = new ArrayList<>();
        mFoundationsAtDistance = new ArrayList<>();
        if (getArguments() != null) {
            mProfileType = getArguments().getString(getString(R.string.profile_type));
            mRequestedDogProfileUI = getArguments().getString(getString(R.string.requested_specific_dog_profile));
            mRequestedFamilyProfileUI = getArguments().getString(getString(R.string.requested_specific_family_profile));
            mRequestedFoundationProfileUI = getArguments().getString(getString(R.string.requested_specific_foundation_profile));
            mSelectedProfileIndex = getArguments().getInt(getString(R.string.selected_profile_index));
            mProfileSelectionRecyclerViewPosition = getArguments().getInt(getString(R.string.search_screen_fragment_rv_position));
            mDogsList = getArguments().getParcelableArrayList(getString(R.string.search_results_dogs_list));
            mFamiliesList = getArguments().getParcelableArrayList(getString(R.string.search_results_families_list));
            mFoundationsList = getArguments().getParcelableArrayList(getString(R.string.search_results_foundations_list));
            mDogsAtDistance = getArguments().getParcelableArrayList(getString(R.string.search_results_dogs_at_distance));
            mFamiliesAtDistance = getArguments().getParcelableArrayList(getString(R.string.search_results_families_at_distance));
            mFoundationsAtDistance = getArguments().getParcelableArrayList(getString(R.string.search_results_foundations_at_distance));
        }
    }
    private void initializeParameters() {
        mUser = new TinDogUser();
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
                    updateObjectListAccordingToDistance();
                }
                return false;
            }
        });

        Uri imageUri = Uri.fromFile(new File("//android_asset/magnifying_glass.png"));
        Picasso.with(getContext())
                .load(imageUri)
                .placeholder(mImageViewMagnifyingGlass.getDrawable()) //inspired by: https://github.com/square/picasso/issues/257
                //.error(R.drawable.ic_image_not_available)
                .into(mImageViewMagnifyingGlass);

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
            mDogsListRecycleViewAdapter.setSelectedProfile(mSelectedProfileIndex);
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            if (mFamiliesListRecycleViewAdapter==null) mFamiliesListRecycleViewAdapter = new FamiliesListRecycleViewAdapter(getContext(), this, null);
            mRecyclerViewProfileSelection.setAdapter(mFamiliesListRecycleViewAdapter);
            mFamiliesListRecycleViewAdapter.setSelectedProfile(mSelectedProfileIndex);
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            if (mFoundationsListRecycleViewAdapter==null) mFoundationsListRecycleViewAdapter = new FoundationsListRecycleViewAdapter(getContext(), this, null);
            mRecyclerViewProfileSelection.setAdapter(mFoundationsListRecycleViewAdapter);
            mFoundationsListRecycleViewAdapter.setSelectedProfile(mSelectedProfileIndex);
        }

        mRecyclerViewProfileSelection.scrollToPosition(mProfileSelectionRecyclerViewPosition);
        mRecyclerViewProfileSelection.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mProfileSelectionRecyclerViewPosition = Utilities.getLinearRecyclerViewPosition(mRecyclerViewProfileSelection);
                //TODO: check if this is the same as dy
                onSearchScreenOperationsHandler.onProfileSelectionListLayoutCalculated(mProfileSelectionRecyclerViewPosition);
            }
        });
    }
    private void updateRecyclerView() {

        if (mProfileType.equals(getString(R.string.dog_profile)) && mDogsListRecycleViewAdapter!=null) {
            mDogsListRecycleViewAdapter.setContents(mDogsAtDistance);
            mUpdatedRecyclerView = true;
        }
        else if (mProfileType.equals(getString(R.string.family_profile)) && mFamiliesListRecycleViewAdapter!=null) {
            mFamiliesListRecycleViewAdapter.setContents(mFamiliesAtDistance);
            mUpdatedRecyclerView = true;
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile)) && mFoundationsListRecycleViewAdapter!=null) {
            mFoundationsListRecycleViewAdapter.setContents(mFoundationsAtDistance);
            mUpdatedRecyclerView = true;
        }

        if (mRecyclerViewProfileSelection!=null) {
            mRecyclerViewProfileSelection.scrollToPosition(mProfileSelectionRecyclerViewPosition);
        }

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
        if (getContext()==null) return;
        if (mImageSyncAsyncTaskLoader!=null) {
            mImageSyncAsyncTaskLoader.stopUpdatingImagesForObjects();
            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(LIST_MAIN_IMAGES_SYNC_LOADER);
        }
    }
    private void createFakeDogsForTesting() {
        //*********Special code designed to create dogs near the user, used for testing purposes only************
        onSearchScreenOperationsHandler.onUserLocationFound(mUserLatitude, mUserLongitude);
    }
    public void updateProfilesListParameters(int selectedProfileIndex, int recyclerViewPosition) {
        if (getContext()==null) return;
        mSelectedProfileIndex = selectedProfileIndex;
        if (mProfileType.equals(getString(R.string.dog_profile))) {
            if (mDogsListRecycleViewAdapter!=null) {
                mDogsListRecycleViewAdapter.setSelectedProfile(mSelectedProfileIndex);
            }
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            if (mFamiliesListRecycleViewAdapter!=null) {
                mFamiliesListRecycleViewAdapter.setSelectedProfile(mSelectedProfileIndex);
            }
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            if (mFoundationsListRecycleViewAdapter!=null) {
                mFoundationsListRecycleViewAdapter.setSelectedProfile(mSelectedProfileIndex);
            }
        }

        mProfileSelectionRecyclerViewPosition = recyclerViewPosition;
        mRecyclerViewProfileSelection.scrollToPosition(mProfileSelectionRecyclerViewPosition);
    }
    void updateObjectsList(Object objectsList) {
        if(getContext()==null) return;

        if (mProfileType.equals(getString(R.string.dog_profile))) {
            mDogsList = (List<Dog>) objectsList;
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            mFamiliesList = (List<Family>) objectsList;
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            mFoundationsList = (List<Foundation>) objectsList;
        }
    }
    private void stopListeningForLocation() {
        if (mLocationManager!=null) {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
            mLocationManager = null;
        }
        if (mLocationListener != null) mLocationListener = null;
    }
    void updateObjectListAccordingToDistance() {
        if(getContext()==null) return;

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


    //View click listeners
    @OnClick(R.id.search_screen_show_in_map_button) public void onShowInMapButtonClick() {
        if (mFoundResults) {

            Intent intent = new Intent(getContext(), MapActivity.class);
            if (mDogsAtDistance!=null && mProfileType.equals(getString(R.string.dog_profile))) {
                intent.putParcelableArrayListExtra(getString(R.string.search_results_dogs_at_distance_list), new ArrayList<>(mDogsAtDistance));
                startActivity(intent);
            }
            else if (mFamiliesAtDistance!=null && mProfileType.equals(getString(R.string.family_profile))) {
                intent.putParcelableArrayListExtra(getString(R.string.search_results_families_at_distance_list), new ArrayList<>(mFamiliesAtDistance));
                startActivity(intent);
            }
            else if (mFoundationsAtDistance!=null && mProfileType.equals(getString(R.string.foundation_profile))) {
                intent.putParcelableArrayListExtra(getString(R.string.search_results_foundations_at_distance_list), new ArrayList<>(mFoundationsAtDistance));
                startActivity(intent);
            }
        }
        else {
            Toast.makeText(getContext(), R.string.please_wait_while_results_loaded, Toast.LENGTH_SHORT).show();
        }
    }
    @OnClick(R.id.search_screen_magnifying_glass_image) public void onMagnifyingGlassButtonClick() {
        onSearchScreenOperationsHandler.onRequestListsRefresh();
    }


    //Communication with other classes:

    //Communication with RecyclerView adapters
    @Override public void onDogsListItemClick(int clickedItemIndex) {
        onSearchScreenOperationsHandler.onProfileSelected(clickedItemIndex);
        updateProfilesListParameters(clickedItemIndex, mProfileSelectionRecyclerViewPosition);
    }
    @Override public void onFamiliesListItemClick(int clickedItemIndex) {
        onSearchScreenOperationsHandler.onProfileSelected(clickedItemIndex);
        updateProfilesListParameters(clickedItemIndex, mProfileSelectionRecyclerViewPosition);
    }
    @Override public void onFoundationsListItemClick(int clickedItemIndex) {
        onSearchScreenOperationsHandler.onProfileSelected(clickedItemIndex);
        updateProfilesListParameters(clickedItemIndex, mProfileSelectionRecyclerViewPosition);
    }

    //Communication with Location handler
    @Override public void onLocalCoordinatesFound(double longitude, double latitude) {
        if (getContext()==null) return; //Prevents the code from continuing to work with a null context if the user exited the fragment too fast

        mUserLongitude = longitude;
        mUserLatitude = latitude;
        Utilities.setAppPreferenceUserLongitude(getContext(), longitude);
        Utilities.setAppPreferenceUserLatitude(getContext(), latitude);

        createFakeDogsForTesting(); //TODO: ***********Remove this in regular app

        if (!(mUserLongitude == 0.0 && mUserLatitude == 0.0) && mLocationManager!=null) {
            stopListeningForLocation();
        }

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
        void onRequestListsRefresh();
        void onUserLocationFound(double latitude, double longitude);
        void onProfileSelectionListLayoutCalculated(int recyclerViewPosition);
    }
}
