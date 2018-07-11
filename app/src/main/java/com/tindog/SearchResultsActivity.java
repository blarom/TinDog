package com.tindog;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tindog.resources.SharedMethods;

import java.util.Arrays;
import java.util.List;

public class SearchResultsActivity extends AppCompatActivity implements
        SearchScreenFragment.OnProfileSelectedListener {

    private static final String DEBUG_TAG = "TinDog Search Results";
    private FragmentManager mFragmentManager;
    private Boolean mActivatedDetailFragment = false;
    private DogProfileFragment mDogProfileFragment;
    private FamilyProfileFragment mFamilyProfileFragment;
    private FoundationProfileFragment mFoundationProfileFragment;
    private SearchScreenFragment mSearchScreenFragment;
    private FirebaseUser mCurrentFirebaseUser;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseAuth mFirebaseAuth;


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        initializeParameters();

        if(savedInstanceState == null) setFragmentLayouts(getString(R.string.dog_profile), 0); //Initializing with defaults
    }
    @Override public void onStart() {
        super.onStart();
        setupFirebaseAuthentication();
    }
    @Override protected void onStop() {
        super.onStop();
        cleanUpListeners();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        removeListenersAndHandlers();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SharedMethods.FIREBASE_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                mSearchScreenFragment.reloadDataAfterSuccessfulSignIn();
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_results_menu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        Intent intent;
        switch (itemThatWasClickedId) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.action_edit_my_family_profile:
                intent = new Intent(this, UpdateMyFamilyActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            case R.id.action_edit_preferences:
                intent = new Intent(this, PreferencesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            case R.id.action_signout:
                if (mCurrentFirebaseUser!=null) mFirebaseAuth.signOut();
                SharedMethods.setAppPreferenceSignInRequestState(getApplicationContext(), false);
                return true;
            case R.id.action_signin:
                SharedMethods.setAppPreferenceSignInRequestState(getApplicationContext(), true);
                showSignInScreen();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override public void onBackPressed() {

        //inspired by: https://stackoverflow.com/questions/5448653/how-to-implement-onbackpressed-in-fragments
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
        }
    }


    //Functionality methods
    private void initializeParameters() {
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFragmentManager = getSupportFragmentManager();
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.profile_finder);
        }
    }
    private void setFragmentLayouts(String profile, int selectedProfileIndex) {
        if (SharedMethods.getSmallestWidth(this) < getResources().getInteger(R.integer.tablet_smallest_width_threshold)) {
            if (!mActivatedDetailFragment) setFragmentInPlaceholder("search_screen", R.id.master_fragment_container, selectedProfileIndex);
            else setFragmentInPlaceholder(profile, R.id.master_fragment_container, selectedProfileIndex);
        } else {
            setFragmentInPlaceholder("search_screen", R.id.master_fragment_container, selectedProfileIndex);
            setFragmentInPlaceholder(profile, R.id.detail_fragment_container, selectedProfileIndex);
        }
    }
    private void setFragmentInPlaceholder(String fragmentIdentifier, int placeholderId, int selectedProfileIndex) {

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        Bundle bundle = new Bundle();

        if (fragmentIdentifier.equals("search_screen")) {
            if (mSearchScreenFragment==null) mSearchScreenFragment = new SearchScreenFragment();
            //bundle.putParcelable(Statics.STEP_DETAILS_PARCEL, mSelectedRecipeSteps.get(mSelectedStepIndex));
            //bundle.putParcelable(Statics.RECIPE_DETAILS_PARCEL, mSelectedRecipe);
            //bundle.putInt(Statics.CURRENT_RECIPE_STEP_INDEX, mSelectedStepIndex);
            //bundle.putInt(Statics.CURRENT_RECIPE_STEP_COUNT, mSelectedRecipeSteps.size());
            //bundle.putLong(Statics.VIDEO_PLAYER_CURRENT_PROGRESS, mVideoPlayerProgress);
            //bundle.putBoolean(Statics.VIDEO_PLAYER_PLAY_STATE, mVideoPlayerPlaybackState);
            mSearchScreenFragment.setArguments(bundle);

            fragmentTransaction.replace(placeholderId, mSearchScreenFragment);
        }
        else if (fragmentIdentifier.equals(getString(R.string.dog_profile))) {
            mDogProfileFragment = new DogProfileFragment();
            bundle.putInt(getString(R.string.selected_profile_index), selectedProfileIndex);
            //bundle.putParcelable(Statics.STEP_DETAILS_PARCEL, mSelectedRecipeSteps.get(mSelectedStepIndex));
            //bundle.putParcelable(Statics.RECIPE_DETAILS_PARCEL, mSelectedRecipe);
            //bundle.putInt(Statics.CURRENT_RECIPE_STEP_INDEX, mSelectedStepIndex);
            //bundle.putInt(Statics.CURRENT_RECIPE_STEP_COUNT, mSelectedRecipeSteps.size());
            //bundle.putLong(Statics.VIDEO_PLAYER_CURRENT_PROGRESS, mVideoPlayerProgress);
            //bundle.putBoolean(Statics.VIDEO_PLAYER_PLAY_STATE, mVideoPlayerPlaybackState);

            mDogProfileFragment.setArguments(bundle);
            fragmentTransaction.replace(placeholderId, mDogProfileFragment);
        }
        else if (fragmentIdentifier.equals(getString(R.string.family_profile))) {
            mFamilyProfileFragment = new FamilyProfileFragment();
            bundle.putInt(getString(R.string.selected_profile_index), selectedProfileIndex);
            mFamilyProfileFragment.setArguments(bundle);
            fragmentTransaction.replace(placeholderId, mFamilyProfileFragment);
        }
        else if (fragmentIdentifier.equals(getString(R.string.foundation_profile))) {
            mFoundationProfileFragment = new FoundationProfileFragment();
            bundle.putInt(getString(R.string.selected_profile_index), selectedProfileIndex);
            mFoundationProfileFragment.setArguments(bundle);
            fragmentTransaction.replace(placeholderId, mFoundationProfileFragment);
        }

        fragmentTransaction.commit();
    }
    private void setupFirebaseAuthentication() {
        // Check if user is signed in (non-null) and update UI accordingly.
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                mCurrentFirebaseUser = firebaseAuth.getCurrentUser();
                if (mCurrentFirebaseUser != null) {
                    // TinDogUser is signed in
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_in:" + mCurrentFirebaseUser.getUid());
                } else {
                    // TinDogUser is signed out
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_out");
                    if (SharedMethods.getAppPreferenceSignInRequestState(getApplicationContext())) showSignInScreen();
                }
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private void cleanUpListeners() {
        if (mFirebaseAuth!=null) mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }
    private void showSignInScreen() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                SharedMethods.FIREBASE_SIGN_IN);
    }
    private void removeListenersAndHandlers() {

    }


    //Communication with other activities/fragments
    @Override public void onProfileSelected(String profile, int selectedProfileIndex) {
        mActivatedDetailFragment = true;
        setFragmentLayouts(profile, selectedProfileIndex);
    }
}
