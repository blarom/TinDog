package com.tindog;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.tindog.adapters.SimpleTextRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.TinDogUser;
import com.tindog.resources.SharedMethods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class UpdateDogActivity extends AppCompatActivity implements
        FirebaseDao.FirebaseOperationsHandler,
        ImagesRecycleViewAdapter.ImageClickHandler,
        AdapterView.OnItemSelectedListener, SimpleTextRecycleViewAdapter.TextClickHandler {

    //region Parameters
    private static final String DEBUG_TAG = "TinDog Update";
    @BindView(R.id.update_dog_button_choose_main_pic) Button mButtonChooseMainPic;
    @BindView(R.id.update_dog_button_upload_pics) Button mButtonUploadPics;
    @BindView(R.id.update_dog_button_add_video_link) Button mButtonAddVideoLink;
    @BindView(R.id.update_dog_value_name) TextInputEditText mEditTextName;
    @BindView(R.id.update_dog_value_foundation) TextInputEditText mEditTextFoundation;
    @BindView(R.id.update_dog_value_country) TextInputEditText mEditTextCountry;
    @BindView(R.id.update_dog_value_city) TextInputEditText mEditTextCity;
    @BindView(R.id.update_dog_value_history) TextInputEditText mEditTextHistory;
    @BindView(R.id.update_dog_image_main) ImageView mImageViewMain;
    @BindView(R.id.update_dog_recyclerview_video_links) RecyclerView mRecyclerViewVideoLinks;
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
    private String mChosenDogId;
    private String mFirebaseUid;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private String mDogImagesDirectory;
    private boolean mDogSaved;
    private boolean mCreatedDog;
    private Unbinder mBinding;
    private SimpleTextRecycleViewAdapter mVideoLinksRecycleViewAdapter;
    private List<String> mVideoLinks;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_dog);

        getExtras();
        initializeParameters();
        getDogProfileFromFirebase();
        setupVideoLinksRecyclerView();
        setupDogImagesRecyclerView();
        SharedMethods.refreshMainImageShownToUser(getApplicationContext(), mDogImagesDirectory, mImageViewMain);
        SharedMethods.updateImagesFromFirebaseIfRelevant(mDog, mFirebaseDao);
        setButtonBehaviors();
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
        removeListeners();
    }
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri croppedImageTempUri = result.getUri();
                boolean succeeded = SharedMethods.shrinkImageWithUri(getApplicationContext(), croppedImageTempUri, 300, 300);

                if (succeeded) {
                    Uri copiedImageUri = SharedMethods.updateImageInLocalDirectory(croppedImageTempUri, mDogImagesDirectory, mImageName);
                    SharedMethods.displayImages(getApplicationContext(), mDogImagesDirectory, mImageName, mImageViewMain, mDogImagesRecycleViewAdapter);

                    if (copiedImageUri != null)
                        mFirebaseDao.putImageInFirebaseStorage(mDog, copiedImageUri, mImageName);
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
                if (!mCreatedDog) getDogProfileFromFirebase();
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
        getMenuInflater().inflate(R.menu.update_dog_menu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.action_save:
                updateDogWithUserInput();
                return true;
            case R.id.action_done:
                updateDogWithUserInput();
                if (mDogSaved) finish();
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
        if (getIntent().hasExtra(SharedMethods.DOG_ID)) {
            mChosenDogId = intent.getStringExtra(SharedMethods.DOG_ID);
        }
    }
    private void initializeParameters() {
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.dog_profile));
        }

        mBinding =  ButterKnife.bind(this);
        mCreatedDog = false;
        mDogSaved = false;
        mEditTextFoundation.setEnabled(false);
        mDog = new Dog();
        mVideoLinks = new ArrayList<>();

        mFirebaseDao = new FirebaseDao(getBaseContext(), this);
        mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mSpinnerAdapterAge = ArrayAdapter.createFromResource(this, R.array.dog_age_simple, android.R.layout.simple_spinner_item);
        mSpinnerAdapterAge.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerAge.setAdapter(mSpinnerAdapterAge);
        mSpinnerAge.setOnItemSelectedListener(this);

        mSpinnerAdapterSize = ArrayAdapter.createFromResource(this, R.array.dog_size_simple, android.R.layout.simple_spinner_item);
        mSpinnerAdapterSize.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerSize.setAdapter(mSpinnerAdapterSize);
        mSpinnerSize.setOnItemSelectedListener(this);

        mSpinnerAdapterGender = ArrayAdapter.createFromResource(this, R.array.dog_gender_simple, android.R.layout.simple_spinner_item);
        mSpinnerAdapterGender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerGender.setAdapter(mSpinnerAdapterGender);
        mSpinnerGender.setOnItemSelectedListener(this);

        mSpinnerAdapterRace = ArrayAdapter.createFromResource(this, R.array.dog_race_simple, android.R.layout.simple_spinner_item);
        mSpinnerAdapterRace.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerRace.setAdapter(mSpinnerAdapterRace);
        mSpinnerRace.setOnItemSelectedListener(this);

        mSpinnerAdapterBehavior = ArrayAdapter.createFromResource(this, R.array.dog_behavior_simple, android.R.layout.simple_spinner_item);
        mSpinnerAdapterBehavior.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerBehavior.setAdapter(mSpinnerAdapterBehavior);
        mSpinnerBehavior.setOnItemSelectedListener(this);

        mSpinnerAdapterInteractions = ArrayAdapter.createFromResource(this, R.array.dog_interactions_simple, android.R.layout.simple_spinner_item);
        mSpinnerAdapterInteractions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerInteractions.setAdapter(mSpinnerAdapterInteractions);
        mSpinnerInteractions.setOnItemSelectedListener(this);

    }
    private void getDogProfileFromFirebase() {
        if (mCurrentFirebaseUser != null && !mCreatedDog) {
            // Name, email address, and profile photo Url
            mNameFromFirebase = mCurrentFirebaseUser.getDisplayName();
            mEmailFromFirebase = mCurrentFirebaseUser.getEmail();
            mPhotoUriFromFirebase = mCurrentFirebaseUser.getPhotoUrl();
            mFirebaseUid = mCurrentFirebaseUser.getUid();

            //Setting the requested Dog's id
            mDog.setUI(mChosenDogId);

            //Initializing the local parameters that depend on this dog, used in the rest of the activity
            mImageName = "mainImage";

            //Getting the foundation details
            mFirebaseDao.getUniqueObjectFromFirebaseDb(new Foundation(mFirebaseUid));

            //Getting the rest of the dog's parameters
            if (!TextUtils.isEmpty(mChosenDogId)) {
                mDogImagesDirectory = getFilesDir()+"/dogs/"+ mDog.getUI()+"/images/";
                mFirebaseDao.getUniqueObjectFromFirebaseDb(mDog);
            }
            else {
                if (!mCreatedDog) createNewDogInFirebaseDb();
            }
        }
    }
    private void createNewDogInFirebaseDb() {
        mDog = new Dog();
        String key = mFirebaseDao.addObjectToFirebaseDb(mDog);
        mDog.setUI(key);
        mDog.setAFid(mFirebaseUid);
        mDogImagesDirectory = getFilesDir()+"/dogs/"+ mDog.getUI()+"/images/";
        mCreatedDog = true;
    }
    private void updateProfileFieldsOnScreen() {
        mEditTextFoundation.setText(mDog.getFN());
        mEditTextName.setText(mDog.getNm());
        mEditTextCountry.setText(mDog.getCn());
        mEditTextCity.setText(mDog.getCt());
        mEditTextHistory.setText(mDog.getHs());

        mSpinnerAge.setSelection(getSpinnerIndex(mSpinnerAge, mDog.getAg()));
        mSpinnerSize.setSelection(getSpinnerIndex(mSpinnerSize, mDog.getSz()));
        mSpinnerGender.setSelection(getSpinnerIndex(mSpinnerGender, mDog.getGn()));
        mSpinnerRace.setSelection(getSpinnerIndex(mSpinnerRace, mDog.getRc()));
        mSpinnerBehavior.setSelection(getSpinnerIndex(mSpinnerBehavior, mDog.getBh()));
        mSpinnerInteractions.setSelection(getSpinnerIndex(mSpinnerInteractions, mDog.getIt()));

        mVideoLinksRecycleViewAdapter.setContents(mDog.getVU());

        SharedMethods.hideSoftKeyboard(this);
    }
    private int getSpinnerIndex(Spinner spinner, String myString){

        int index = 0;

        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).equals(myString)){
                index = i;
                break;
            }
        }
        return index;
    }
    private void setupVideoLinksRecyclerView() {
        mRecyclerViewVideoLinks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
        mRecyclerViewVideoLinks.setNestedScrollingEnabled(false);
        mVideoLinksRecycleViewAdapter = new SimpleTextRecycleViewAdapter(this, this, null);
        mRecyclerViewVideoLinks.setAdapter(mVideoLinksRecycleViewAdapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                mVideoLinks = mDog.getVU();
                mVideoLinks.remove(viewHolder.getLayoutPosition());
                mVideoLinksRecycleViewAdapter.setContents(mVideoLinks);
                //mVideoLinksRecycleViewAdapter.notifyItemRemoved(viewHolder.getLayoutPosition());
                mDog.setVU(mVideoLinks);
            }

        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerViewVideoLinks);
    }
    private void setupDogImagesRecyclerView() {
        mRecyclerViewDogImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true));
        mRecyclerViewDogImages.setNestedScrollingEnabled(true);
        mDogImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(this, this, SharedMethods.getExistingImageUris(mDogImagesDirectory, true));
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

                if (SharedMethods.getExistingImageUris(mDogImagesDirectory, true).size() == 5) {
                    Toast.makeText(getApplicationContext(), R.string.reached_max_images, Toast.LENGTH_SHORT).show();
                }
                else {
                    mImageName = SharedMethods.getNameOfFirstAvailableImageInImagesList(mDogImagesDirectory);
                    performImageCaptureAndCrop();
                }
            }
        });
        mButtonAddVideoLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showVideoLinkDialog();
            }
        });
    }
    private void showVideoLinkDialog() {

        //Get the dialog view
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_enter_video_link, null);
        final EditText inputText = dialogView.findViewById(R.id.input_text_video_link);

        //Building the dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.add_video_link);
        inputText.setText("");

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                List<String> videoUrls = mDog.getVU();
                videoUrls.add(inputText.getText().toString());
                mVideoLinksRecycleViewAdapter.setContents(videoUrls);
                mDog.setVU(videoUrls);

                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) builder.setView(dialogView);
        else builder.setMessage(R.string.device_version_too_low);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
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
                    //getDogProfileFromFirebase();
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
    private void updateDogWithUserInput() {

        mDog.setFN(mEditTextFoundation.getText().toString());
        mDog.setAFid(mFirebaseUid);
        mDog.setNm(mEditTextName.getText().toString());
        mDog.setCn(mEditTextCountry.getText().toString());
        mDog.setCt(mEditTextCity.getText().toString());
        mDog.setHs(mEditTextHistory.getText().toString());

        mDog.setAg(mSpinnerAge.getSelectedItem().toString());
        mDog.setSz(mSpinnerSize.getSelectedItem().toString());
        mDog.setGn(mSpinnerGender.getSelectedItem().toString());
        mDog.setRc(mSpinnerRace.getSelectedItem().toString());
        mDog.setBh(mSpinnerBehavior.getSelectedItem().toString());
        mDog.setIt(mSpinnerInteractions.getSelectedItem().toString());

        if (mEditTextName.getText().toString().length() < 2
                || mEditTextCountry.getText().toString().length() < 2
                || mEditTextCity.getText().toString().length() < 2) {
            Toast.makeText(getApplicationContext(), R.string.dog_not_saved, Toast.LENGTH_SHORT).show();
            mDogSaved = false;
        }
        else {
            mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mDog);
            mDogSaved = true;
        }

    }
    private void updateProfileFieldsWithFoundationData(Foundation foundation) {
        mEditTextFoundation.setText(foundation.getNm());
        if (mEditTextCity.getText().toString().equals("")) mEditTextCity.setText(foundation.getCt());
        if (mEditTextCountry.getText().toString().equals("")) mEditTextCountry.setText(foundation.getCn());
    }
    private void removeListeners() {
        mFirebaseDao.removeListeners();
        if (mSpinnerAge!=null) mSpinnerAge.setOnItemSelectedListener(null);
        if (mSpinnerSize!=null) mSpinnerSize.setOnItemSelectedListener(null);
        if (mSpinnerGender!=null) mSpinnerGender.setOnItemSelectedListener(null);
        if (mSpinnerRace!=null) mSpinnerRace.setOnItemSelectedListener(null);
        if (mSpinnerBehavior!=null) mSpinnerBehavior.setOnItemSelectedListener(null);
        if (mSpinnerInteractions!=null) mSpinnerInteractions.setOnItemSelectedListener(null);
        if (mButtonChooseMainPic!=null) mButtonChooseMainPic.setOnClickListener(null);
        if (mButtonUploadPics!=null) mButtonUploadPics.setOnClickListener(null);
    }


    //Communication with other activities/fragments:

    //Communication with ImagesRecyclerView adapter
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

    //Communication with VideoLinkRecyclerView adapter
    @Override public void onTextClick(int clickedItemIndex) {
        //TODO make the link open youtube or a media player
    }

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {
        if (dogsList.size()==1) {
            if (dogsList.get(0) != null) mDog = dogsList.get(0);
            mCreatedDog = true;
        }
        else if (dogsList.size()>1) {
            //Get the first dog that corresponds to the Foundation's Firebase Id
            for (Dog dog : dogsList) {
                if (dog.getAFid().equals(mFirebaseUid)) {
                    mDog = dog;
                    break;
                }
            }
            mCreatedDog = true;
            Log.i(DEBUG_TAG, "Warning! Multiple dogs found with the same characteristics.");
        }
        else {
            if (!mCreatedDog) createNewDogInFirebaseDb();
            Toast.makeText(getBaseContext(), "No dog found for your foundation, press DONE to create a new dog.", Toast.LENGTH_SHORT).show();
        }

        updateProfileFieldsOnScreen();
        SharedMethods.updateImagesFromFirebaseIfRelevant(mDog, mFirebaseDao);
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {

        if (foundationsList.size()==1) {
            if (foundationsList.get(0) != null) updateProfileFieldsWithFoundationData(foundationsList.get(0));
        }
        else if (foundationsList.size()>1) {
            updateProfileFieldsWithFoundationData(foundationsList.get(0));
            Log.i(DEBUG_TAG, "Warning! Multiple foundations found with the same id.");
        }
        else {
            Log.i(DEBUG_TAG, "Warning! No foundation found with the required id.");
        }

    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onImageAvailable(Uri downloadedImageUri, String imageName) {

        SharedMethods.synchronizeImageOnAllDevices(mDog, mFirebaseDao, mDogImagesDirectory, imageName, downloadedImageUri);
        SharedMethods.displayImages(getApplicationContext(), mDogImagesDirectory, imageName, mImageViewMain, mDogImagesRecycleViewAdapter);
    }
    @Override public void onImageUploaded(List<String> uploadTimes) {
        mDog.setIUT(uploadTimes);
    }
    
    //Communication with spinner adapters
    @Override public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        switch (adapterView.getId()) {
            case R.id.update_dog_age_spinner:
                mDog.setAg((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_dog_size_spinner:
                mDog.setSz((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_dog_gender_spinner:
                mDog.setGn((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_dog_race_spinner:
                mDog.setRc((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_dog_behavior_spinner:
                mDog.setBh((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.update_dog_interactions_spinner:
                mDog.setIt((String) adapterView.getItemAtPosition(pos));
                break;
        }
    }
    @Override public void onNothingSelected(AdapterView<?> adapterView) {

    }

}
