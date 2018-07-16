package com.tindog;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ProgressBar;
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
import com.tindog.resources.TinDogLocationListener;

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
        TinDogLocationListener.LocationListenerHandler {

    //regionParameters
    private static final String DEBUG_TAG = "TinDog Search Screen";
    @BindView(R.id.search_screen_magnifying_glass_image) ImageView mImageViewMagnifyingGlass;
    @BindView(R.id.search_screen_loading_indicator) ProgressBar mProgressBarLoadingIndicator;
    @BindView(R.id.search_screen_profile_selection_recycler_view) RecyclerView mRecyclerViewProfileSelection;
    @BindView(R.id.search_screen_distance_edittext) EditText mEditTextDistance;
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
    private int mFirebaseImageQueryIndex;
    private int mFirebaseFamilyImageQueryIndex;
    private int mFirebaseFoundationImageQueryIndex;
    private List<Dog> mDogsAtDistance;
    private List<Family> mFamiliesAtDistance;
    private List<Foundation> mFoundationsAtDistance;
    private boolean[] mSyncedImagesChecker;
    private boolean mSwitchedTabs;
    private String mProfileType;
    private String mCurrentImage;
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
        getListsFromFirebase();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_screen, container, false);

        initializeViews(rootView);
        setupRecyclerViews();
        showLoadingIndicator();
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
        if (getArguments() != null) {
            mProfileType = getArguments().getString(getString(R.string.profile_type));
        }
    }
    private void initializeParameters() {
        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        mUser = new TinDogUser();
        mFirebaseDbReference = firebaseDb.getReference();
        mFirebaseDao = new FirebaseDao(getContext(), this);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentFirebaseUser = mFirebaseAuth.getCurrentUser();
        hasLocationPermissions = checkLocationPermission();
        mSwitchedTabs = false;
        mDistance = 0;

        mUserLongitude = 0.0;
        mUserLatitude = 0.0;
        if (getContext()!=null) {
            mUserLongitude = SharedMethods.getAppPreferenceUserLongitude(getContext());
            mUserLatitude = SharedMethods.getAppPreferenceUserLatitude(getContext());
        }
    }
    private void initializeViews(View rootView) {

        mBinding = ButterKnife.bind(this, rootView);

        mEditTextDistance.setText("100");
        mEditTextDistance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    getObjectsAtDistance();
                }
                return false;
            }
        });

        mImageViewMagnifyingGlass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getObjectsAtDistance();
            }
        });
    }
    private void showLoadingIndicator() {
        if (mUserLatitude == 0.0 && mUserLongitude == 0.0) {
            if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.VISIBLE);
        }
    }
    private void hideLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.INVISIBLE);
    }
    private void startListeningForUserLocation() {
        if (getContext()!=null) {
            mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            mLocationListener = new TinDogLocationListener(getContext(), this);
            if (mLocationManager!=null && checkLocationPermission()) {
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
            mFirebaseDao.getFullObjectsListFromFirebaseDb(new Dog());
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            mFirebaseDao.getFullObjectsListFromFirebaseDb(new Family());
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            mFirebaseDao.getFullObjectsListFromFirebaseDb(new Foundation());
        }
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
    }
    private boolean checkLocationPermission() {
        if (getContext()!=null && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        else return false;
    }


    //Object manipulation methods
    private void updateObjectListAccordingToDistance() {
        getObjectsAtDistance();
        updateImagesForObjectsAtDistance();
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
    private void updateImagesForObjectsAtDistance() {

        mCurrentImage = "mainImage";
        mSyncedImagesChecker = new boolean[]{false, false, false, false, false, false};
        mFirebaseImageQueryIndex = 0;

        if (mProfileType.equals(getString(R.string.dog_profile))) {
            if (mDogsAtDistance!=null && mDogsAtDistance.size()!=0)
                SharedMethods.updateImageFromFirebaseIfRelevant(mDogsAtDistance.get(0), mCurrentImage, mFirebaseDao);
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            if (mFamiliesAtDistance!=null && mFamiliesAtDistance.size()!=0)
                SharedMethods.updateImageFromFirebaseIfRelevant(mFamiliesAtDistance.get(0), mCurrentImage, mFirebaseDao);
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            if (mFoundationsAtDistance!=null && mFoundationsAtDistance.size()!=0)
                SharedMethods.updateImageFromFirebaseIfRelevant(mFoundationsAtDistance.get(0), mCurrentImage, mFirebaseDao);
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
    private void syncNextImage(String currentImageName) {

        //Requesting the next image to sync, or if all the images are synced then go to the next element in the list to update its images
        if (mProfileType.equals(getString(R.string.dog_profile))) {
            if (mFirebaseImageQueryIndex == mDogsAtDistance.size()) return;
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            if (mFirebaseImageQueryIndex == mFamiliesAtDistance.size()) return;
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            if (mFirebaseImageQueryIndex == mFoundationsAtDistance.size()) return;
        }

        switch (currentImageName) {
            case "mainImage": {
                mCurrentImage = "image1";
                updateImageFromFirebase();
                break;
            }
            case "image1": {
                mCurrentImage = "image2";
                updateImageFromFirebase();
                break;
            }
            case "image2": {
                mCurrentImage = "image3";
                updateImageFromFirebase();
                break;
            }
            case "image3": {
                mCurrentImage = "image4";
                updateImageFromFirebase();
                break;
            }
            case "image4": {
                mCurrentImage = "image5";
                updateImageFromFirebase();
                break;
            }
            case "image5": {
                mCurrentImage = "mainImage";
                mFirebaseImageQueryIndex++;

                if (mProfileType.equals(getString(R.string.dog_profile))) {
                    if (mFirebaseImageQueryIndex == mDogsAtDistance.size()) return;
                }
                else if (mProfileType.equals(getString(R.string.family_profile))) {
                    if (mFirebaseImageQueryIndex == mFamiliesAtDistance.size()) return;
                }
                else if (mProfileType.equals(getString(R.string.foundation_profile))) {
                    if (mFirebaseImageQueryIndex == mFoundationsAtDistance.size()) return;
                }
                updateImageFromFirebase();
                break;
            }
        }

    }
    private void updateImageFromFirebase() {
        if (mProfileType.equals(getString(R.string.dog_profile))) {
            SharedMethods.updateImageFromFirebaseIfRelevant(mDogsAtDistance.get(mFirebaseImageQueryIndex), mCurrentImage, mFirebaseDao);
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            SharedMethods.updateImageFromFirebaseIfRelevant(mFamiliesAtDistance.get(mFirebaseImageQueryIndex), mCurrentImage, mFirebaseDao);
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            SharedMethods.updateImageFromFirebaseIfRelevant(mFoundationsAtDistance.get(mFirebaseImageQueryIndex), mCurrentImage, mFirebaseDao);
        }
    }
    private void syncImageForCurrentObject(String imageName, Uri imageUri) {
        if (getContext()==null) return;
        if (mProfileType.equals(getString(R.string.dog_profile))) {
            if (mFirebaseImageQueryIndex == mDogsAtDistance.size()) return;
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            if (mFirebaseImageQueryIndex == mFamiliesAtDistance.size()) return;
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            if (mFirebaseImageQueryIndex == mFoundationsAtDistance.size()) return;
        }

        String imagesDirectory = "";
        Object listElement = null;
        if (mProfileType.equals(getString(R.string.dog_profile))) {
            imagesDirectory = getContext().getFilesDir()+"/dogs/"+ mDogsAtDistance.get(mFirebaseImageQueryIndex).getUI()+"/images/";
            listElement = mDogsAtDistance.get(mFirebaseImageQueryIndex);
        }
        else if (mProfileType.equals(getString(R.string.family_profile))) {
            imagesDirectory = getContext().getFilesDir()+"/families/"+ mFamiliesAtDistance.get(mFirebaseImageQueryIndex).getUI()+"/images/";
            listElement = mFamiliesAtDistance.get(mFirebaseImageQueryIndex);
        }
        else if (mProfileType.equals(getString(R.string.foundation_profile))) {
            imagesDirectory = getContext().getFilesDir()+"/foundations/"+ mFoundationsAtDistance.get(mFirebaseImageQueryIndex).getUI()+"/images/";
            listElement = mFoundationsAtDistance.get(mFirebaseImageQueryIndex);
        }

        SharedMethods.synchronizeImageOnAllDevices(listElement, mFirebaseDao, imagesDirectory, imageName, imageUri);
    }


    //Location methods
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
                    city = dog.getCt();
                    if (TextUtils.isEmpty(city)) dogsNearby.add(dog);
                    else {
                        address = SharedMethods.getAddressFromCity(getContext(), dog.getCt());
                        if (isNearby(address, distanceMeters) && isInCountry(address, dog.getCn())) dogsNearby.add(dog);
                    }
                }
                return dogsNearby;
            }
            else if (objectsList.get(0) instanceof Family) {
                List<Family> familiesNearby = new ArrayList<>();
                for (int i=0; i<objectsList.size(); i++) {
                    Family family = (Family) objectsList.get(i);
                    city = family.getCt();
                    if (TextUtils.isEmpty(city)) familiesNearby.add(family);
                    else {
                        address = SharedMethods.getAddressFromCity(getContext(), family.getCt());
                        if (isNearby(address, distanceMeters) && isInCountry(address, family.getCn())) familiesNearby.add(family);
                    }
                }
                return familiesNearby;
            }
            else if (objectsList.get(0) instanceof Foundation) {
                List<Foundation> foundationsNearby = new ArrayList<>();
                for (int i=0; i<objectsList.size(); i++) {
                    Foundation foundation = (Foundation) objectsList.get(i);
                    city = foundation.getCt();
                    if (TextUtils.isEmpty(city)) foundationsNearby.add(foundation);
                    else {
                        address = SharedMethods.getAddressFromCity(getContext(), foundation.getCt());
                        if (isNearby(address, distanceMeters) && isInCountry(address, foundation.getCn())) foundationsNearby.add(foundation);
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
    private boolean isInCountry(Address address, String objectCountry) {
        if (mUser.getLC()) {
            String code = address.getCountryCode();
            Locale locale = new Locale("", code);
            return locale.getDisplayCountry().equals(objectCountry);
        }
        else return true;
    }


    //Communication with other activities/fragments:

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
        updateObjectListAccordingToDistance();
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
        if (getContext()==null) return; //Prevents the code from continuing to work with a null context if the user exited the fragment too fast
        mFamiliesList = familiesList;
        updateObjectListAccordingToDistance();
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {
        if (getContext()==null) return; //Prevents the code from continuing to work with a null context if the user exited the fragment too fast
        mFoundationsList = foundationsList;
        updateObjectListAccordingToDistance();
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
    @Override public void onImageAvailable(Uri imageUri, String currentImageName) {
        if (getContext()==null) return; //Prevents the code from continuing to work with a null context if the user exited the fragment too fast
        syncImageForCurrentObject(currentImageName, imageUri);
        if (currentImageName.equals("mainImage")) updateRecyclerView();
        syncNextImage(currentImageName);
    }
    @Override public void onImageUploaded(List<String> uploadTimes) {

    }

    //Communication with Location handler
    @Override public void onLocalCoordinatesFound(double longitude, double latitude) {
        mUserLongitude = longitude;
        mUserLatitude = latitude;
        SharedMethods.setAppPreferenceUserLongitude(getContext(), longitude);
        SharedMethods.setAppPreferenceUserLatitude(getContext(), latitude);
        if (mUserLongitude != 0.0 && mUserLatitude != 0.0 && mLocationManager!=null) {
            hideLoadingIndicator();
            mLocationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
            mLocationManager = null;
        }
        if (getContext()==null) return; //Prevents the code from continuing to work with a null context if the user exited the fragment too fast
        updateObjectListAccordingToDistance();
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
