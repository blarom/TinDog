package com.tindog;

import android.content.Intent;
import android.net.Uri;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
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
import com.tindog.resources.SharedMethods;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UpdateMyFoundationActivity extends AppCompatActivity implements FirebaseDao.FirebaseOperationsHandler, ImagesRecycleViewAdapter.ImageClickHandler {
    
    //region Parameters
    private static final String DEBUG_TAG = "TinDog Update";
    @BindView(R.id.update_my_foundation_button_choose_main_pic) Button mButtonChooseMainPic;
    @BindView(R.id.update_my_foundation_button_upload_pics) Button mButtonUploadPics;
    @BindView(R.id.update_my_foundation_value_name) TextInputEditText mEditTextName;
    @BindView(R.id.update_my_foundation_value_contact_phone) TextInputEditText mEditTextContactPhone;
    @BindView(R.id.update_my_foundation_value_contact_email) TextInputEditText mEditTextContactEmail;
    @BindView(R.id.update_my_foundation_value_website) TextInputEditText mEditTextWebsite;
    @BindView(R.id.update_my_foundation_value_country) TextInputEditText mEditTextCountry;
    @BindView(R.id.update_my_foundation_value_city) TextInputEditText mEditTextCity;
    @BindView(R.id.update_my_foundation_value_street) TextInputEditText mEditTextStreet;
    @BindView(R.id.update_my_foundation_value_street_number) TextInputEditText mEditTextStreetNumber;
    @BindView(R.id.update_my_foundation_image_main) ImageView mImageViewMain;
    @BindView(R.id.update_my_foundation_recyclerview_images) RecyclerView mRecyclerViewFoundationImages;
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
    private String mChosenAction;
    private String mFirebaseUid;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private String mFoundationImagesDirectory;
    //endregion


    //Lifecycle methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_my_foundation);

        getExtras();
        initializeParameters();
        getFoundationProfileFromFirebase();
        updateProfileFieldsOnScreen();
        setupFoundationImagesRecyclerView();
        SharedMethods.refreshMainImageShownToUser(getApplicationContext(), mFoundationImagesDirectory, mImageViewMain);
        SharedMethods.refreshImagesListShownToUser(mFoundationImagesDirectory, mFoundationImagesRecycleViewAdapter);
        SharedMethods.updateImagesFromFirebaseIfRelevant(mFoundation, mFirebaseDao);
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
                SharedMethods.shrinkImageWithUri(croppedImageTempUri, 300, 300);

                Uri copiedImageUri = SharedMethods.updateImageInLocalDirectoryAndShowIt(getApplicationContext(),
                        croppedImageTempUri, mFoundationImagesDirectory, mImageName, mImageViewMain, mFoundationImagesRecycleViewAdapter);

                if (copiedImageUri !=null) mFirebaseDao.putImageInFirebaseStorage(mFoundation, copiedImageUri, mImageName);
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
                getFoundationProfileFromFirebase();
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
        getMenuInflater().inflate(R.menu.update_my_foundation_menu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {
            case R.id.action_done:
                updateFoundationWithUserInput();
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
        mStoredFoundationImagesRecyclerViewPosition = SharedMethods.getImagesRecyclerViewPosition(mRecyclerViewFoundationImages);
        outState.putInt(SharedMethods.PROFILE_UPDATE_PET_IMAGES_RV_POSITION, mStoredFoundationImagesRecyclerViewPosition);
        outState.putString(SharedMethods.PROFILE_UPDATE_IMAGE_NAME, mImageName);
        super.onSaveInstanceState(outState, outPersistentState);

    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mStoredFoundationImagesRecyclerViewPosition = savedInstanceState.getInt(SharedMethods.PROFILE_UPDATE_PET_IMAGES_RV_POSITION);
            mRecyclerViewFoundationImages.scrollToPosition(mStoredFoundationImagesRecyclerViewPosition);
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
        if (getSupportActionBar()!=null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ButterKnife.bind(this);
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

            //Setting the requested Family's id
            mFirebaseUid = mCurrentFirebaseUser.getUid();
            mFoundation.setOwnerfirebaseUid(mFirebaseUid);

            //Initializing the local parameters that depend on this family, used in the rest of the activity
            mFoundationImagesDirectory = getFilesDir()+"/foundations/"+mFoundation.getUniqueIdentifier()+"/images/";
            mImageName = "mainImage";

            //Getting the rest of the family's parameters
            mFirebaseDao.getUniqueObjectFromFirebaseDb(mFoundation);
        }
    }
    private void updateProfileFieldsOnScreen() {
        mEditTextName.setText(mNameFromFirebase);
        mEditTextContactPhone.setText(mFoundation.getContactPhone());
        mEditTextContactEmail.setText(mFoundation.getContactEmail());
        mEditTextWebsite.setText(mFoundation.getWebsite());
        mEditTextCountry.setText(mFoundation.getCountry());
        mEditTextCity.setText(mFoundation.getCity());
        mEditTextStreet.setText(mFoundation.getStreet());
        mEditTextStreetNumber.setText(mFoundation.getStreetNumber());
    }
    private void setupFoundationImagesRecyclerView() {
        mRecyclerViewFoundationImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true));
        mRecyclerViewFoundationImages.setNestedScrollingEnabled(true);
        mFoundationImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(this, this, SharedMethods.getExistingImageUris(mFoundationImagesDirectory));
        mRecyclerViewFoundationImages.setAdapter(mFoundationImagesRecycleViewAdapter);
    }
    private void setButtonBehaviors() {
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

                if (SharedMethods.getExistingImageUris(mFoundationImagesDirectory).size() == 5) {
                    Toast.makeText(getApplicationContext(), R.string.reached_max_images, Toast.LENGTH_SHORT).show();
                }
                else {
                    mImageName = SharedMethods.getNameOfFirstAvailableImageInImagesList(mFoundationImagesDirectory);
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
                    getFoundationProfileFromFirebase();
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
    private void updateFoundationWithUserInput() {
        mFoundation.setOwnerfirebaseUid(mFirebaseUid);
        mFoundation.setName(mEditTextName.getText().toString());
        mFoundation.setContactPhone(mEditTextContactPhone.getText().toString());
        mFoundation.setContactEmail(mEditTextContactEmail.getText().toString());
        mFoundation.setWebsite(mEditTextWebsite.getText().toString());
        mFoundation.setCountry(mEditTextCountry.getText().toString());
        mFoundation.setCity(mEditTextCity.getText().toString());
        mFoundation.setStreet(mEditTextStreet.getText().toString());
        mFoundation.setStreetNumber(mEditTextStreetNumber.getText().toString());

        mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mFoundation);
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
            if (foundationsList.get(0) != null) mFoundation = foundationsList.get(0);
        }
        else if (foundationsList.size()>1) {
            mFoundation = foundationsList.get(0);
            Toast.makeText(getBaseContext(), "Warning! Multiple foundations found for your entered email.", Toast.LENGTH_SHORT).show();
        }
        else {
            mFoundation = new Foundation(mFirebaseUid);
            Toast.makeText(getBaseContext(), "No user found for your entered email, press DONE to create a new user.", Toast.LENGTH_SHORT).show();
        }

        updateProfileFieldsOnScreen();
        SharedMethods.updateImagesFromFirebaseIfRelevant(mFoundation, mFirebaseDao);
    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onImageAvailable(Uri downloadedImageUri, String imageName) {

        SharedMethods.synchronizeImageOnAllDevices(getApplicationContext(),
                mFoundation, mFirebaseDao, mFoundationImagesDirectory, imageName, downloadedImageUri, mImageViewMain, mFoundationImagesRecycleViewAdapter);
    }
}