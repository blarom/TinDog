package com.tindog;

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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.TinDogUser;
import com.tindog.resources.Utilities;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class UpdateMyFamilyActivity extends AppCompatActivity implements FirebaseDao.FirebaseOperationsHandler, ImagesRecycleViewAdapter.ImageClickHandler, AdapterView.OnItemSelectedListener {

    //region Parameters
    private static final String DEBUG_TAG = "TinDog UpdateMyFamily";
    @BindView(R.id.update_my_family_button_choose_main_pic) Button mButtonChooseMainPic;
    @BindView(R.id.update_my_family_button_upload_pics) Button mButtonUploadPics;
    @BindView(R.id.update_my_family_value_username) TextInputEditText mEditTextUsername;
    @BindView(R.id.update_my_family_value_pseudonym) TextInputEditText mEditTextPseudonym;
    @BindView(R.id.update_my_family_value_cell) TextInputEditText mEditTextCell;
    @BindView(R.id.update_my_family_value_email) TextInputEditText mEditTextEmail;
    @BindView(R.id.update_my_family_value_country) TextInputEditText mEditTextCountry;
    @BindView(R.id.update_my_family_value_city) TextInputEditText mEditTextCity;
    @BindView(R.id.update_my_family_value_street) TextInputEditText mEditTextStreet;
    @BindView(R.id.update_my_family_value_history) TextInputEditText mEditTextExperience;
    @BindView(R.id.update_my_family_checkbox_foster) CheckBox mCheckBoxFoster;
    @BindView(R.id.update_my_family_checkbox_adopt) CheckBox mCheckBoxAdopt;
    @BindView(R.id.update_my_family_checkbox_foster_and_adopt) CheckBox mCheckBoxFosterAndAdopt;
    @BindView(R.id.update_my_family_spinner_foster_period) Spinner mSpinnerFosterPeriod;
    @BindView(R.id.update_my_family_checkbox_help_organize_move_equipment) CheckBox mCheckBoxHelpOrganizeMovingEquipment;
    @BindView(R.id.update_my_family_checkbox_help_organize_move_dogs) CheckBox mCheckBoxHelpOrganizeMovingDogs;
    @BindView(R.id.update_my_family_checkbox_help_organize_coordinating) CheckBox mCheckBoxHelpOrganizeCoordinating;
    @BindView(R.id.update_my_family_checkbox_help_organize_lending_hand) CheckBox mCheckBoxHelpOrganizeLendingHand;
    @BindView(R.id.update_my_family_checkbox_dogwalking) CheckBox mCheckBoxDogWalking;
    @BindView(R.id.update_my_family_spinner_where_dogwalking) Spinner mSpinnerDogWalkingWhere;
    @BindView(R.id.update_my_family_checkbox_dogwalking_afternoon) CheckBox mCheckBoxDogWalkingAfternoon;
    @BindView(R.id.update_my_family_checkbox_dogwalking_evening) CheckBox mCheckBoxDogWalkingEvening;
    @BindView(R.id.update_my_family_checkbox_dogwalking_morning) CheckBox mCheckBoxDogWalkingMorning;
    @BindView(R.id.update_my_family_checkbox_dogwalking_noon) CheckBox mCheckBoxDogWalkingNoon;
    @BindView(R.id.update_my_family_image_main) ImageView mImageViewMain;
    @BindView(R.id.update_my_family_recyclerview_pet_images) RecyclerView mRecyclerViewPetImages;
    @BindView(R.id.update_my_family_scroll_container) NestedScrollView mScrollViewContainer;
    private Unbinder mBinding;
    private Family mFamily;
    private FirebaseDao mFirebaseDao;
    private ImagesRecycleViewAdapter mPetImagesRecycleViewAdapter;
    private String mImageName = "mainImage";
    private int mStoredPetImagesRecyclerViewPosition;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentFirebaseUser;
    private String mNameFromFirebase;
    private String mEmailFromFirebase;
    private Uri mPhotoUriFromFirebase;
    private String mFirebaseUid;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private boolean mFamilyFound;
    private ArrayAdapter<CharSequence> mSpinnerAdapterDogwalkingWhere;
    private ArrayAdapter<CharSequence> mSpinnerAdapterFosterPeriod;
    private boolean[] mImagesReady;
    private int mScrollPosition;
    private Bundle mSavedInstanceState;
    private boolean mFamilyCriticalParametersSet;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_my_family);

        mSavedInstanceState = savedInstanceState;
        initializeParameters();
        getFamilyProfileFromFirebase();
        setupPetImagesRecyclerView();
        Utilities.displayObjectImageInImageView(getApplicationContext(), mFamily, "mainImage", mImageViewMain);
        setButtonBehaviors();
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
                    Uri copiedImageUri = Utilities.updateLocalObjectImage(getApplicationContext(), croppedImageTempUri, mFamily, mImageName);
                    if (copiedImageUri != null)
                        if (mImageName.equals("mainImage")) {
                            Utilities.displayObjectImageInImageView(getApplicationContext(), mFamily, "mainImage", mImageViewMain);
                        }
                        else {
                            List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mFamily, true);
                            mPetImagesRecycleViewAdapter.setContents(uris);
                        }
                        mFirebaseDao.putImageInFirebaseStorage(mFamily, copiedImageUri, mImageName);
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
                getFamilyProfileFromFirebase();
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.update_my_family_menu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.action_save:
                updateFamilyWithUserInput();
                if (mFamilyCriticalParametersSet) {
                    if (!TextUtils.isEmpty(mFirebaseUid)) mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mFamily);
                }
                return true;
            case R.id.action_done:
                updateFamilyWithUserInput();
                if (mFamilyCriticalParametersSet) {
                    if (!TextUtils.isEmpty(mFirebaseUid)) mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mFamily);
                }
                finish();
                return true;
            case R.id.action_edit_preferences:
                Intent intent = new Intent(this, PreferencesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override public void onSaveInstanceState(Bundle outState) {
        mStoredPetImagesRecyclerViewPosition = Utilities.getImagesRecyclerViewPosition(mRecyclerViewPetImages);
        outState.putInt(getString(R.string.profile_update_pet_images_rv_position), mStoredPetImagesRecyclerViewPosition);
        outState.putString(getString(R.string.profile_update_image_name), mImageName);
        mScrollPosition = mScrollViewContainer.getScrollY();
        outState.putInt(getString(R.string.scroll_position),mScrollPosition);
        outState.putString(getString(R.string.saved_firebase_email), mEmailFromFirebase);
        outState.putString(getString(R.string.saved_firebase_name), mNameFromFirebase);
        outState.putString(getString(R.string.saved_firebase_id), mFirebaseUid);
        updateFamilyWithUserInput();
        outState.putParcelable(getString(R.string.saved_profile), mFamily);
        super.onSaveInstanceState(outState);

    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mStoredPetImagesRecyclerViewPosition = savedInstanceState.getInt(getString(R.string.profile_update_pet_images_rv_position));
            mRecyclerViewPetImages.scrollToPosition(mStoredPetImagesRecyclerViewPosition);
            mImageName = savedInstanceState.getString(getString(R.string.profile_update_image_name));
            mFamily = savedInstanceState.getParcelable(getString(R.string.saved_profile));
            mScrollPosition = savedInstanceState.getInt(getString(R.string.scroll_position));
            mEmailFromFirebase = savedInstanceState.getString(getString(R.string.saved_firebase_email));
            mNameFromFirebase = savedInstanceState.getString(getString(R.string.saved_firebase_name));
            mFirebaseUid = savedInstanceState.getString(getString(R.string.saved_firebase_id));

            mScrollViewContainer.setScrollY(mScrollPosition);
            updateProfileFieldsOnScreen();
            setupPetImagesRecyclerView();
            Utilities.displayObjectImageInImageView(getApplicationContext(), mFamily, "mainImage", mImageViewMain);
        }
    }


    //Structural methods
    private void initializeParameters() {
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.family_profile));
        }

        mBinding =  ButterKnife.bind(this);
        mEditTextUsername.setEnabled(false);
        mEditTextEmail.setEnabled(false);

        mFamily = new Family();
        mFirebaseDao = new FirebaseDao(getBaseContext(), this);
        mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mFamilyCriticalParametersSet = false;
        mImagesReady = new boolean[]{false, false, false, false, false, false};
        mFamilyFound = false;

        mSpinnerAdapterDogwalkingWhere = ArrayAdapter.createFromResource(this, R.array.dogwalking_location, android.R.layout.simple_spinner_item);
        mSpinnerAdapterDogwalkingWhere.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerDogWalkingWhere.setAdapter(mSpinnerAdapterDogwalkingWhere);
        mSpinnerDogWalkingWhere.setOnItemSelectedListener(this);

        mSpinnerAdapterFosterPeriod = ArrayAdapter.createFromResource(this, R.array.foster_period, android.R.layout.simple_spinner_item);
        mSpinnerAdapterFosterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerFosterPeriod.setAdapter(mSpinnerAdapterFosterPeriod);
        mSpinnerFosterPeriod.setOnItemSelectedListener(this);
    }
    private void getFamilyProfileFromFirebase() {
        if (mCurrentFirebaseUser != null) {
            // Name, email address, and profile photo Url
            mNameFromFirebase = mCurrentFirebaseUser.getDisplayName();
            mEmailFromFirebase = mCurrentFirebaseUser.getEmail();
            mPhotoUriFromFirebase = mCurrentFirebaseUser.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = mCurrentFirebaseUser.isEmailVerified();

            //Setting the requested Family's id
            mFirebaseUid = mCurrentFirebaseUser.getUid();
            mFamily.setOFid(mFirebaseUid);

            //Initializing the local parameters that depend on this family, used in the rest of the activity
            mImageName = "mainImage";

            //Getting the rest of the family's parameters
            if (!mFamilyFound) mFirebaseDao.getUniqueObjectFromFirebaseDbOrCreateIt(mFamily);
        }
    }
    private void updateProfileFieldsOnScreen() {
        mEditTextUsername.setText(mNameFromFirebase);
        mEditTextPseudonym.setText(mFamily.getPn());
        mEditTextCell.setText(mFamily.getCp());
        mEditTextEmail.setText(mEmailFromFirebase);
        mEditTextCountry.setText(mFamily.getCn());
        mEditTextCity.setText(mFamily.getCt());
        mEditTextStreet.setText(mFamily.getSt());
        mEditTextExperience.setText(mFamily.getXp());
        mCheckBoxFoster.setChecked(mFamily.getFD());
        mCheckBoxAdopt.setChecked(mFamily.getAD());
        mCheckBoxFosterAndAdopt.setChecked(mFamily.getFAD());
        mSpinnerFosterPeriod.setSelection(Utilities.getSpinnerPositionFromText(mSpinnerFosterPeriod, mFamily.getFP()));
        mCheckBoxHelpOrganizeMovingEquipment.setChecked(mFamily.getHOE());
        mCheckBoxHelpOrganizeMovingDogs.setChecked(mFamily.getHOD());
        mCheckBoxHelpOrganizeCoordinating.setChecked(mFamily.getHOC());
        mCheckBoxHelpOrganizeLendingHand.setChecked(mFamily.getHOL());
        mCheckBoxDogWalking.setChecked(mFamily.getHD());
        mSpinnerDogWalkingWhere.setSelection(Utilities.getSpinnerPositionFromText(mSpinnerDogWalkingWhere, mFamily.getHDW()));
        mCheckBoxDogWalkingMorning.setChecked(mFamily.getHDM());
        mCheckBoxDogWalkingNoon.setChecked(mFamily.getHDN());
        mCheckBoxDogWalkingAfternoon.setChecked(mFamily.setHDA());
        mCheckBoxDogWalkingEvening.setChecked(mFamily.getHDE());

        Utilities.hideSoftKeyboard(this);
    }
    private void setupPetImagesRecyclerView() {
        mRecyclerViewPetImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mRecyclerViewPetImages.setNestedScrollingEnabled(true);
        List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mFamily, true);
        mPetImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(this, this, uris);
        mRecyclerViewPetImages.setAdapter(mPetImagesRecycleViewAdapter);
    }
    private void setButtonBehaviors() {
        mButtonChooseMainPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mFamilyCriticalParametersSet && !TextUtils.isEmpty(mFamily.getUI())) {
                    mImageName = "mainImage";
                    performImageCaptureAndCrop();
                }
                else {
                    Toast.makeText(getApplicationContext(), R.string.must_save_profile_first, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mButtonUploadPics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mFamilyCriticalParametersSet && !TextUtils.isEmpty(mFamily.getUI())) {

                    List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mFamily, true);
                    if (uris.size() == 5) {
                        Toast.makeText(getApplicationContext(), R.string.reached_max_images, Toast.LENGTH_SHORT).show();
                    }
                    else {
                        mImageName = Utilities.getNameOfFirstAvailableImageInImagesList(getApplicationContext(), mFamily);
                        if (!TextUtils.isEmpty(mImageName)) performImageCaptureAndCrop();
                        else Toast.makeText(getApplicationContext(), R.string.error_processing_request, Toast.LENGTH_SHORT).show();
                    }

                }
                else {
                    Toast.makeText(getApplicationContext(), R.string.must_save_profile_first, Toast.LENGTH_SHORT).show();
                }


            }
        });
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
                    //getFamilyProfileFromFirebase();
                } else {
                    // TinDogUser is signed out
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_out");
                    //Showing the sign-in screen
                    if (Utilities.getAppPreferenceUserHasNotRefusedSignIn(getApplicationContext())) {
                        mSavedInstanceState = null;
                        Utilities.showSignInScreen(UpdateMyFamilyActivity.this);
                    }
                }
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private void cleanUpListeners() {
        if (mFirebaseAuth!=null) mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }
    private void updateFamilyWithUserInput() {
        mFamily.setOFid(mFirebaseUid);
        mFamily.setCp(mEditTextCell.getText().toString());
        mFamily.setEm(mEditTextEmail.getText().toString());
        mFamily.setCn(mEditTextCountry.getText().toString());

        String pseudonym = mEditTextPseudonym.getText().toString();
        String country = mEditTextCountry.getText().toString();
        String city = mEditTextCity.getText().toString();
        String street = mEditTextStreet.getText().toString();

        mFamily.setPn(pseudonym);
        mFamily.setCn(country);
        mFamily.setCt(city);
        mFamily.setSt(street);

        String addressString = street + ", " + city + ", " + country;
        Address address = Utilities.getAddressObjectFromAddressString(this, addressString);
        if (address!=null) {
            String geoAddressCountry = address.getCountryCode();
            double geoAddressLatitude = address.getLatitude();
            double geoAddressLongitude = address.getLongitude();

            mFamily.setGaC(geoAddressCountry);
            mFamily.setGaLt(Double.toString(geoAddressLatitude));
            mFamily.setGaLg(Double.toString(geoAddressLongitude));
        }

        mFamily.setSt(mEditTextStreet.getText().toString());
        mFamily.setXp(mEditTextExperience.getText().toString());

        mFamily.setFD(mCheckBoxFoster.isChecked());
        mFamily.setAD(mCheckBoxAdopt.isChecked());
        mFamily.setFAD(mCheckBoxFosterAndAdopt.isChecked());
        mFamily.setFP(mSpinnerFosterPeriod.getSelectedItem().toString());
        mFamily.setHOE(mCheckBoxHelpOrganizeMovingEquipment.isChecked());
        mFamily.setHOD(mCheckBoxHelpOrganizeMovingDogs.isChecked());
        mFamily.setHOC(mCheckBoxHelpOrganizeCoordinating.isChecked());
        mFamily.setHOL(mCheckBoxHelpOrganizeLendingHand.isChecked());
        mFamily.setHD(mCheckBoxDogWalking.isChecked());
        mFamily.setHDW(mSpinnerDogWalkingWhere.getSelectedItem().toString());
        mFamily.setHDM(mCheckBoxDogWalkingMorning.isChecked());
        mFamily.setHDN(mCheckBoxDogWalkingNoon.isChecked());
        mFamily.getHDA(mCheckBoxDogWalkingAfternoon.isChecked());
        mFamily.setHDE(mCheckBoxDogWalkingEvening.isChecked());

        if (pseudonym.length() < 2 || country.length() < 2 || city.length() < 1) {
            Toast.makeText(getApplicationContext(), R.string.family_not_saved, Toast.LENGTH_SHORT).show();
            mFamilyCriticalParametersSet = false;
        }
        else {
            mFamilyCriticalParametersSet = true;
        }

        if (TextUtils.isEmpty(mFamily.getUI())) Log.i(DEBUG_TAG, "Error: TinDog Family has empty unique ID!");

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
        if (familiesList.size()==1) {
            if (familiesList.get(0) != null) {
                mFamily = familiesList.get(0);
                mFamilyFound = true;
            }
        }
        else if (familiesList.size()>1) {
            mFamily = familiesList.get(0);
            mFamilyFound = true;
            Log.i(DEBUG_TAG, "Warning! Multiple users found for your entered email.");
        }
        else {
            mFamily = new Family(mFirebaseUid);
            Toast.makeText(getBaseContext(), "Your family doesn't exist yet, press DONE to create your family.", Toast.LENGTH_SHORT).show();
        }

        if (mSavedInstanceState==null) updateProfileFieldsOnScreen();
        mFirebaseDao.getAllObjectImagesFromFirebaseStorage(mFamily);
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {

    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onImageAvailable(Uri downloadedImageUri, String imageName) {

        if (mImageViewMain==null
                || mPetImagesRecycleViewAdapter==null
                || mFamily==null) return;

        Utilities.synchronizeImageOnAllDevices(getApplicationContext(), mFamily, mFirebaseDao, imageName, downloadedImageUri);

        //Display the images

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
            Utilities.displayObjectImageInImageView(getApplicationContext(), mFamily, "mainImage", mImageViewMain);
            List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mFamily, true);
            mPetImagesRecycleViewAdapter.setContents(uris);
        }

    }
    @Override public void onImageUploaded(List<String> uploadTimes) {
        mFamily.setUIT(uploadTimes);
    }

    //Communication with spinner adapters
    @Override public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        switch (adapterView.getId()) {
            case R.id.update_my_family_spinner_where_dogwalking:
                mFamily.setHDW((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_my_family_spinner_foster_period:
                mFamily.setFP((String) adapterView.getItemAtPosition(pos));
                break;
        }
    }
    @Override public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
