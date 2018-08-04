package com.tindog.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
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
import com.tindog.services.WidgetUpdateJobIntentService;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SearchResultsActivity extends AppCompatActivity implements
        SearchScreenFragment.OnSearchScreenOperationsHandler,
        ViewPager.OnPageChangeListener,
        FirebaseDao.FirebaseOperationsHandler {

    //region Parameters
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
    private int mSelectedProfileIndex;
    private String mRequestedDogProfileUI;
    private String mRequestedFamilyProfileUI;
    private String mRequestedFoundationProfileUI;
    private Menu mMenu;
    private FirebaseDao mFirebaseDao;
    private List<Dog> mDogsList;
    private List<Family> mFamiliesList;
    private List<Foundation> mFoundationsList;
    private TinDogUser mUser;
    private String mNameFromFirebase;
    private String mEmailFromFirebase;
    private Uri mPhotoUriFromFirebase;
    private String mFirebaseUid;
    private int mProfileSelectionRecyclerViewPosition;
    private List<Dog> mDogsAtDistance;
    private List<Family> mFamiliesAtDistance;
    private List<Foundation> mFoundationsAtDistance;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        getExtras();
        initializeParameters();
        if (!Utilities.internetIsAvailable(this)) {
            Toast.makeText(this, R.string.no_internet_bad_results_warning, Toast.LENGTH_SHORT).show();
        }
    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mActivatedDetailFragment = savedInstanceState.getBoolean(getString(R.string.search_results_activated_detail_fragment), false);
        mDogsList = savedInstanceState.getParcelableArrayList((getString(R.string.search_results_dogs_list)));
        mFamiliesList = savedInstanceState.getParcelableArrayList((getString(R.string.search_results_families_list)));
        mFoundationsList = savedInstanceState.getParcelableArrayList((getString(R.string.search_results_foundations_list)));
        mDogsAtDistance = savedInstanceState.getParcelableArrayList(getString(R.string.search_results_dogs_at_distance));
        mFamiliesAtDistance = savedInstanceState.getParcelableArrayList(getString(R.string.search_results_families_at_distance));
        mFoundationsAtDistance = savedInstanceState.getParcelableArrayList(getString(R.string.search_results_foundations_at_distance));
        mProfileSelectionRecyclerViewPosition = savedInstanceState.getInt(getString(R.string.search_screen_fragment_rv_position));
        mSelectedProfileIndex = savedInstanceState.getInt(getString(R.string.search_results_selected_profile));
    }
    @Override public void onStart() {
        super.onStart();
        setupFirebaseAuthentication();
    }
    @Override protected void onResume() {
        super.onResume();
        setFragmentLayouts(mSelectedProfileIndex);
        getListsFromFirebase();
    }
    @Override protected void onStop() {
        super.onStop();
        if (mFirebaseAuth!=null) mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }
    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mPager!=null && mActivatedDetailFragment) {
            mSelectedProfileIndex = mPager.getCurrentItem();
            mPager.setAdapter(null);
        }

        if (mDogsAtDistance.size()==0) {
            String a="";
        }

        outState.putInt(getString(R.string.search_results_selected_profile), mSelectedProfileIndex);
        outState.putBoolean(getString(R.string.search_results_activated_detail_fragment), mActivatedDetailFragment);
        outState.putParcelableArrayList(getString(R.string.search_results_dogs_list), new ArrayList<>(mDogsList));
        outState.putParcelableArrayList(getString(R.string.search_results_families_list), new ArrayList<>(mFamiliesList));
        outState.putParcelableArrayList(getString(R.string.search_results_foundations_list), new ArrayList<>(mFoundationsList));
        outState.putParcelableArrayList(getString(R.string.search_results_dogs_at_distance), new ArrayList<>(mDogsAtDistance));
        outState.putParcelableArrayList(getString(R.string.search_results_families_at_distance), new ArrayList<>(mFamiliesAtDistance));
        outState.putParcelableArrayList(getString(R.string.search_results_foundations_at_distance), new ArrayList<>(mFoundationsAtDistance));
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        mBinding.unbind();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Utilities.FIREBASE_SIGN_IN_KEY) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                Utilities.updateSignInMenuItem(mMenu, this, true);
                reloadDataAfterSuccessfulSignIn();
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
        getMenuInflater().inflate(R.menu.search_results_menu, menu);
        mMenu = menu;
        if (mCurrentFirebaseUser==null) Utilities.updateSignInMenuItem(mMenu, this, false);
        else Utilities.updateSignInMenuItem(mMenu, this, true);

        //inspired by: https://developer.android.com/training/sharing/shareaction
        //MenuItem item = menu.findItem(R.id.action_share);
        //mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        Intent intent;
        switch (itemThatWasClickedId) {
            case android.R.id.home:
                onBackPressed();
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
            case R.id.action_show_in_widget:
                updateDogImageWidgetWithDogsList(mDogs);
                return true;
            case R.id.action_signin:
                if (mCurrentFirebaseUser==null) {
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                    Utilities.showSignInScreen(SearchResultsActivity.this);
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
    @Override public void onBackPressed() {

        //Returns to the SearchScreenFragment only if the user cliked on a profile in the SearchScreenFragment
        if (!mActivatedDetailFragment
                || !TextUtils.isEmpty(mRequestedDogProfileUI)
                || !TextUtils.isEmpty(mRequestedFamilyProfileUI)
                || !TextUtils.isEmpty(mRequestedFoundationProfileUI)) {
            if (mSearchScreenFragment!=null) {
                mSearchScreenFragment.stopImageSyncThread();
            }
            super.onBackPressed();
        } else {
            mActivatedDetailFragment = false;
            setFragmentLayouts(mSelectedProfileIndex);

        }
        Utilities.resetProfileScrollPositions(this);

    }


    //Functionality methods
    private void getExtras() {
        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.profile_type))) {
            mProfileType = intent.getStringExtra(getString(R.string.profile_type));
        }
        mRequestedDogProfileUI = "";
        if (intent.hasExtra(getString(R.string.requested_specific_dog_profile))) {
            //If the user requested a dog profile, the activity sends this request to the SearchScreenFragment,
            //which registers it as an automatic click on the relevant profile if it's available.
            //This in turn activates the profiles pager on the correct profile
            mRequestedDogProfileUI = intent.getStringExtra(getString(R.string.requested_specific_dog_profile));
        }
        mRequestedFamilyProfileUI = "";
        if (intent.hasExtra(getString(R.string.requested_specific_family_profile))) {
            //If the user requested a family profile, the activity sends this request to the SearchScreenFragment,
            //which registers it as an automatic click on the relevant profile if it's available.
            //This in turn activates the profiles pager on the correct profile
            mRequestedFoundationProfileUI = intent.getStringExtra(getString(R.string.requested_specific_family_profile));
        }
        mRequestedFoundationProfileUI = "";
        if (intent.hasExtra(getString(R.string.requested_specific_foundation_profile))) {
            //If the user requested a foundation profile, the activity sends this request to the SearchScreenFragment,
            //which registers it as an automatic click on the relevant profile if it's available.
            //This in turn activates the profiles pager on the correct profile
            mRequestedFoundationProfileUI = intent.getStringExtra(getString(R.string.requested_specific_foundation_profile));
        }
    }
    private void initializeParameters() {
        mDogsList = new ArrayList<>();
        mFamiliesList = new ArrayList<>();
        mFoundationsList = new ArrayList<>();
        mDogsAtDistance = new ArrayList<>();
        mFamiliesAtDistance = new ArrayList<>();
        mFoundationsAtDistance = new ArrayList<>();
        mUser = new TinDogUser();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDao = new FirebaseDao(this, this);
        mFragmentManager = getSupportFragmentManager();
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (mProfileType.equals(getString(R.string.dog_profile))) getSupportActionBar().setTitle(R.string.dog_finder);
            else if (mProfileType.equals(getString(R.string.family_profile))) getSupportActionBar().setTitle(R.string.family_finder);
            else if (mProfileType.equals(getString(R.string.foundation_profile))) getSupportActionBar().setTitle(R.string.foundation_finder);
            else getSupportActionBar().setTitle(R.string.profile_finder);
        }

        mBinding =  ButterKnife.bind(this);
        Utilities.hideSoftKeyboard(this);
    }
    private void setFragmentLayouts(int selectedProfileIndex) {

        mFragmentManager = getSupportFragmentManager();
        if (Utilities.getSmallestWidth(this) < getResources().getInteger(R.integer.tablet_smallest_width_threshold)) {
            setupSearchScreenFragment();
            if (!mActivatedDetailFragment) {
                removePager();
                showSearchScreenFragment();
                if (mMasterFragmentContainer!=null) mMasterFragmentContainer.setVisibility(View.VISIBLE);
                if (mPager!=null) mPager.setVisibility(View.GONE);
            }
            else {
                setupAndShowProfilesPager(selectedProfileIndex);
                if (mMasterFragmentContainer!=null) mMasterFragmentContainer.setVisibility(View.GONE);
                if (mPager!=null) mPager.setVisibility(View.VISIBLE);
            }
        } else {
            setupSearchScreenFragment();
            showSearchScreenFragment();
            setupAndShowProfilesPager(selectedProfileIndex);
            if (mMasterFragmentContainer!=null) mMasterFragmentContainer.setVisibility(View.VISIBLE);
            if (mPager!=null) mPager.setVisibility(View.VISIBLE);
        }

    }
    private void setupSearchScreenFragment() {
        if (mSearchScreenFragment==null) {
            mSearchScreenFragment = new SearchScreenFragment();

            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.profile_type), mProfileType);
            bundle.putInt(getString(R.string.selected_profile_index), mSelectedProfileIndex);
            bundle.putInt(getString(R.string.search_screen_fragment_rv_position), mProfileSelectionRecyclerViewPosition);
            bundle.putParcelableArrayList(getString(R.string.search_results_dogs_list), new ArrayList<>(mDogsList));
            bundle.putParcelableArrayList(getString(R.string.search_results_families_list), new ArrayList<>(mFamiliesList));
            bundle.putParcelableArrayList(getString(R.string.search_results_foundations_list), new ArrayList<>(mFoundationsList));
            bundle.putParcelableArrayList(getString(R.string.search_results_dogs_at_distance), new ArrayList<>(mDogsAtDistance));
            bundle.putParcelableArrayList(getString(R.string.search_results_families_at_distance), new ArrayList<>(mFamiliesAtDistance));
            bundle.putParcelableArrayList(getString(R.string.search_results_foundations_at_distance), new ArrayList<>(mFoundationsAtDistance));
            if (!TextUtils.isEmpty(mRequestedDogProfileUI))
                bundle.putString(getString(R.string.requested_specific_dog_profile), mRequestedDogProfileUI);
            else if (!TextUtils.isEmpty(mRequestedFamilyProfileUI))
                bundle.putString(getString(R.string.requested_specific_family_profile), mRequestedFamilyProfileUI);
            else if (!TextUtils.isEmpty(mRequestedFoundationProfileUI))
                bundle.putString(getString(R.string.requested_specific_foundation_profile), mRequestedFoundationProfileUI);

            mSearchScreenFragment.setArguments(bundle);
        }
        else {
            mSearchScreenFragment.updateProfilesListParameters(mSelectedProfileIndex, mProfileSelectionRecyclerViewPosition);
        }

    }
    private void showSearchScreenFragment() {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.master_fragment_container, mSearchScreenFragment);
        fragmentTransaction.commit();
    }
    private void setupAndShowProfilesPager(final int selectedProfileIndex) {
        mPagerAdapter = new ProfilesPagerAdapter(mFragmentManager);
        if (mPager!=null) {
            mPager.setAdapter(mPagerAdapter);
            mPager.addOnPageChangeListener(this);
            displaySelectedProfile(selectedProfileIndex);
        }
    }
    private void displaySelectedProfile(final int selectedProfileIndex) {

        mPager.setCurrentItem(selectedProfileIndex);
        //inspired by: https://stackoverflow.com/questions/19316729/android-viewpager-setcurrentitem-not-working-after-onresume
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                //Setting the current item again after a delay since all the fragment may not have yet been loaded

                if (mPagerAdapter!=null && mPager!=null) {
                    mPagerAdapter.notifyDataSetChanged();
                    mPager.setCurrentItem(selectedProfileIndex);
                }
            }
        });

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
                    if (Utilities.getAppPreferenceUserHasNotRefusedSignIn(getApplicationContext())) {
                        Utilities.showSignInScreen(SearchResultsActivity.this);
                    }
                }
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private class ProfilesPagerAdapter extends FragmentStatePagerAdapter {

        ProfilesPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (getCount() == 0) {
                Log.i(DEBUG_TAG, "Error in SearchResultsActivity/FragmentStatePagerAdapter - count is zero");
                return new DogProfileFragment();
            }

            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.profile_type), mProfileType);

            if (mProfileType.equals(getString(R.string.dog_profile))) {
                bundle.putParcelable(getString(R.string.profile_parcelable), mDogsAtDistance.get(position));
                mDogProfileFragment = new DogProfileFragment();
                mDogProfileFragment.setRetainInstance(false);
                mDogProfileFragment.setArguments(bundle);
                return mDogProfileFragment;
            }
            else if (mProfileType.equals(getString(R.string.family_profile))) {
                bundle.putParcelable(getString(R.string.profile_parcelable), mFamiliesAtDistance.get(position));
                mFamilyProfileFragment = new FamilyProfileFragment();
                mFamilyProfileFragment.setRetainInstance(false);
                mFamilyProfileFragment.setArguments(bundle);
                return mFamilyProfileFragment;
            }
            else if (mProfileType.equals(getString(R.string.foundation_profile))) {
                bundle.putParcelable(getString(R.string.profile_parcelable), mFoundationsAtDistance.get(position));
                mFoundationProfileFragment = new FoundationProfileFragment();
                mFoundationProfileFragment.setRetainInstance(false);
                mFoundationProfileFragment.setArguments(bundle);
                return mFoundationProfileFragment;
            }

            return new DogProfileFragment();
        }

        @Override
        public int getCount() {
            if (mProfileType.equals(getString(R.string.dog_profile))) {
                return (mDogsAtDistance!=null) ? mDogsAtDistance.size() : 0;
            }
            else if (mProfileType.equals(getString(R.string.family_profile))) {
                return (mFamiliesAtDistance!=null) ? mFamiliesAtDistance.size() : 0;
            }
            else if (mProfileType.equals(getString(R.string.foundation_profile))) {
                return (mFoundationsAtDistance!=null) ? mFoundationsAtDistance.size() : 0;
            }
            else return 0;
        }

    }
    private void updateDogImageWidgetWithDogsList(List<Dog> dogList) {
        if (dogList!=null && dogList.size()>0) {
            Intent intent = new Intent(this, WidgetUpdateJobIntentService.class);
            ArrayList<Dog> dogs = new ArrayList<>(mDogs);
            intent.putParcelableArrayListExtra(getString(R.string.intent_extra_dogs_list), dogs);
            intent.setAction(getString(R.string.action_update_widget_random_dog));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //startService(intent);
            WidgetUpdateJobIntentService.enqueueWork(this, intent);
        }
    }
    private void getListsFromFirebase() {

        if(mCurrentFirebaseUser==null) {
            Log.i(DEBUG_TAG, "Current Firebase user is null, cancelling database request. Did you sign in?");
            return;
        }

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
    private void reloadDataAfterSuccessfulSignIn() {
        getUserInfoFromFirebase();
        getListsFromFirebase();
    }


    //Communication with other classes

    //Communication with Search Screen Fragment
    @Override public void onProfileSelected(int selectedProfileIndex) {
        mActivatedDetailFragment = true;
        mSelectedProfileIndex = selectedProfileIndex;
        setFragmentLayouts(mSelectedProfileIndex);
    }
    @Override public void onDogsFound(List<Dog> dogList) {
        mDogsAtDistance = dogList;
    }
    @Override public void onFamiliesFound(List<Family> familyList) {
        mFamiliesAtDistance = familyList;
        if (mPagerAdapter!=null) mPagerAdapter.notifyDataSetChanged();
    }
    @Override public void onFoundationsFound(List<Foundation> foundationList) {
        mFoundationsAtDistance = foundationList;
        if (mPagerAdapter!=null) mPagerAdapter.notifyDataSetChanged();
    }
    @Override public void onRequestListsRefresh() {
        getListsFromFirebase();
    }
    @Override public void onUserLocationFound(double latitude, double longitude) {
        //*********Special code designed to create dogs near the user, used for testing purposes only************
        mFirebaseDao.populateFirebaseDbWithDummyData(this, latitude, longitude);
    }
    @Override public void onProfileSelectionListLayoutCalculated(int recyclerViewPosition) {
        mProfileSelectionRecyclerViewPosition = recyclerViewPosition;
    }

    //Communication with Pager
    @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }
    @Override public void onPageSelected(int position) {
        mSelectedProfileIndex = position;

        //Resetting the profile scroll positions
        Utilities.setAppPreferenceProfileScrollPosition(this, 0);
        Utilities.setAppPreferenceProfileImagesRvPosition(this, 0);

        if (mSearchScreenFragment!=null) mSearchScreenFragment.updateProfilesListParameters(position, mProfileSelectionRecyclerViewPosition);
    }
    @Override public void onPageScrollStateChanged(int state) {

    }

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {
        mDogsList = dogsList;

        //If the user requested a dogs list, then show the list at the requested distance
        if (TextUtils.isEmpty(mRequestedDogProfileUI) && mSearchScreenFragment!=null) {
            mSearchScreenFragment.updateObjectsList(mDogsList);
            mSearchScreenFragment.updateObjectListAccordingToDistance();
        }

        //If the user requested a specific dog, then update its index in the list for the parent activity.
        else {
            if (!TextUtils.isEmpty(mRequestedFamilyProfileUI)) mSelectedProfileIndex = 0;
            if (mDogsList!=null && mDogsList.size()>0) {
                if (mPagerAdapter!=null) mPagerAdapter.notifyDataSetChanged();
                onProfileSelected(mSelectedProfileIndex);
            }
        }
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
        mFamiliesList = familiesList;

        //If the user requested a family list, then show the list at the requested distance
        if (TextUtils.isEmpty(mRequestedFamilyProfileUI) && mSearchScreenFragment!=null){
            mSearchScreenFragment.updateObjectsList(mFamiliesList);
            mSearchScreenFragment.updateObjectListAccordingToDistance();
        }

        //If the user requested a specific family, then update its index in the list for the parent activity.
        else {
            if (!TextUtils.isEmpty(mRequestedFamilyProfileUI)) mSelectedProfileIndex = 0;
            if (mFamiliesList!=null && mFamiliesList.size()>0) {
                if (mPagerAdapter!=null) mPagerAdapter.notifyDataSetChanged();
                onProfileSelected(mSelectedProfileIndex);
            }
        }
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {
        mFoundationsList = foundationsList;

        //If the user requested a foundations list, then show the list at the requested distance
        if (TextUtils.isEmpty(mRequestedFoundationProfileUI) && mSearchScreenFragment!=null){
            mSearchScreenFragment.updateObjectsList(mFoundationsList);
            mSearchScreenFragment.updateObjectListAccordingToDistance();
        }

        //If the user requested a specific foundation, then update its index in the list for the parent activity.
        else {
            if (!TextUtils.isEmpty(mRequestedFamilyProfileUI)) mSelectedProfileIndex = 0;
            if (mFoundationsList!=null && mFoundationsList.size()>0) {
                if (mPagerAdapter!=null) mPagerAdapter.notifyDataSetChanged();
                onProfileSelected(mSelectedProfileIndex);
            }
        }
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
            Toast.makeText(this, R.string.error_when_getting_preferences, Toast.LENGTH_SHORT).show();
        }

    }
    @Override public void onMapMarkerListFound(List<MapMarker> markersList) {

    }
    @Override public void onImageAvailable(Uri imageUri, String currentImageName) {
    }
    @Override public void onImageUploaded(List<String> uploadTimes) {

    }
}
