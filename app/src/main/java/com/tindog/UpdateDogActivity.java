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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
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

public class UpdateDogActivity extends AppCompatActivity implements
        FirebaseDao.FirebaseOperationsHandler,
        ImagesRecycleViewAdapter.ImageClickHandler,
        AdapterView.OnItemSelectedListener {

    //region Parameters
    private static final String DEBUG_TAG = "TinDog Update";
    @BindView(R.id.update_dog_button_choose_main_pic) Button mButtonChooseMainPic;
    @BindView(R.id.update_dog_button_upload_pics) Button mButtonUploadPics;
    @BindView(R.id.update_dog_value_name) TextInputEditText mEditTextName;
    @BindView(R.id.update_dog_value_foundation) TextInputEditText mEditTextFoundation;
    @BindView(R.id.update_dog_value_country) TextInputEditText mEditTextCountry;
    @BindView(R.id.update_dog_value_city) TextInputEditText mEditTextCity;
    @BindView(R.id.update_dog_value_history) TextInputEditText mEditTextHistory;
    @BindView(R.id.update_dog_image_main) ImageView mImageViewMain;
    @BindView(R.id.update_dog_recyclerview_images) RecyclerView mRecyclerViewDogImages;
    @BindView(R.id.update_dog_age_spinner) Spinner mSpinnerAge;
    @BindView(R.id.update_dog_size_spinner) Spinner mSpinnerSize;
    @BindView(R.id.update_dog_gender_spinner) Spinner mSpinnerGender;
    @BindView(R.id.update_dog_race_spinner) Spinner mSpinnerRace;
    @BindView(R.id.update_dog_behavior_spinner) Spinner mSpinnerBehavior;
    @BindView(R.id.update_dog_interactions_spinner) Spinner mSpinnerInteractions;
    private ArrayAdapter<CharSequence> mSpinnerAdapterAge;
    private ArrayAdapter<CharSequence> mSpinnerAdapterSize;
    private ArrayAdapter<CharSequence> mSpinnerAdapterGender;
    private ArrayAdapter<CharSequence> mSpinnerAdapterRace;
    private ArrayAdapter<CharSequence> mSpinnerAdapterBehavior;
    private ArrayAdapter<CharSequence> mSpinnerAdapterInteractions;
    private Dog mDog;
    private FirebaseDao mFirebaseDao;
    private ImagesRecycleViewAdapter mDogImagesRecycleViewAdapter;
    private String mImageName = "mainImage";
    private int mStoredDogImagesRecyclerViewPosition;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentFirebaseUser;
    private String mNameFromFirebase;
    private String mEmailFromFirebase;
    private Uri mPhotoUriFromFirebase;
    private String mChosenAction;
    private String mFirebaseUid;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private String mDogImagesDirectory;
    //endregion


    //Lifecycle methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_my_foundation);

        getExtras();
        initializeParameters();
        getDogProfileFromFirebase();
        updateProfileFieldsOnScreen();
        setupDogImagesRecyclerView();
        SharedMethods.refreshMainImageShownToUser(getApplicationContext(), mDogImagesDirectory, mImageViewMain);
        SharedMethods.refreshImagesListShownToUser(mDogImagesDirectory, mDogImagesRecycleViewAdapter);
        SharedMethods.updateImagesFromFirebaseIfRelevant(mDog, mFirebaseDao);
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
                        croppedImageTempUri, mDogImagesDirectory, mImageName, mImageViewMain, mDogImagesRecycleViewAdapter);

                if (copiedImageUri !=null) mFirebaseDao.putImageInFirebaseStorage(mDog, copiedImageUri, mImageName);
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
                getDogProfileFromFirebase();
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
                updateDogWithUserInput();
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
        mStoredDogImagesRecyclerViewPosition = SharedMethods.getImagesRecyclerViewPosition(mRecyclerViewDogImages);
        outState.putInt(SharedMethods.PROFILE_UPDATE_PET_IMAGES_RV_POSITION, mStoredDogImagesRecyclerViewPosition);
        outState.putString(SharedMethods.PROFILE_UPDATE_IMAGE_NAME, mImageName);
        super.onSaveInstanceState(outState, outPersistentState);

    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mStoredDogImagesRecyclerViewPosition = savedInstanceState.getInt(SharedMethods.PROFILE_UPDATE_PET_IMAGES_RV_POSITION);
            mRecyclerViewDogImages.scrollToPosition(mStoredDogImagesRecyclerViewPosition);
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
        mDog = new Dog();
        mFirebaseDao = new FirebaseDao(getBaseContext(), this);
        mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mSpinnerAdapterAge = ArrayAdapter.createFromResource(this, R.array.dog_age, android.R.layout.simple_spinner_item);
        mSpinnerAdapterAge.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerAge.setAdapter(mSpinnerAdapterAge);
        mSpinnerAge.setOnItemSelectedListener(this);

        mSpinnerAdapterSize = ArrayAdapter.createFromResource(this, R.array.dog_size, android.R.layout.simple_spinner_item);
        mSpinnerAdapterSize.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerSize.setAdapter(mSpinnerAdapterSize);
        mSpinnerSize.setOnItemSelectedListener(this);

        mSpinnerAdapterGender = ArrayAdapter.createFromResource(this, R.array.dog_gender, android.R.layout.simple_spinner_item);
        mSpinnerAdapterGender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerGender.setAdapter(mSpinnerAdapterGender);
        mSpinnerGender.setOnItemSelectedListener(this);

        mSpinnerAdapterRace = ArrayAdapter.createFromResource(this, R.array.dog_race, android.R.layout.simple_spinner_item);
        mSpinnerAdapterRace.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerRace.setAdapter(mSpinnerAdapterRace);
        mSpinnerRace.setOnItemSelectedListener(this);

        mSpinnerAdapterBehavior = ArrayAdapter.createFromResource(this, R.array.dog_behavior, android.R.layout.simple_spinner_item);
        mSpinnerAdapterBehavior.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerBehavior.setAdapter(mSpinnerAdapterBehavior);
        mSpinnerBehavior.setOnItemSelectedListener(this);

        mSpinnerAdapterInteractions = ArrayAdapter.createFromResource(this, R.array.dog_interactions, android.R.layout.simple_spinner_item);
        mSpinnerAdapterInteractions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerInteractions.setAdapter(mSpinnerAdapterInteractions);
        mSpinnerInteractions.setOnItemSelectedListener(this);
    }
    private void getDogProfileFromFirebase() {
        if (mCurrentFirebaseUser != null) {
            // Name, email address, and profile photo Url
            mNameFromFirebase = mCurrentFirebaseUser.getDisplayName();
            mEmailFromFirebase = mCurrentFirebaseUser.getEmail();
            mPhotoUriFromFirebase = mCurrentFirebaseUser.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = mCurrentFirebaseUser.isEmailVerified();

            //Setting the requested Family's id
            mFirebaseUid = mCurrentFirebaseUser.getUid();
            mDog.setOwnerfirebaseUid(mFirebaseUid);

            //Initializing the local parameters that depend on this family, used in the rest of the activity
            mDogImagesDirectory = getFilesDir()+"/foundations/"+ mDog.getUniqueIdentifier()+"/images/";
            mImageName = "mainImage";

            //Getting the rest of the family's parameters
            mFirebaseDao.getUniqueObjectFromFirebaseDb(mDog);
        }
    }
    private void updateProfileFieldsOnScreen() {
        mEditTextFoundation.setText(mDog.getAssociatedFoundationName());
        mEditTextName.setText(mDog.getName());
        mEditTextCountry.setText(mDog.getCountry());
        mEditTextCity.setText(mDog.getCity());
        mEditTextHistory.setText(mDog.getHistory());
    }
    private void setupDogImagesRecyclerView() {
        mRecyclerViewDogImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true));
        mRecyclerViewDogImages.setNestedScrollingEnabled(true);
        mDogImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(this, this, SharedMethods.getExistingImageUris(mDogImagesDirectory));
        mRecyclerViewDogImages.setAdapter(mDogImagesRecycleViewAdapter);
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

                if (SharedMethods.getExistingImageUris(mDogImagesDirectory).size() == 5) {
                    Toast.makeText(getApplicationContext(), R.string.reached_max_images, Toast.LENGTH_SHORT).show();
                }
                else {
                    mImageName = SharedMethods.getNameOfFirstAvailableImageInImagesList(mDogImagesDirectory);
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
                    getDogProfileFromFirebase();
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
    private void updateDogWithUserInput() {
        mDog.setOwnerfirebaseUid(mFirebaseUid);
        mDog.setName(mEditTextName.getText().toString());
        mDog.setCountry(mEditTextCountry.getText().toString());
        mDog.setCity(mEditTextCity.getText().toString());
        mDog.setHistory(mEditTextHistory.getText().toString());

        mDog.setAge(mSpinnerAge.getSelectedItem().toString());
        mDog.setSize(mSpinnerSize.getSelectedItem().toString());
        mDog.setGender(mSpinnerGender.getSelectedItem().toString());
        mDog.setRace(mSpinnerAge.getSelectedItem().toString());
        mDog.setBehavior(mSpinnerBehavior.getSelectedItem().toString());
        mDog.setInteractions(mSpinnerInteractions.getSelectedItem().toString());

        mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mDog);
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
        if (dogsList.size()==1) {
            if (dogsList.get(0) != null) mDog = dogsList.get(0);
        }
        else if (dogsList.size()>1) {
            mDog = dogsList.get(0);
            Toast.makeText(getBaseContext(), "Warning! Multiple dogs found with the same characteristics.", Toast.LENGTH_SHORT).show();
        }
        else {
            mDog = new Dog(mFirebaseUid);
            Toast.makeText(getBaseContext(), "No dog found for your foundation, press DONE to create a new dog.", Toast.LENGTH_SHORT).show();
        }

        updateProfileFieldsOnScreen();
        SharedMethods.updateImagesFromFirebaseIfRelevant(mDog, mFirebaseDao);
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {
    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onImageAvailable(Uri downloadedImageUri, String imageName) {

        SharedMethods.synchronizeImageOnAllDevices(getApplicationContext(),
                mDog, mFirebaseDao, mDogImagesDirectory, imageName, downloadedImageUri, mImageViewMain, mDogImagesRecycleViewAdapter);
    }
    
    //Communication with spinner adapters
    @Override public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        switch (adapterView.getId()) {
            case R.id.update_dog_age_spinner:
                mDog.setAge((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_dog_size_spinner:
                mDog.setSize((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_dog_gender_spinner:
                mDog.setGender((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_dog_race_spinner:
                mDog.setRace((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_dog_behavior_spinner:
                mDog.setBehavior((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_dog_interactions_spinner:
                mDog.setInteractions((String) adapterView.getItemAtPosition(pos));
                break;
        }
    }
    @Override public void onNothingSelected(AdapterView<?> adapterView) {

    }
}