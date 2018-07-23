package com.tindog;

//TODO: update the preference activity options with better dog characteristics
//TODO: restore recyclerview positions and parameters on resume in all activities/fragments
//TODO: search for dog profiles according to the user preferences
//TODO: populate map with knowledge pins
//TODO: consider adding swiperefreshlayout
//TODO: fix dogs list activity not refreshing search when returning to it
//TODO: Add option to delete pictures in dog/famiy/foundation profile updaters
//TODO: Fix failed to find image problem
//TODO: add set address from map pin functionality
//TODO: implement profile/image sharing from the profile fragments

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tindog.resources.SharedMethods;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static butterknife.internal.Utils.arrayOf;

public class TaskSelectionActivity extends AppCompatActivity {

    private static final String DEBUG_TAG = "Tindog Firebase";
    public static final int APP_PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 555;
    private static final int APP_PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 123;
    private static final String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentFirebaseUser;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private boolean hasStoragePermissions;
    private boolean hasLocationPermissions;
    @BindView(R.id.task_selection_find_dog) Button mButtonFindDog;
    @BindView(R.id.task_selection_find_family) Button mButtonFindFamily;
    @BindView(R.id.task_selection_find_foundation) Button mButtonFindFoundation;
    @BindView(R.id.task_selection_update_map) Button mButtonUpdateMap;
    private Unbinder mBinding;
    private InterstitialAd mInterstitialAd;


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_selection);

        setupInterstitialAd();


        mFirebaseAuth = FirebaseAuth.getInstance();
        mBinding =  ButterKnife.bind(this);

        mButtonFindDog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                }
                startSearchResultsActivity(getString(R.string.dog_profile));
            }
        });
        mButtonFindFamily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                }
                startSearchResultsActivity(getString(R.string.family_profile));
            }
        });
        mButtonFindFoundation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                }
                startSearchResultsActivity(getString(R.string.foundation_profile));
            }
        });
        mButtonUpdateMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToMapScreen();
            }
        });

        setupFirebaseAuthentication();
        hasStoragePermissions = checkStoragePermission();
        hasLocationPermissions = checkLocationPermission();
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

        if (requestCode == SharedMethods.FIREBASE_SIGN_IN_KEY) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.task_selection_menu, menu);
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
            case R.id.action_signout:
                if (mCurrentFirebaseUser!=null) mFirebaseAuth.signOut();
                SharedMethods.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), false);
                return true;
            case R.id.action_signin:
                SharedMethods.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                SharedMethods.showSignInScreen(TaskSelectionActivity.this);
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
            doThisIfFirstTimeUsingApp();
        }
    }


    //Functionality methods
    private void setupInterstitialAd() {
        MobileAds.initialize(this, ADMOB_APP_ID);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });
    }
    private void startSearchResultsActivity(String profileType) {
        Intent intent = new Intent(this, SearchResultsActivity.class);
        intent.putExtra(getString(R.string.profile_type), profileType);
        startActivity(intent);
    }
    private void goToMapScreen() {
        Intent intent = new Intent(this, MapActivity.class);
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
                    SharedMethods.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_in:" + mCurrentFirebaseUser.getUid());
                } else {
                    // TinDogUser is signed out
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_out");
                    //Showing the sign-in screen
                    if (SharedMethods.getAppPreferenceUserHasNotRefusedSignIn(getApplicationContext()))
                        SharedMethods.showSignInScreen(TaskSelectionActivity.this);
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
        if (mButtonFindDog!=null) mButtonFindDog.setOnClickListener(null);
        if (mButtonFindFamily!=null) mButtonFindFamily.setOnClickListener(null);
        if (mButtonFindFoundation!=null) mButtonFindFoundation.setOnClickListener(null);
        if (mButtonUpdateMap!=null) mButtonUpdateMap.setOnClickListener(null);
    }
    private void doThisIfFirstTimeUsingApp() {
        boolean firstTime = SharedMethods.getAppPreferenceFirstTimeUsingApp(this);
        if (firstTime) {
            Toast.makeText(this, R.string.first_time_preferences, Toast.LENGTH_SHORT).show();
            SharedMethods.setAppPreferenceFirstTimeUsingApp(this, false);
            Intent intent = new Intent(this, PreferencesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }



}
