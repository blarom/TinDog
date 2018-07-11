package com.tindog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.TinDogUser;
import com.tindog.resources.SharedMethods;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UpdateMyFamilyActivity extends AppCompatActivity implements FirebaseDao.FirebaseOperationsHandler, ImagesRecycleViewAdapter.ImageClickHandler {

    //region Parameters
    private static final String DEBUG_TAG = "TinDog UpdateMyFamily";
    @BindView(R.id.update_my_family_button_choose_main_pic) Button mButtonChooseMainPic;
    @BindView(R.id.update_my_family_button_upload_pics) Button mButtonUploadPics;
    @BindView(R.id.update_my_family_button_done) Button mButtonDone;
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
    @BindView(R.id.update_my_family_checkbox_help_organize) CheckBox mCheckBoxHelpOrganize;
    @BindView(R.id.update_my_family_checkbox_dogwalking) CheckBox mCheckBoxDogWalking;
    @BindView(R.id.update_my_family_image_main) ImageView mImageViewMain;
    @BindView(R.id.update_my_family_recyclerview_pet_images) RecyclerView mRecyclerViewPetImages;
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
    private String mChosenAction;
    private String mFirebaseUid;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private String mFamilyImagesDirectory;
    private boolean mFamilyFound;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_my_family);

        getExtras();
        initializeParameters();
        getFamilyProfileFromFirebase();
        setupPetImagesRecyclerView();
        SharedMethods.refreshMainImageShownToUser(getApplicationContext(), mFamilyImagesDirectory, mImageViewMain);
        SharedMethods.updateImagesFromFirebaseIfRelevant(mFamily, mFirebaseDao);
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
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri croppedImageTempUri = result.getUri();
                boolean succeeded = SharedMethods.shrinkImageWithUri(getApplicationContext(), croppedImageTempUri, 300, 300);

                if (succeeded) {

                    Uri copiedImageUri = SharedMethods.updateImageInLocalDirectoryAndShowIt(getApplicationContext(),
                            croppedImageTempUri, mFamilyImagesDirectory, mImageName, mImageViewMain, mPetImagesRecycleViewAdapter);

                    if (copiedImageUri != null)
                        mFirebaseDao.putImageInFirebaseStorage(mFamily, copiedImageUri, mImageName);
                }
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        if (requestCode == SharedMethods.FIREBASE_SIGN_IN) {
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
                return true;
            case R.id.action_done:
                updateFamilyWithUserInput();
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
    @Override public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        mStoredPetImagesRecyclerViewPosition = SharedMethods.getImagesRecyclerViewPosition(mRecyclerViewPetImages);
        outState.putInt(SharedMethods.PROFILE_UPDATE_PET_IMAGES_RV_POSITION, mStoredPetImagesRecyclerViewPosition);
        outState.putString(SharedMethods.PROFILE_UPDATE_IMAGE_NAME, mImageName);
        super.onSaveInstanceState(outState, outPersistentState);

    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mStoredPetImagesRecyclerViewPosition = savedInstanceState.getInt(SharedMethods.PROFILE_UPDATE_PET_IMAGES_RV_POSITION);
            mRecyclerViewPetImages.scrollToPosition(mStoredPetImagesRecyclerViewPosition);
            mImageName = savedInstanceState.getString(SharedMethods.PROFILE_UPDATE_IMAGE_NAME);
        }
    }


    //Structural methods
    private void getExtras() {
        Intent intent = getIntent();
        if (getIntent().hasExtra(SharedMethods.CHOSEN_ACTION_KEY)) {
            mChosenAction = intent.getStringExtra(SharedMethods.CHOSEN_ACTION_KEY);
        }
    }
    private void initializeParameters() {
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.family_profile));
        }

        ButterKnife.bind(this);
        mEditTextUsername.setEnabled(false);
        mEditTextEmail.setEnabled(false);

        mFamily = new Family();
        mFirebaseDao = new FirebaseDao(getBaseContext(), this);
        mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mFamilyFound = false;
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
            mFamily.setOwnerfirebaseUid(mFirebaseUid);

            //Initializing the local parameters that depend on this family, used in the rest of the activity
            mFamilyImagesDirectory = getFilesDir()+"/families/"+mFamily.getUniqueIdentifier()+"/images/";
            mImageName = "mainImage";

            //Getting the rest of the family's parameters
            if (!mFamilyFound) mFirebaseDao.getUniqueObjectFromFirebaseDb(mFamily);
        }
    }
    private void updateProfileFieldsOnScreen() {
        mEditTextUsername.setText(mNameFromFirebase);
        mEditTextPseudonym.setText(mFamily.getPseudonym());
        mEditTextCell.setText(mFamily.getCell());
        mEditTextEmail.setText(mEmailFromFirebase);
        mEditTextCountry.setText(mFamily.getCountry());
        mEditTextCity.setText(mFamily.getCity());
        mEditTextStreet.setText(mFamily.getStreet());
        mEditTextExperience.setText(mFamily.getExperience());
        mCheckBoxFoster.setChecked(false);
        mCheckBoxAdopt.setChecked(false);
        mCheckBoxFosterAndAdopt.setChecked(false);
        mCheckBoxHelpOrganize.setChecked(false);
        mCheckBoxDogWalking.setChecked(false);
        SharedMethods.hideSoftKeyboard(this);
    }
    private void setupPetImagesRecyclerView() {
        mRecyclerViewPetImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true));
        mRecyclerViewPetImages.setNestedScrollingEnabled(true);
        mPetImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(this, this, SharedMethods.getExistingImageUris(mFamilyImagesDirectory));
        mRecyclerViewPetImages.setAdapter(mPetImagesRecycleViewAdapter);
    }
    private void setButtonBehaviors() {
        mButtonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                updateFamilyWithUserInput();

                if (!TextUtils.isEmpty(mChosenAction)) {
                    if (mChosenAction.equals(getString(R.string.action_search_profiles))) {
                        Intent intent = new Intent(getApplicationContext(), SearchResultsActivity.class);
                        startActivity(intent);
                    }
                    else if (mChosenAction.equals(getString(R.string.action_update_profile))) {
                        Intent intent = new Intent(getApplicationContext(), TaskSelectionActivity.class);
                        startActivity(intent);
                    }
                }
            }
        });

        mButtonChooseMainPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mImageName = "mainImage";
                performImageCaptureAndCrop();
            }
        });

        mButtonUploadPics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (SharedMethods.getExistingImageUris(mFamilyImagesDirectory).size() == 5) {
                    Toast.makeText(getApplicationContext(), R.string.reached_max_images, Toast.LENGTH_SHORT).show();
                }
                else {
                    mImageName = SharedMethods.getNameOfFirstAvailableImageInImagesList(mFamilyImagesDirectory);
                    performImageCaptureAndCrop();
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
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_in:" + mCurrentFirebaseUser.getUid());
                    //getFamilyProfileFromFirebase();
                } else {
                    // TinDogUser is signed out
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_out");
                    //Showing the sign-in screen
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
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private void cleanUpListeners() {
        if (mFirebaseAuth!=null) mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }
    private void updateFamilyWithUserInput() {
        mFamily.setOwnerfirebaseUid(mFirebaseUid);
        mFamily.setPseudonym(mEditTextPseudonym.getText().toString());
        mFamily.setCell(mEditTextCell.getText().toString());
        mFamily.setEmail(mEditTextEmail.getText().toString());
        mFamily.setCountry(mEditTextCountry.getText().toString());
        mFamily.setCity(mEditTextCity.getText().toString());
        mFamily.setStreet(mEditTextStreet.getText().toString());
        mFamily.setExperience(mEditTextExperience.getText().toString());

        mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mFamily);
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
            Toast.makeText(getBaseContext(), "No user found for your entered email, press DONE to create a new user.", Toast.LENGTH_SHORT).show();
        }

        updateProfileFieldsOnScreen();
        SharedMethods.updateImagesFromFirebaseIfRelevant(mFamily, mFirebaseDao);
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {

    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onImageAvailable(Uri downloadedImageUri, String imageName) {

        SharedMethods.synchronizeImageOnAllDevices(getApplicationContext(),
                mFamily, mFirebaseDao, mFamilyImagesDirectory, imageName, downloadedImageUri, mImageViewMain, mPetImagesRecycleViewAdapter);

    }

}
