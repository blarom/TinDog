package com.tindog.ui;

import android.content.Intent;
import android.location.Address;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.tindog.R;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.MapMarker;
import com.tindog.data.TinDogUser;
import com.tindog.resources.Utilities;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class UpdateMyFoundationActivity extends AppCompatActivity implements FirebaseDao.FirebaseOperationsHandler, ImagesRecycleViewAdapter.ImageClickHandler {
    
    //region Parameters
    private static final String DEBUG_TAG = "TinDog Update";
    @BindView(R.id.update_my_foundation_value_name) TextInputEditText mEditTextName;
    @BindView(R.id.update_my_foundation_value_contact_phone) TextInputEditText mEditTextContactPhone;
    @BindView(R.id.update_my_foundation_value_contact_email) TextInputEditText mEditTextContactEmail;
    @BindView(R.id.update_my_foundation_value_website) TextInputEditText mEditTextWebsite;
    @BindView(R.id.update_my_foundation_value_country) TextInputEditText mEditTextCountry;
    @BindView(R.id.update_my_foundation_value_state) TextInputEditText mEditTextState;
    @BindView(R.id.update_my_foundation_value_city) TextInputEditText mEditTextCity;
    @BindView(R.id.update_my_foundation_value_street) TextInputEditText mEditTextStreet;
    @BindView(R.id.update_my_foundation_value_street_number) TextInputEditText mEditTextStreetNumber;
    @BindView(R.id.update_my_foundation_image_main) ImageView mImageViewMain;
    @BindView(R.id.update_my_foundation_recyclerview_images) RecyclerView mRecyclerViewFoundationImages;
    @BindView(R.id.update_my_foundation_scroll_container) NestedScrollView mScrollViewContainer;
    private Unbinder mBinding;
    private Foundation mFoundation;
    private FirebaseDao mFirebaseDao;
    private ImagesRecycleViewAdapter mFoundationImagesRecycleViewAdapter;
    private String mImageName = "mainImage";
    private int mStoredFoundationImagesRecyclerViewPosition;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentFirebaseUser;
    private String mNameFromFirebase;
    private String mEmailFromFirebase;
    private Uri mPhotoUriFromFirebase;
    private String mFirebaseUid;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private boolean[] mImagesReady;
    private int mScrollPosition;
    private Bundle mSavedInstanceState;
    private boolean mFoundationFound;
    private boolean mFoundationCriticalParametersSet;
    private Menu mMenu;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_my_foundation);

        mSavedInstanceState = savedInstanceState;
        initializeParameters();
        getFoundationProfileFromFirebase();
        setupFoundationImagesRecyclerView();
        Utilities.displayObjectImageInImageView(getApplicationContext(), mFoundation, "mainImage", mImageViewMain);
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
        mBinding.unbind();
        mFirebaseDao.removeListeners();
    }
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri croppedImageTempUri = result.getUri();
                boolean succeeded = Utilities.shrinkImageWithUri(getApplicationContext(), croppedImageTempUri, 300, 300);

                if (succeeded) {
                    Uri copiedImageUri = Utilities.updateLocalObjectImage(getApplicationContext(), croppedImageTempUri, mFoundation, mImageName);
                    if (copiedImageUri != null) {
                        if (mImageName.equals("mainImage")) {
                            Utilities.displayObjectImageInImageView(getApplicationContext(), mFoundation, "mainImage", mImageViewMain);
                        }
                        else {
                            List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mFoundation, true);
                            mFoundationImagesRecycleViewAdapter.setContents(uris);
                        }
                        mFirebaseDao.putImageInFirebaseStorage(mFoundation, copiedImageUri, mImageName);
                    }
                }
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        if (requestCode == Utilities.FIREBASE_SIGN_IN_KEY) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                Utilities.updateSignInMenuItem(mMenu, this, true);
                getFoundationProfileFromFirebase();
                // ...
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
        getMenuInflater().inflate(R.menu.update_my_foundation_menu, menu);
        mMenu = menu;
        if (mCurrentFirebaseUser==null) Utilities.updateSignInMenuItem(mMenu, this, false);
        else Utilities.updateSignInMenuItem(mMenu, this, true);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.action_save:
                updateFoundationWithUserInput();
                if (mFoundationCriticalParametersSet) {
                    if (!TextUtils.isEmpty(mFirebaseUid)) mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mFoundation, true);
                }
                return true;
            case R.id.action_done:
                updateFoundationWithUserInput();
                if (mFoundationCriticalParametersSet) {
                    if (!TextUtils.isEmpty(mFirebaseUid)) mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mFoundation, true);
                }
                finish();
                return true;
            case R.id.action_edit_preferences:
                Intent intent = new Intent(this, PreferencesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            case R.id.action_signin:
                if (mCurrentFirebaseUser==null) {
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                    Utilities.showSignInScreen(UpdateMyFoundationActivity.this);
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
    @Override public void onSaveInstanceState(Bundle outState) {
        mStoredFoundationImagesRecyclerViewPosition = Utilities.getLinearRecyclerViewPosition(mRecyclerViewFoundationImages);
        outState.putInt(getString(R.string.profile_update_pet_images_rv_position), mStoredFoundationImagesRecyclerViewPosition);
        outState.putString(getString(R.string.profile_update_image_name), mImageName);
        mScrollPosition = mScrollViewContainer.getScrollY();
        outState.putInt(getString(R.string.scroll_position),mScrollPosition);
        outState.putString(getString(R.string.saved_firebase_email), mEmailFromFirebase);
        outState.putString(getString(R.string.saved_firebase_name), mNameFromFirebase);
        outState.putString(getString(R.string.saved_firebase_id), mFirebaseUid);
        outState.putBoolean(getString(R.string.critical_parameters_set), mFoundationCriticalParametersSet);
        updateFoundationWithUserInput();
        outState.putParcelable(getString(R.string.saved_profile), mFoundation);
        super.onSaveInstanceState(outState);

    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mStoredFoundationImagesRecyclerViewPosition = savedInstanceState.getInt(getString(R.string.profile_update_pet_images_rv_position));
            mRecyclerViewFoundationImages.scrollToPosition(mStoredFoundationImagesRecyclerViewPosition);
            mImageName = savedInstanceState.getString(getString(R.string.profile_update_image_name));
            mFoundation = savedInstanceState.getParcelable(getString(R.string.saved_profile));
            mScrollPosition = savedInstanceState.getInt(getString(R.string.scroll_position));
            mEmailFromFirebase = savedInstanceState.getString(getString(R.string.saved_firebase_email));
            mNameFromFirebase = savedInstanceState.getString(getString(R.string.saved_firebase_name));
            mFirebaseUid = savedInstanceState.getString(getString(R.string.saved_firebase_id));
            mFoundationCriticalParametersSet = savedInstanceState.getBoolean(getString(R.string.critical_parameters_set));

            mScrollViewContainer.setScrollY(mScrollPosition);
            updateProfileFieldsOnScreen();
            setupFoundationImagesRecyclerView();
            Utilities.displayObjectImageInImageView(getApplicationContext(), mFoundation, "mainImage", mImageViewMain);
        }
    }


    //Functional methods
    private void initializeParameters() {
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.foundation_profile);
        }
        mBinding =  ButterKnife.bind(this);
        mImagesReady = new boolean[]{false, false, false, false, false, false};
        mFoundationFound = false;
        mFoundationCriticalParametersSet = false;
        mFoundation = new Foundation();
        mFirebaseDao = new FirebaseDao(getBaseContext(), this);
        mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseAuth = FirebaseAuth.getInstance();

    }
    private void getFoundationProfileFromFirebase() {
        if (mCurrentFirebaseUser != null) {
            // Name, email address, and profile photo Url
            mNameFromFirebase = mCurrentFirebaseUser.getDisplayName();
            mEmailFromFirebase = mCurrentFirebaseUser.getEmail();
            mPhotoUriFromFirebase = mCurrentFirebaseUser.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = mCurrentFirebaseUser.isEmailVerified();

            //Setting the requested Foundation's id
            mFirebaseUid = mCurrentFirebaseUser.getUid();
            mFoundation.setOFId(mFirebaseUid);

            //Initializing the local parameters that depend on this family, used in the rest of the activity
            mImageName = "mainImage";

            //Getting the rest of the family's parameters
            if (!mFoundationFound) mFirebaseDao.getUniqueObjectFromFirebaseDbOrCreateIt(mFoundation, true);
        }
    }
    private void updateProfileFieldsOnScreen() {
        mEditTextName.setText(mFoundation.getNm());
        mEditTextContactPhone.setText(mFoundation.getCP());
        mEditTextContactEmail.setText(mFoundation.getCE());
        mEditTextWebsite.setText(mFoundation.getWb());
        mEditTextCountry.setText(mFoundation.getCn());
        mEditTextState.setText(mFoundation.getSe());
        mEditTextCity.setText(mFoundation.getCt());
        mEditTextStreet.setText(mFoundation.getSt());
        mEditTextStreetNumber.setText(mFoundation.getStN());
        Utilities.hideSoftKeyboard(this);
    }
    private void setupFoundationImagesRecyclerView() {
        mRecyclerViewFoundationImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mRecyclerViewFoundationImages.setNestedScrollingEnabled(true);
        List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mFoundation, true);
        mFoundationImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(this, this, uris);
        mRecyclerViewFoundationImages.setAdapter(mFoundationImagesRecycleViewAdapter);
    }
    private void performImageCaptureAndCrop() {
        // start source picker (camera, gallery, etc..) to get image for cropping and then use the image in cropping activity
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(this);
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
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_in:" + mCurrentFirebaseUser.getUid());
                    //getFoundationProfileFromFirebase();
                } else {
                    // TinDogUser is signed out
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_out");
                    //Showing the sign-in screen
                    if (Utilities.getAppPreferenceUserHasNotRefusedSignIn(getApplicationContext())) {
                        mSavedInstanceState = null;
                        Utilities.showSignInScreen(UpdateMyFoundationActivity.this);
                    }
                }
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private void cleanUpListeners() {
        if (mFirebaseAuth!=null) mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }
    private void updateFoundationWithUserInput() {
        mFoundation.setOFId(mFirebaseUid);
        mFoundation.setCP(mEditTextContactPhone.getText().toString());
        mFoundation.setCE(mEditTextContactEmail.getText().toString());
        mFoundation.setWb(mEditTextWebsite.getText().toString());
        mFoundation.setCn(mEditTextCountry.getText().toString());

        String name = mEditTextName.getText().toString();
        String country = mEditTextCountry.getText().toString();
        String state = mEditTextState.getText().toString();
        String city = mEditTextCity.getText().toString();
        String street = mEditTextStreet.getText().toString();
        String streeNumber = mEditTextStreetNumber.getText().toString();

        mFoundation.setNm(name);
        mFoundation.setCn(country);
        mFoundation.setCt(state);
        mFoundation.setCt(city);
        mFoundation.setSt(street);
        mFoundation.setStN(streeNumber);

        String addressString = Utilities.getAddressStringFromComponents(streeNumber, street, city, state, country);
        Address address = Utilities.getAddressObjectFromAddressString(this, addressString);
        if (address!=null) {
            String geoAddressCountry = address.getCountryCode();
            double geoAddressLatitude = address.getLatitude();
            double geoAddressLongitude = address.getLongitude();

            mFoundation.setGaC(geoAddressCountry);
            mFoundation.setGaLt(Double.toString(geoAddressLatitude));
            mFoundation.setGaLg(Double.toString(geoAddressLongitude));
        }

        mFoundation.setSt(mEditTextStreet.getText().toString());
        mFoundation.setStN(mEditTextStreetNumber.getText().toString());

        mFoundation.setUniqueIdentifierFromDetails();

        if (name.length() < 2 || country.length() < 2 || city.length() < 1) {
            Toast.makeText(getApplicationContext(), R.string.foundation_not_saved, Toast.LENGTH_SHORT).show();
            mFoundationCriticalParametersSet = false;
        }
        else {
            mFoundationCriticalParametersSet = true;
        }

        if (TextUtils.isEmpty(mFoundation.getUI())) Log.i(DEBUG_TAG, "Error: TinDog Foundation has empty unique ID!");
    }


    //View click listeners
    @OnClick(R.id.update_my_foundation_button_choose_main_pic) public void onChooseMainPicButtonClick() {
        if (mFoundationCriticalParametersSet && !TextUtils.isEmpty(mFoundation.getUI())) {
            mImageName = "mainImage";
            performImageCaptureAndCrop();
        }
        else {
            Toast.makeText(getApplicationContext(), R.string.must_save_profile_first, Toast.LENGTH_SHORT).show();
        }
    }
    @OnClick(R.id.update_my_foundation_button_upload_pics) public void onUploadPicsButtonClick() {
        if (mFoundationCriticalParametersSet && !TextUtils.isEmpty(mFoundation.getUI())) {

            List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mFoundation, true);
            if (uris.size() == 5) {
                Toast.makeText(getApplicationContext(), R.string.reached_max_images, Toast.LENGTH_SHORT).show();
            }
            else {
                mImageName = Utilities.getNameOfFirstAvailableImageInImagesList(getApplicationContext(), mFoundation);
                if (!TextUtils.isEmpty(mImageName)) performImageCaptureAndCrop();
                else Toast.makeText(getApplicationContext(), R.string.error_processing_request, Toast.LENGTH_SHORT).show();
            }

        }
        else {
            Toast.makeText(getApplicationContext(), R.string.must_save_profile_first, Toast.LENGTH_SHORT).show();
        }
    }


    //Communication with other activities/fragments:

    //Communication with RecyclerView adapters
    @Override public void onImageClick(int clickedItemIndex) {
        switch (clickedItemIndex) {
            case 0: mImageName = getString(R.string.image1); break;
            case 1: mImageName = getString(R.string.image2); break;
            case 2: mImageName = getString(R.string.image3); break;
            case 3: mImageName = getString(R.string.image4); break;
            case 4: mImageName = getString(R.string.image5); break;
        }
        performImageCaptureAndCrop();
    }

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {

    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {
        if (foundationsList.size()==1) {
            if (foundationsList.get(0) != null) {
                mFoundation = foundationsList.get(0);
                mFoundationFound = true;
            }
        }
        else if (foundationsList.size()>1) {
            mFoundation = foundationsList.get(0);
            mFoundationFound = true;
            Log.i(DEBUG_TAG, "Warning! Multiple foundations found for the user's credentials.");
        }
        else {
            mFoundation = new Foundation(mFirebaseUid);
            Toast.makeText(getBaseContext(), R.string.foundation_not_found_press_done_to_create, Toast.LENGTH_SHORT).show();
        }

        if (mSavedInstanceState==null) {
            updateProfileFieldsOnScreen();
            updateFoundationWithUserInput();
        }
        mFirebaseDao.getAllObjectImagesFromFirebaseStorage(mFoundation);
    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onMapMarkerListFound(List<MapMarker> markersList) {

    }
    @Override public void onImageAvailable(Uri downloadedImageUri, String imageName) {

        if (mImageViewMain==null
                || mFoundationImagesRecycleViewAdapter==null
                || mFoundation==null) return;

        Utilities.synchronizeImageOnAllDevices(getApplicationContext(), mFoundation, mFirebaseDao, imageName, downloadedImageUri);

        //Only showing the images if all images are ready (prevents image flickering)
        switch (imageName) {
            case "mainImage": mImagesReady[0] = true; break;
            case "image1": mImagesReady[1] = true; break;
            case "image2": mImagesReady[2] = true; break;
            case "image3": mImagesReady[3] = true; break;
            case "image4": mImagesReady[4] = true; break;
            case "image5": mImagesReady[5] = true; break;
        }
        boolean allImagesReady = true;
        for (boolean isReady : mImagesReady) {
            if (!isReady) { allImagesReady = false; break; }
        }
        if (allImagesReady) {
            Utilities.displayObjectImageInImageView(getApplicationContext(), mFoundation, "mainImage", mImageViewMain);
            List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mFoundation, true);
            mFoundationImagesRecycleViewAdapter.setContents(uris);
        }

    }
    @Override public void onImageUploaded(List<String> uploadTimes) {
        mFoundation.setIUT(uploadTimes);
    }
}
