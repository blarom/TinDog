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
    @BindView(R.id.profile_update_button_choose_main_pic) Button mButtonChooseMainPic;
    @BindView(R.id.profile_update_button_upload_pics) Button mButtonUploadPics;
    @BindView(R.id.profile_update_button_done) Button mButtonDone;
    @BindView(R.id.profile_update_value_username) TextInputEditText mEditTextUsername;
    @BindView(R.id.profile_update_value_pseudonym) TextInputEditText mEditTextPseudonym;
    @BindView(R.id.profile_update_value_cell) TextInputEditText mEditTextCell;
    @BindView(R.id.profile_update_value_email) TextInputEditText mEditTextEmail;
    @BindView(R.id.profile_update_value_country) TextInputEditText mEditTextCountry;
    @BindView(R.id.profile_update_value_city) TextInputEditText mEditTextCity;
    @BindView(R.id.profile_update_value_street) TextInputEditText mEditTextStreet;
    @BindView(R.id.profile_update_value_my_experience) TextInputEditText mEditTextExperience;
    @BindView(R.id.profile_update_checkbox_foster) CheckBox mCheckBoxFoster;
    @BindView(R.id.profile_update_checkbox_adopt) CheckBox mCheckBoxAdopt;
    @BindView(R.id.profile_update_checkbox_foster_and_adopt) CheckBox mCheckBoxFosterAndAdopt;
    @BindView(R.id.profile_update_checkbox_help_organize) CheckBox mCheckBoxHelpOrganize;
    @BindView(R.id.profile_update_checkbox_dogwalking) CheckBox mCheckBoxDogWalking;
    @BindView(R.id.profile_update_image_main) ImageView mImageViewMain;
    @BindView(R.id.profile_update_recyclerview_pet_images) RecyclerView mRecyclerViewPetImages;
    private Family mFamily;
    private FirebaseDao mFirebaseDao;
    private ImagesRecycleViewAdapter mPetImagesRecycleViewAdapter;
    private String mImageName;
    private int mStoredPetImagesRecyclerViewPosition;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentFirebaseUser;
    private String mNameFromFirebase;
    private String mEmailFromFirebase;
    private Uri mPhotoUriFromFirebase;
    private String mChosenAction;
    private String mFirebaseUid;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    //endregion

    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_my_family);

        getExtras();
        initializeParameters();
        updateProfileFields();
        setupPetImagesRecyclerView();
        showImagesIfAvailable();
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
                android.net.Uri resultUri = result.getUri();
                shrinkImageWithUri(resultUri, 300, 300);
                mFirebaseDao.putImageInFirebaseStorage(mFamily, resultUri, mImageName);
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
        getMenuInflater().inflate(R.menu.profile_update_menu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {
            case R.id.action_done:
                updateFamilyWithUserInput();
                finish();
                return true;
            case R.id.action_edit_user_profile:
                //TODO finish this
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        mStoredPetImagesRecyclerViewPosition = getPetImagesRecyclerViewPosition();
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
        ButterKnife.bind(this);
        mFamily = new Family();
        mFirebaseDao = new FirebaseDao(getBaseContext(), this);
        mFirebaseAuth = FirebaseAuth.getInstance();
    }
    private void getFamilyProfileFromFirebase() {
        if (mCurrentFirebaseUser != null) {
            // Name, email address, and profile photo Url
            mNameFromFirebase = mCurrentFirebaseUser.getDisplayName();
            mEmailFromFirebase = mCurrentFirebaseUser.getEmail();
            mPhotoUriFromFirebase = mCurrentFirebaseUser.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = mCurrentFirebaseUser.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getIdToken() instead.
            mFirebaseUid = mCurrentFirebaseUser.getUid();
            Family family = new Family(mFirebaseUid);
            mFirebaseDao.getUniqueObjectFromFirebaseDb(family);
        }
    }
    private void updateProfileFields() {
        mEditTextUsername.setText(mNameFromFirebase);
        mEditTextUsername.setEnabled(false);
        mEditTextPseudonym.setText(mFamily.getPseudonym());
        mEditTextCell.setText(mFamily.getCell());
        mEditTextEmail.setText(mFamily.getEmail());
        mEditTextEmail.setEnabled(false);
        mEditTextCountry.setText(mFamily.getCountry());
        mEditTextCity.setText(mFamily.getCity());
        mEditTextStreet.setText(mFamily.getStreet());
        mEditTextExperience.setText(mFamily.getExperience());
        mCheckBoxFoster.setChecked(false);
        mCheckBoxAdopt.setChecked(false);
        mCheckBoxFosterAndAdopt.setChecked(false);
        mCheckBoxHelpOrganize.setChecked(false);
        mCheckBoxDogWalking.setChecked(false);
    }
    private void showImagesIfAvailable() {
        mFirebaseDao.getImageFromFirebaseStorage(mFamily, getString(R.string.mainImage));
        mFirebaseDao.getImageFromFirebaseStorage(mFamily, getString(R.string.image0));
        mFirebaseDao.getImageFromFirebaseStorage(mFamily, getString(R.string.image1));
        mFirebaseDao.getImageFromFirebaseStorage(mFamily, getString(R.string.image2));
        mFirebaseDao.getImageFromFirebaseStorage(mFamily, getString(R.string.image3));
        mFirebaseDao.getImageFromFirebaseStorage(mFamily, getString(R.string.image4));
    }
    private void setupPetImagesRecyclerView() {
        mRecyclerViewPetImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true));
        mRecyclerViewPetImages.setNestedScrollingEnabled(true);
        mPetImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(this, this, mFamily.getImageUris());
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
                mImageName = getString(R.string.mainImage);
                performImageCaptureAndCrop();
            }
        });

        mButtonUploadPics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mImageName = getString(R.string.image0);
                for (int i=0; i<mFamily.getImageUris().size(); i++) {
                    if (mFamily.getImageUris().get(i).equals("")) {
                        mImageName = "image" + i;
                        break;
                    }
                }
                performImageCaptureAndCrop();
            }
        });
    }
    private void performImageCaptureAndCrop() {
        // start source picker (camera, gallery, etc..) to get image for cropping and then use the image in cropping activity
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(this);
    }
    private int getPetImagesRecyclerViewPosition() {
        LinearLayoutManager layoutManager = ((LinearLayoutManager) mRecyclerViewPetImages.getLayoutManager());
        return layoutManager.findFirstVisibleItemPosition();
    }
    private void shrinkImageWithUri(Uri uri, int width, int height){

        //inspired by: from: https://stackoverflow.com/questions/16954109/reduce-the-size-of-a-bitmap-to-a-specified-size-in-android

        //If the image is already small, don't change it (file.length()==0 means the image wasn't found)
        File file = new File(uri.getPath());
        while (file.length()/1024 > SharedMethods.MAX_IMAGE_FILE_SIZE) {
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inJustDecodeBounds = true;
            Bitmap bitmap;

            int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight / (float) height);
            int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth / (float) width);

            if (heightRatio > 1 || widthRatio > 1) {
                if (heightRatio > widthRatio) {
                    bmpFactoryOptions.inSampleSize = heightRatio;
                } else {
                    bmpFactoryOptions.inSampleSize = widthRatio;
                }
            }

            bmpFactoryOptions.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(uri.toString(), bmpFactoryOptions);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] imageInByte = stream.toByteArray();

            //this gives the size of the compressed image in kb
            long lengthbmp = imageInByte.length / 1024;

            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(uri.toString()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            height = (int) Math.ceil(height * 0.75);
            height = (int) Math.ceil(height * 0.75);
            file = new File(uri.getPath());
        }

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
                    getFamilyProfileFromFirebase();
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
    }

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {

    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
        if (familiesList.size()==1) {
            mFamily = familiesList.get(0);
            mFamily.setEmail(mEmailFromFirebase);
        }
        else if (familiesList.size()>1) {
            mFamily = familiesList.get(0);
            mFamily.setEmail(mEmailFromFirebase);
            Toast.makeText(getBaseContext(), "Warning! Multiple users found for your entered email.", Toast.LENGTH_SHORT).show();
        }
        else {
            mFamily = new Family(mFirebaseUid);
            mFamily.setEmail(mEmailFromFirebase);
            mFamily.setPseudonym(mNameFromFirebase);
            Toast.makeText(getBaseContext(), "No user found for your entered email, press DONE to create a new user.", Toast.LENGTH_SHORT).show();
        }


        updateProfileFields();
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {

    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onImageAvailable(android.net.Uri imageUri, String imageName) {

        List<String> images;
        switch (imageName) {
            case "mainImage":
                Picasso.with(getBaseContext())
                        .load(imageUri)
                        .error(R.drawable.ic_image_not_available)
                        .into(mImageViewMain);
                mFamily.setMainImageUri(imageUri.toString());
                break;
            case "image0":
                images = mFamily.getImageUris();
                images.set(0,imageUri.toString());
                mFamily.setImageUris(images);
                mPetImagesRecycleViewAdapter.setContents(images);
                break;
            case "image1":
                images = mFamily.getImageUris();
                images.set(1,imageUri.toString());
                mFamily.setImageUris(images);
                mPetImagesRecycleViewAdapter.setContents(images);
                break;
            case "image2":
                images = mFamily.getImageUris();
                images.set(2,imageUri.toString());
                mFamily.setImageUris(images);
                mPetImagesRecycleViewAdapter.setContents(images);
                break;
            case "image3":
                images = mFamily.getImageUris();
                images.set(3,imageUri.toString());
                mFamily.setImageUris(images);
                mPetImagesRecycleViewAdapter.setContents(images);
                break;
            case "image4":
                images = mFamily.getImageUris();
                images.set(4,imageUri.toString());
                mFamily.setImageUris(images);
                mPetImagesRecycleViewAdapter.setContents(images);
                break;
            default:
        }
    }

}
