package com.tindog.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.widget.NestedScrollView;
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

import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.tindog.R;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.adapters.SimpleTextRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.MapMarker;
import com.tindog.data.TinDogUser;
import com.tindog.resources.Utilities;

import java.util.ArrayList;
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
    @BindView(R.id.update_dog_value_state) TextInputEditText mEditTextState;
    @BindView(R.id.update_dog_value_city) TextInputEditText mEditTextCity;
    @BindView(R.id.update_dog_value_street) TextInputEditText mEditTextStreet;
    @BindView(R.id.update_dog_value_street_number) TextInputEditText mEditTextStreetNumber;
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
    @BindView(R.id.update_dog_scroll_container) NestedScrollView mScrollViewContainer;
    private ArrayAdapter<CharSequence> mSpinnerAdapterAge;
    private ArrayAdapter<CharSequence> mSpinnerAdapterSize;
    private ArrayAdapter<CharSequence> mSpinnerAdapterGender;
    private ArrayAdapter<CharSequence> mSpinnerAdapterRace;
    private ArrayAdapter<CharSequence> mSpinnerAdapterBehavior;
    private ArrayAdapter<CharSequence> mSpinnerAdapterInteractions;
    private int mAgeSpinnerPosition;
    private int mSizeSpinnerPosition;
    private int mGenderSpinnerPosition;
    private int mRaceSpinnerPosition;
    private int mBehaviorSpinnerPosition;
    private int mInteractionsSpinnerPosition;
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
    private boolean mDogCriticalParametersSet;
    private boolean mDogAlreadyExistsInFirebaseDb;
    private Unbinder mBinding;
    private SimpleTextRecycleViewAdapter mVideoLinksRecycleViewAdapter;
    private List<String> mVideoLinks;
    private boolean[] mImagesReady;
    private Bundle mSavedInstanceState;
    private String mFoundationName;
    private String mFoundationCity;
    private String mFoundationCountry;
    private String mFoundationStreet;
    private String mFoundationStreetNumber;
    private int mScrollPosition;
    private String mFoundationId;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_dog);

        mSavedInstanceState = savedInstanceState;
        getExtras();
        initializeParameters();
        if (savedInstanceState==null) getDogProfileFromFirebase();
        setupVideoLinksRecyclerView();
        setupDogImagesRecyclerView();
        Utilities.displayObjectImageInImageView(getApplicationContext(), mDog, "mainImage", mImageViewMain);
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
                boolean succeeded = Utilities.shrinkImageWithUri(getApplicationContext(), croppedImageTempUri, 300, 300);

                if (succeeded) {
                    Uri copiedImageUri = Utilities.updateLocalObjectImage(getApplicationContext(), croppedImageTempUri, mDog, mImageName);
                    if (copiedImageUri!=null) {
                        if (mImageName.equals("mainImage")) {
                            Utilities.displayObjectImageInImageView(getApplicationContext(), mDog, "mainImage", mImageViewMain);
                        }
                        else {
                            List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mDog, true);
                            mDogImagesRecycleViewAdapter.setContents(uris);
                        }
                        mFirebaseDao.putImageInFirebaseStorage(mDog, copiedImageUri, mImageName);
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
                if (!mDogAlreadyExistsInFirebaseDb) getDogProfileFromFirebase();
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
                setResult(Activity.RESULT_OK, new Intent());
                this.finish();
                return true;
            case R.id.action_save:
                updateDogWithUserInput();
                if (mDogCriticalParametersSet) {
                    if (!mDogAlreadyExistsInFirebaseDb) {
                        createNewDogInFirebaseDb();
                        mDogAlreadyExistsInFirebaseDb = true;
                    }
                    if (!TextUtils.isEmpty(mFirebaseUid)) mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mDog, true);
                }
                return true;
            case R.id.action_done:
                updateDogWithUserInput();
                if (mDogCriticalParametersSet) {
                    if (!mDogAlreadyExistsInFirebaseDb) {
                        createNewDogInFirebaseDb();
                        mDogAlreadyExistsInFirebaseDb = true;
                    }
                    else if (!TextUtils.isEmpty(mFirebaseUid)) mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mDog, true);
                    setResult(Activity.RESULT_OK, new Intent());
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override public void onSaveInstanceState(Bundle outState) {
        mStoredDogImagesRecyclerViewPosition = Utilities.getImagesRecyclerViewPosition(mRecyclerViewDogImages);
        mScrollPosition = mScrollViewContainer.getScrollY();
        outState.putInt(getString(R.string.scroll_position),mScrollPosition);
        outState.putInt(getString(R.string.profile_update_pet_images_rv_position), mStoredDogImagesRecyclerViewPosition);
        outState.putString(getString(R.string.profile_update_image_name), mImageName);
        outState.putString(getString(R.string.saved_foundation_name), mFoundationName);
        outState.putString(getString(R.string.saved_foundation_city), mFoundationCity);
        outState.putString(getString(R.string.saved_foundation_id), mFoundationId);
        outState.putString(getString(R.string.saved_foundation_country), mFoundationCountry);
        outState.putString(getString(R.string.saved_foundation_street), mFoundationStreet);
        outState.putString(getString(R.string.saved_foundation_street_number), mFoundationStreetNumber);
        outState.putBoolean(getString(R.string.critical_parameters_set), mDogCriticalParametersSet);
        updateDogWithUserInput();
        outState.putParcelable(getString(R.string.saved_profile), mDog);
        super.onSaveInstanceState(outState);

    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mStoredDogImagesRecyclerViewPosition = savedInstanceState.getInt(getString(R.string.profile_update_pet_images_rv_position));
            mRecyclerViewDogImages.scrollToPosition(mStoredDogImagesRecyclerViewPosition);
            mImageName = savedInstanceState.getString(getString(R.string.profile_update_image_name));
            mFoundationName = savedInstanceState.getString(getString(R.string.saved_foundation_name));
            mFoundationCity = savedInstanceState.getString(getString(R.string.saved_foundation_city));
            mFoundationId = savedInstanceState.getString(getString(R.string.saved_foundation_id));
            mFoundationCountry = savedInstanceState.getString(getString(R.string.saved_foundation_country));
            mFoundationStreet = savedInstanceState.getString(getString(R.string.saved_foundation_street));
            mFoundationStreetNumber = savedInstanceState.getString(getString(R.string.saved_foundation_street_number));
            mDog = savedInstanceState.getParcelable(getString(R.string.saved_profile));
            mScrollPosition = savedInstanceState.getInt(getString(R.string.scroll_position));
            mDogCriticalParametersSet = savedInstanceState.getBoolean(getString(R.string.critical_parameters_set));

            mScrollViewContainer.setScrollY(mScrollPosition);
            updateProfileFieldsWithFoundationData();
            updateProfileFieldsOnScreen();
            setupDogImagesRecyclerView();
            Utilities.displayObjectImageInImageView(getApplicationContext(), mDog, "mainImage", mImageViewMain);
        }
    }


    //Structural methods
    private void getExtras() {
        Intent intent = getIntent();
        if (getIntent().hasExtra(getString(R.string.selected_dog_id))) {
            mChosenDogId = intent.getStringExtra(getString(R.string.selected_dog_id));
        }
    }
    private void initializeParameters() {
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.dog_profile));
        }

        mBinding =  ButterKnife.bind(this);
        mDogAlreadyExistsInFirebaseDb = false;
        mDogCriticalParametersSet = false;
        mImagesReady = new boolean[]{false, false, false, false, false, false};
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
        if (mCurrentFirebaseUser != null && !mDogAlreadyExistsInFirebaseDb) {
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
            mFirebaseDao.getUniqueObjectFromFirebaseDbOrCreateIt(new Foundation(mFirebaseUid), true);

            //Getting the rest of the dog's parameters
            if (!TextUtils.isEmpty(mChosenDogId)) {
                mFirebaseDao.getUniqueObjectFromFirebaseDbOrCreateIt(mDog, true);
            }
        }
    }
    private void createNewDogInFirebaseDb() {
        String key = mFirebaseDao.addObjectToFirebaseDb(mDog);
        mDog.setUI(key);
        mDogAlreadyExistsInFirebaseDb = true;
    }
    private void updateProfileFieldsOnScreen() {
        //mEditTextFoundation.setText(mDog.getFN());
        mEditTextName.setText(mDog.getNm());
        mEditTextCountry.setText(mDog.getCn());
        mEditTextState.setText(mDog.getSe());
        mEditTextCity.setText(mDog.getCt());
        mEditTextStreet.setText(mDog.getSt());
        mEditTextStreetNumber.setText(mDog.getStN());
        mEditTextHistory.setText(mDog.getHs());

        mAgeSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerAge, mDog.getAg());
        mSizeSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerSize, mDog.getSz());
        mGenderSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerGender, mDog.getGn());
        mRaceSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerRace, mDog.getRc());
        mBehaviorSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerBehavior, mDog.getBh());
        mInteractionsSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerInteractions, mDog.getIt());

        mSpinnerAge.setSelection(mAgeSpinnerPosition);
        mSpinnerSize.setSelection(mSizeSpinnerPosition);
        mSpinnerGender.setSelection(mGenderSpinnerPosition);
        mSpinnerRace.setSelection(mRaceSpinnerPosition);
        mSpinnerBehavior.setSelection(mBehaviorSpinnerPosition);
        mSpinnerInteractions.setSelection(mInteractionsSpinnerPosition);

        mVideoLinksRecycleViewAdapter.setContents(mDog.getVU());

        Utilities.hideSoftKeyboard(this);
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
        mRecyclerViewDogImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mRecyclerViewDogImages.setNestedScrollingEnabled(true);
        List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mDog, true);
        mDogImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(this, this, uris);
        mRecyclerViewDogImages.setAdapter(mDogImagesRecycleViewAdapter);
    }
    private void setButtonBehaviors() {
        mButtonChooseMainPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mDogCriticalParametersSet && !TextUtils.isEmpty(mDog.getUI())) {
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

                if (mDogCriticalParametersSet && !TextUtils.isEmpty(mDog.getUI())) {

                    List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mDog, true);
                    if (uris.size() == 5) {
                        Toast.makeText(getApplicationContext(), R.string.reached_max_images, Toast.LENGTH_SHORT).show();
                    }
                    else {
                        mImageName = Utilities.getNameOfFirstAvailableImageInImagesList(getApplicationContext(), mDog);
                        if (!TextUtils.isEmpty(mImageName)) performImageCaptureAndCrop();
                        else Toast.makeText(getApplicationContext(), R.string.error_processing_request, Toast.LENGTH_SHORT).show();
                    }

                }
                else {
                    Toast.makeText(getApplicationContext(), R.string.must_save_profile_first, Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

        AlertDialog dialog = builder.create();
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
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_in:" + mCurrentFirebaseUser.getUid());
                } else {
                    // TinDogUser is signed out
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_out");
                    //Showing the sign-in screen
                    if (Utilities.getAppPreferenceUserHasNotRefusedSignIn(getApplicationContext())) {
                        mSavedInstanceState = null;
                        Utilities.showSignInScreen(UpdateDogActivity.this);
                    }
                }
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private void updateDogWithUserInput() {

        mDog.setFN(mEditTextFoundation.getText().toString());
        mDog.setAFid(mFoundationId);

        String name = mEditTextName.getText().toString();
        String country = mEditTextCountry.getText().toString();
        String state = mEditTextState.getText().toString();
        String city = mEditTextCity.getText().toString();
        String street = mEditTextStreet.getText().toString();
        String streeNumber = mEditTextStreetNumber.getText().toString();

        mDog.setNm(name);
        mDog.setCn(country);
        mDog.setSe(state);
        mDog.setCt(city);
        mDog.setSt(street);
        mDog.setStN(streeNumber);

        String addressString = Utilities.getAddressStringFromComponents(streeNumber, street, city, state, country);
        Address address = Utilities.getAddressObjectFromAddressString(this, addressString);
        if (address!=null) {
            String geoAddressCountry = address.getCountryCode();
            double geoAddressLatitude = address.getLatitude();
            double geoAddressLongitude = address.getLongitude();

            mDog.setGaC(geoAddressCountry);
            mDog.setGaLt(Double.toString(geoAddressLatitude));
            mDog.setGaLg(Double.toString(geoAddressLongitude));
        }

        mDog.setHs(mEditTextHistory.getText().toString());

        mDog.setAg(mSpinnerAge.getSelectedItem().toString());
        mDog.setSz(mSpinnerSize.getSelectedItem().toString());
        mDog.setGn(mSpinnerGender.getSelectedItem().toString());
        mDog.setRc(mSpinnerRace.getSelectedItem().toString());
        mDog.setBh(mSpinnerBehavior.getSelectedItem().toString());
        mDog.setIt(mSpinnerInteractions.getSelectedItem().toString());


        if (name.length() < 2 || country.length() < 2 || city.length() < 1 || street.length() < 2 || streeNumber.length() < 1) {
            Toast.makeText(getApplicationContext(), R.string.dog_not_saved, Toast.LENGTH_SHORT).show();
            mDogCriticalParametersSet = false;
        }
        else {
            mDogCriticalParametersSet = true;
        }

    }
    private void updateProfileFieldsWithFoundationData() {
        if (!TextUtils.isEmpty(mFoundationId)) mEditTextFoundation.setText(mFoundationName);
        if (!TextUtils.isEmpty(mFoundationName)) mEditTextFoundation.setText(mFoundationName);
        if (!TextUtils.isEmpty(mFoundationCity) && mEditTextCity.getText().toString().equals("")) mEditTextCity.setText(mFoundationCity);
        if (!TextUtils.isEmpty(mFoundationCountry) && mEditTextCountry.getText().toString().equals("")) mEditTextCountry.setText(mFoundationCountry);
        if (!TextUtils.isEmpty(mFoundationStreet) && mEditTextStreet.getText().toString().equals("")) mEditTextStreet.setText(mFoundationStreet);
        if (!TextUtils.isEmpty(mFoundationStreetNumber) && mEditTextStreetNumber.getText().toString().equals("")) mEditTextStreetNumber.setText(mFoundationStreetNumber);
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
        mVideoLinks = mDog.getVU();
        if (mVideoLinks==null || mVideoLinks.size()==0) return;
        String url = mVideoLinks.get(clickedItemIndex);
        Utilities.goToWebLink(this, url);
    }

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {
        if (dogsList.size()==1) {
            if (dogsList.get(0) != null) mDog = dogsList.get(0);
            mDogAlreadyExistsInFirebaseDb = true;
        }
        else if (dogsList.size()>1) {
            //Get the first dog that corresponds to the Foundation's Firebase Id
            for (Dog dog : dogsList) {
                if (dog.getAFid().equals(mFirebaseUid)) {
                    mDog = dog;
                    break;
                }
            }
            mDogAlreadyExistsInFirebaseDb = true;
            Log.i(DEBUG_TAG, "Warning! Multiple dogs found with the same characteristics.");
        }
        else {
            Toast.makeText(getBaseContext(), "No dog found for your foundation, press DONE to create a new dog.", Toast.LENGTH_SHORT).show();
        }

        if (mSavedInstanceState==null) {
            updateProfileFieldsOnScreen();
            updateDogWithUserInput();
        }
        mFirebaseDao.getAllObjectImagesFromFirebaseStorage(mDog);
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {

        if (foundationsList.size()==1) {
            if (foundationsList.get(0) != null) {
                mFoundationId = foundationsList.get(0).getUI();
                mFoundationName = foundationsList.get(0).getNm();
                mFoundationCity = foundationsList.get(0).getCt();
                mFoundationCountry = foundationsList.get(0).getCn();
                mFoundationStreet = foundationsList.get(0).getSt();
                mFoundationStreetNumber = foundationsList.get(0).getStN();
                updateProfileFieldsWithFoundationData();
            }
        }
        else if (foundationsList.size()>1) {
            mFoundationId = foundationsList.get(0).getUI();
            mFoundationName = foundationsList.get(0).getNm();
            mFoundationCity = foundationsList.get(0).getCt();
            mFoundationCountry = foundationsList.get(0).getCn();
            mFoundationStreet = foundationsList.get(0).getSt();
            mFoundationStreetNumber = foundationsList.get(0).getStN();
            updateProfileFieldsWithFoundationData();
            Log.i(DEBUG_TAG, "Warning! Multiple foundations found with the same id.");
        }
        else {
            Log.i(DEBUG_TAG, "Warning! No foundation found with the required id.");
        }

    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onMapMarkerListFound(List<MapMarker> markersList) {

    }
    @Override public void onImageAvailable(Uri downloadedImageUri, String imageName) {

        if (mImageViewMain==null
                || mDogImagesRecycleViewAdapter==null
                || mDog==null) return;

        Utilities.synchronizeImageOnAllDevices(getApplicationContext(), mDog, mFirebaseDao, imageName, downloadedImageUri);

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
            Utilities.displayObjectImageInImageView(getApplicationContext(), mDog, "mainImage", mImageViewMain);
            List<Uri> uris = Utilities.getExistingImageUriListForObject(getApplicationContext(), mDog, true);
            mDogImagesRecycleViewAdapter.setContents(uris);
        }
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
