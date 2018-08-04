package com.tindog.ui;

//Improvements to the app:
//Short term
//TODO: re-work the user interface, including flow change, using cards, etc..
//TODO: remove dummy data (this could also speed up the app, since geo calls are not done on the main thread)
//TODO: throw all geo calls to asynctasks to prevent stalling the app on the main thread
//TODO: add button that allows people interested in dogs to ask for the contact info for a family, and the family can reply with the info through the app/other means
//TODO: add a "who am I" edittext to edit family profile activity
//TODO: In the edit profile screen, there's both "save" & "done", once again, very confusing, what's the difference? I would switch "done" with "cancel"

////Long term
//TODO: I would remove the top brown banner in the loading screen, it's confusing (the user might think he needs to click something)
//TODO: There should be an "I don't care" option in the dog-I-would-like-to-have parameters (not everyone cares about the size etc.)
//TODO: move "show random dogs in widget" to preferences & remove it from menu
//TODO: search for dog profiles according to the user preferences
//TODO: add set address from map pin functionality
//TODO: add ability to save dog I liked into list, which includes "show random dogs in widget"
//TODO: Add option to delete pictures in dog/family/foundation profile updaters
//TODO: implement in-app messaging
//TODO: optional: consider adding swiperefreshlayout
//TODO: optional: add Google Places to augment user-defined markers on map (requires app funding)
//TODO: populate map with knowledge pins
//TODO: update the preference activity options with better dog characteristics
//TODO: make menu "sign in" Title change after menu has exited, not before

////Long term - from Udacity review
//TODO: ensure optimal content descriptions on all relevant views and similar parameters to improve the app's accessibility
//TODO: switch to a repository pattern for data storage, see https://commonsware.com/AndroidArch/previews/the-repository-pattern

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tindog.R;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.MapMarker;
import com.tindog.data.TinDogUser;
import com.tindog.resources.Utilities;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static butterknife.internal.Utils.arrayOf;

public class TaskSelectionActivity extends AppCompatActivity implements FirebaseDao.FirebaseOperationsHandler {

    //region Parameters
    private static final String DEBUG_TAG = "Tindog Firebase";
    public static final int APP_PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 555;
    private static final int APP_PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 123;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentFirebaseUser;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private boolean hasStoragePermissions;
    private boolean hasLocationPermissions;
    private Unbinder mBinding;
    private InterstitialAd mInterstitialAd;
    private Menu mMenu;
    private FirebaseDao mFirebaseDao;
    private TinDogUser mUser;
    private String mFirebaseUid;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_selection);

        setupInterstitialAds();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mBinding =  ButterKnife.bind(this);

        setupFirebaseAuthentication();
        hasStoragePermissions = checkStoragePermission();
        hasLocationPermissions = checkLocationPermission();
        if (hasStoragePermissions && hasLocationPermissions) checkIfUserHasATinDogUserProfile();
    }
    @Override protected void onResume() {
        super.onResume();
        mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        //Resetting the profile scroll positions
        Utilities.resetProfileScrollPositions(this);
    }
    @Override public void onStart() {
        super.onStart();
        setupFirebaseAuthentication();
    }
    @Override protected void onStop() {
        super.onStop();
        if (mFirebaseAuth!=null) mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        removeListeners();
        mBinding.unbind();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Utilities.FIREBASE_SIGN_IN_KEY) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (hasStoragePermissions && hasLocationPermissions) checkIfUserHasATinDogUserProfile();
                Utilities.updateSignInMenuItem(mMenu, this, true);
            } else {
                Utilities.updateSignInMenuItem(mMenu, this, false);
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.task_selection_menu, menu);
        mMenu = menu;
        if (mCurrentFirebaseUser==null) Utilities.updateSignInMenuItem(mMenu, this, false);
        else Utilities.updateSignInMenuItem(mMenu, this, true);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        Intent intent;
        switch (itemThatWasClickedId) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.action_edit_preferences:
                intent = new Intent(this, PreferencesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            case R.id.action_edit_my_family_profile:
                intent = new Intent(this, UpdateMyFamilyActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            case R.id.action_edit_my_foundation_profile:
                intent = new Intent(this, UpdateMyFoundationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            case R.id.action_edit_my_foundation_dogs:
                intent = new Intent(this, UpdateMyDogsListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            case R.id.action_signin:
                if (mCurrentFirebaseUser==null) {
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                    Utilities.showSignInScreen(TaskSelectionActivity.this);
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
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == APP_PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasStoragePermissions = true;

                Log.e(DEBUG_TAG, "Returned from WRITE_EXTERNAL_STORAGE permission request.");
            } else {
                Toast.makeText(this, R.string.no_permissions_terminating, Toast.LENGTH_SHORT).show();

                //Close app (inspired by: https://stackoverflow.com/questions/17719634/how-to-exit-an-android-app-using-code)
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
            }
            if (!hasLocationPermissions) checkLocationPermission();
        }
        else if (requestCode == APP_PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasLocationPermissions = true;

                Log.e(DEBUG_TAG, "Returned from ACCESS_FINE_LOCATION permission request.");
            } else {
                hasLocationPermissions = false;
            }

            checkIfUserHasATinDogUserProfile();
        }
    }


    //Functionality methods
    private void setupInterstitialAds() {
        MobileAds.initialize(this, Utilities.adMobAppId);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(Utilities.adUnitId);
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });
    }
    private void showInterstitialAd() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.");
        }
    }
    private void startSearchResultsActivity(String profileType) {
        Intent intent = new Intent(this, SearchResultsActivity.class);
        intent.putExtra(getString(R.string.profile_type), profileType);
        startActivity(intent);
    }
    private void setupFirebaseAuthentication() {
        // Check if user is signed in (non-null) and update UI accordingly.
        mCurrentFirebaseUser = mFirebaseAuth.getCurrentUser();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                mCurrentFirebaseUser = firebaseAuth.getCurrentUser();
                if (mCurrentFirebaseUser != null) {
                    // TinDogUser is signed in
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                    Utilities.setAppPreferenceFirstTimeUsingApp(getApplicationContext(), false);
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_in:" + mCurrentFirebaseUser.getUid());
                } else {
                    // TinDogUser is signed out
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_out");
                    //Showing the sign-in screen
                    boolean firstTime = Utilities.getAppPreferenceFirstTimeUsingApp(getApplicationContext());
                    if (!firstTime && Utilities.getAppPreferenceUserHasNotRefusedSignIn(getApplicationContext()))
                        Utilities.showSignInScreen(TaskSelectionActivity.this);
                }
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    public boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.e(DEBUG_TAG, "User has granted EXTERNAL_STORAGE permission");
                return true;
            } else {
                Log.e(DEBUG_TAG, "User has asked for EXTERNAL_STORAGE permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, APP_PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
                return false;
            }
        }
        else {
            Log.e(DEBUG_TAG,"User already has the permission");
            return true;
        }
    }
    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.e(DEBUG_TAG, "User has granted ACCESS_FINE_LOCATION permission");
            return true;
        } else {
            Log.e(DEBUG_TAG, "User has asked for ACCESS_FINE_LOCATION permission");

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, APP_PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, R.string.location_rationale, Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        APP_PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
            }
            return false;
        }
    }
    private void removeListeners() {
        mFirebaseDao.removeListeners();
    }
    private void checkIfUserHasATinDogUserProfile() {
        mFirebaseDao = new FirebaseDao(getBaseContext(), this);
        if (mCurrentFirebaseUser != null) {
            mFirebaseUid = mCurrentFirebaseUser.getUid();
            mUser = new TinDogUser();
            mUser.setUI(mFirebaseUid);
            mFirebaseDao.getUniqueObjectFromFirebaseDbOrCreateIt(mUser, false);
        }
    }
    private void createUserAndOpenPreferencesActivity() {

        mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mUser, true);

        //Since this is the first time that the user's TinDogUser profile was created, we send him/her to the Preferences activity

        Toast.makeText(this, R.string.first_time_preferences, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, PreferencesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }


    //View click listeners
    @OnClick(R.id.task_selection_find_dog) public void onFindDogButtonClick() {
        showInterstitialAd();
        startSearchResultsActivity(getString(R.string.dog_profile));
    }
    @OnClick(R.id.task_selection_find_family) public void onFindFamilyButtonClick() {
        showInterstitialAd();
        startSearchResultsActivity(getString(R.string.family_profile));
    }
    @OnClick(R.id.task_selection_find_foundation) public void onFindFoundationButtonClick() {
        showInterstitialAd();
        startSearchResultsActivity(getString(R.string.foundation_profile));
    }
    @OnClick(R.id.task_selection_update_map) public void onUpdateMapButtonClick() {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }


    //Communication with other classes:

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {

    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {

    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {

    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

        if (usersList.size()==1) {
            if (usersList.get(0) != null) {
                mUser = usersList.get(0);
            }
            else {
                createUserAndOpenPreferencesActivity();
            }
        }
        else if (usersList.size()>1) {
            mUser = usersList.get(0);
            Log.i(DEBUG_TAG, getString(R.string.warning_multiple_users));
        }
        else {
            mUser = new TinDogUser(mFirebaseUid);
            createUserAndOpenPreferencesActivity();
        }
    }
    @Override public void onMapMarkerListFound(List<MapMarker> markersList) {

    }
    @Override public void onImageAvailable(Uri imageUri, String imageName) {

    }
    @Override public void onImageUploaded(List<String> uploadTimes) {

    }
}
