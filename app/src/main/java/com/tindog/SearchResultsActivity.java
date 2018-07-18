package com.tindog;

import android.content.Intent;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.Foundation;
import com.tindog.resources.SharedMethods;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SearchResultsActivity extends AppCompatActivity implements
        SearchScreenFragment.OnSearchScreenOperationsHandler,
        DogProfileFragment.OnDogProfileFragmentOperationsHandler,
        FamilyProfileFragment.OnFamilyProfileFragmentOperationsHandler,
        FoundationProfileFragment.OnFoundationProfileFragmentOperationsHandler {

    @BindView(R.id.master_fragment_container) FrameLayout mMasterFragmentContainer;
    @BindView(R.id.profiles_pager) ViewPager mPager;
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
    private PagerAdapter mPagerAdapter;
    private Unbinder mBinding;
    private List<Dog> mDogs;
    private List<Family> mFamilies;
    private List<Foundation> mFoundations;
    private String mProfileType;
    private int mStoredImagesRecyclerViewPosition;
    private int mSelectedProfileIndex;


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        getExtras();
        initializeParameters();
        if(savedInstanceState == null) setFragmentLayouts(0);
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
        mBinding.unbind();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SharedMethods.FIREBASE_SIGN_IN_KEY) {
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
    @Override public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putInt(getString(R.string.search_results_profile_fragment_rv_position), mStoredImagesRecyclerViewPosition);
        outState.putInt(getString(R.string.search_results_selected_profile), mSelectedProfileIndex);
        outState.putBoolean(getString(R.string.search_results_activated_detail_fragment), mActivatedDetailFragment);
        super.onSaveInstanceState(outState, outPersistentState);
    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mActivatedDetailFragment = savedInstanceState.getBoolean(getString(R.string.search_results_activated_detail_fragment), false);
        if (mActivatedDetailFragment && mPager !=null) {
            mSelectedProfileIndex = savedInstanceState.getInt(getString(R.string.search_results_selected_profile));
            setFragmentLayouts(mSelectedProfileIndex);
        }
        mStoredImagesRecyclerViewPosition = savedInstanceState.getInt(getString(R.string.search_results_profile_fragment_rv_position));
    }

    //Functionality methods
    private void getExtras() {
        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.profile_type))) {
            mProfileType = intent.getStringExtra(getString(R.string.profile_type));
        }
    }
    private void initializeParameters() {
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFragmentManager = getSupportFragmentManager();
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.profile_finder);
        }

        mBinding =  ButterKnife.bind(this);
    }
    private void setFragmentLayouts(int selectedProfileIndex) {

        if (SharedMethods.getSmallestWidth(this) < getResources().getInteger(R.integer.tablet_smallest_width_threshold)) {
            if (!mActivatedDetailFragment) {
                setupSearchScreenFragment();
                removePager();
                mMasterFragmentContainer.setVisibility(View.VISIBLE);
                mPager.setVisibility(View.GONE);
            }
            else {
                setupProfilesPager(selectedProfileIndex);
                removeSearchScreenFragment();
                mMasterFragmentContainer.setVisibility(View.GONE);
                mPager.setVisibility(View.VISIBLE);
            }
        } else {
            setupSearchScreenFragment();
            setupProfilesPager(selectedProfileIndex);
            mMasterFragmentContainer.setVisibility(View.VISIBLE);
            mPager.setVisibility(View.VISIBLE);
        }
    }
    private void setupSearchScreenFragment() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        if (mSearchScreenFragment==null) mSearchScreenFragment = new SearchScreenFragment();
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.profile_type), mProfileType);
        mSearchScreenFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.master_fragment_container, mSearchScreenFragment);
        fragmentTransaction.commit();
    }
    private void setupProfilesPager(int selectedProfileIndex) {
        mPagerAdapter = new ProfilesPagerAdapter(mFragmentManager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(selectedProfileIndex);
    }
    private void removeSearchScreenFragment() {
        mSearchScreenFragment = null;
    }
    private void removePager() {
        mPagerAdapter = null;
        mPager.setAdapter(null);
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
    private void showSignInScreen() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                SharedMethods.FIREBASE_SIGN_IN_KEY);
    }
    private class ProfilesPagerAdapter extends FragmentStatePagerAdapter {

        ProfilesPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.profile_type), mProfileType);

            if (mProfileType.equals(getString(R.string.dog_profile))) {
                bundle.putParcelable(getString(R.string.profile_parcelable), mDogs.get(position));
                mDogProfileFragment = new DogProfileFragment();
                mDogProfileFragment.setArguments(bundle);
                mDogProfileFragment.setImagesRecyclerViewPosition(mStoredImagesRecyclerViewPosition);
                return mDogProfileFragment;
            }
            else if (mProfileType.equals(getString(R.string.family_profile))) {
                bundle.putParcelable(getString(R.string.profile_parcelable), mFamilies.get(position));
                mFamilyProfileFragment = new FamilyProfileFragment();
                mFamilyProfileFragment.setArguments(bundle);
                mFamilyProfileFragment.setImagesRecyclerViewPosition(mStoredImagesRecyclerViewPosition);
                return mFamilyProfileFragment;
            }
            else if (mProfileType.equals(getString(R.string.foundation_profile))) {
                bundle.putParcelable(getString(R.string.profile_parcelable), mFoundations.get(position));
                mFoundationProfileFragment = new FoundationProfileFragment();
                mFoundationProfileFragment.setArguments(bundle);
                mFoundationProfileFragment.setImagesRecyclerViewPosition(mStoredImagesRecyclerViewPosition);
                return mFoundationProfileFragment;
            }

            mSelectedProfileIndex = position;
            return new DogProfileFragment();
        }

        @Override
        public int getCount() {
            if (mProfileType.equals(getString(R.string.dog_profile))) {
                return mDogs.size();
            }
            else if (mProfileType.equals(getString(R.string.family_profile))) {
                return mFamilies.size();
            }
            else if (mProfileType.equals(getString(R.string.foundation_profile))) {
                return mFoundations.size();
            }
            else return 0;
        }
    }


    //Communication with other activities/fragments

    //Communication with Search Screen Fragment
    @Override public void onProfileSelected(int selectedProfileIndex) {
        mActivatedDetailFragment = true;
        mSelectedProfileIndex = selectedProfileIndex;
        setFragmentLayouts(mSelectedProfileIndex);
    }
    @Override public void onDogsFound(List<Dog> dogList) {
        mDogs = dogList;
    }
    @Override public void onFamiliesFound(List<Family> familyList) {
        mFamilies = familyList;
    }
    @Override public void onFoundationsFound(List<Foundation> foundationList) {
        mFoundations = foundationList;
    }

    //Communication with Profile fragments
    @Override public void onDogLayoutParametersCalculated(int imagesRecyclerViewPosition) {
        mStoredImagesRecyclerViewPosition = imagesRecyclerViewPosition;
    }
    @Override public void onFamilyLayoutParametersCalculated(int imagesRecyclerViewPosition) {
        mStoredImagesRecyclerViewPosition = imagesRecyclerViewPosition;
    }
    @Override public void onFoundationLayoutParametersCalculated(int imagesRecyclerViewPosition) {
        mStoredImagesRecyclerViewPosition = imagesRecyclerViewPosition;
    }


}
