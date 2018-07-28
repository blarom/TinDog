package com.tindog.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
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

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class PreferencesActivity extends AppCompatActivity implements FirebaseDao.FirebaseOperationsHandler, AdapterView.OnItemSelectedListener {

    //region Parameters
    private static final String DEBUG_TAG = "TinDog Preferences";
    @BindView(R.id.preferences_age_spinner) Spinner mSpinnerAge;
    @BindView(R.id.preferences_size_spinner) Spinner mSpinnerSize;
    @BindView(R.id.preferences_gender_spinner) Spinner mSpinnerGender;
    @BindView(R.id.preferences_race_spinner) Spinner mSpinnerRace;
    @BindView(R.id.preferences_behavior_spinner) Spinner mSpinnerBehavior;
    @BindView(R.id.preferences_interactions_spinner) Spinner mSpinnerInteractions;
    @BindView(R.id.preferences_name_value) TextView mTextViewUserName;
    @BindView(R.id.preferences_email_value) TextView mTextViewUserEmail;
    @BindView(R.id.preferences_change_name) ImageView mImageViewChangeName;
    @BindView(R.id.preferences_change_email) ImageView mImageViewChangeEmail;
    @BindView(R.id.preferences_change_password) ImageView mImageViewChangePassword;
    @BindView(R.id.preferences_search_country_only_checkbox) CheckBox mCheckBoxLimitToCountry;
    private Unbinder mBinding;
    private TinDogUser mUser;
    private FirebaseDao mFirebaseDao;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mCurrentFirebaseUser;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private String mNameFromFirebase;
    private String mEmailFromFirebase;
    private Uri mPhotoUriFromFirebase;
    private String mFirebaseUid;
    private ArrayAdapter<CharSequence> mSpinnerAdapterAge;
    private ArrayAdapter<CharSequence> mSpinnerAdapterSize;
    private ArrayAdapter<CharSequence> mSpinnerAdapterGender;
    private ArrayAdapter<CharSequence> mSpinnerAdapterRace;
    private ArrayAdapter<CharSequence> mSpinnerAdapterBehavior;
    private ArrayAdapter<CharSequence> mSpinnerAdapterInteractions;
    private boolean mUserFound;
    private int mAgeSpinnerPosition;
    private int mSizeSpinnerPosition;
    private int mGenderSpinnerPosition;
    private int mRaceSpinnerPosition;
    private int mBehaviorSpinnerPosition;
    private int mInteractionsSpinnerPosition;
    private boolean mLimitToCountry;
    private Bundle mSavedInstanceState;
    private boolean mAlreadyRequestedUserProfile;
    private Menu mMenu;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        mSavedInstanceState = savedInstanceState;
        initializeParameters();
        getUserInfoFromFirebase();
        getTinDogUserProfileFromFirebase();
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
        removeListeners();
        mBinding.unbind();
    }
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Utilities.FIREBASE_SIGN_IN_KEY) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                mCurrentFirebaseUser = mFirebaseAuth.getCurrentUser();
                Utilities.updateSignInMenuItem(mMenu, this, true);
                getUserInfoFromFirebase();
                getTinDogUserProfileFromFirebase();
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
        getMenuInflater().inflate(R.menu.preferences_update_menu, menu);
        mMenu = menu;
        if (mCurrentFirebaseUser==null) Utilities.updateSignInMenuItem(mMenu, this, false);
        else Utilities.updateSignInMenuItem(mMenu, this, true);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_save:
                updatePreferencesWithUserInput();
                mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mUser, true);
                return true;
            case R.id.action_done:
                updatePreferencesWithUserInput();
                mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mUser, true);
                finish();
                return true;
            case R.id.action_edit_family_profile:
                Intent intent = new Intent(this, UpdateMyFamilyActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            case R.id.action_signin:
                if (mCurrentFirebaseUser==null) {
                    Utilities.updateSignInMenuItem(mMenu, this, false);
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                    Utilities.showSignInScreen(PreferencesActivity.this);
                }
                else {
                    Utilities.updateSignInMenuItem(mMenu, this, true);
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), false);
                    mFirebaseAuth.signOut();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(getString(R.string.saved_firebase_email), mEmailFromFirebase);
        outState.putString(getString(R.string.saved_firebase_name), mNameFromFirebase);
        outState.putString(getString(R.string.saved_firebase_id), mFirebaseUid);
        outState.putParcelable(getString(R.string.saved_profile), mUser);
    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState!=null) {
            mEmailFromFirebase = savedInstanceState.getString(getString(R.string.saved_firebase_email));
            mNameFromFirebase = savedInstanceState.getString(getString(R.string.saved_firebase_name));
            mFirebaseUid = savedInstanceState.getString(getString(R.string.saved_firebase_id));
            mUser = savedInstanceState.getParcelable(getString(R.string.saved_profile));
            updateUserInfoShownToUser();
            updatePreferencesShownToUser();
        }
    }


    //Functional methods
    private void initializeParameters() {

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.preferences);
        }
        mBinding =  ButterKnife.bind(this);

        mUser = new TinDogUser();
        mUserFound = false;
        mFirebaseDao = new FirebaseDao(getBaseContext(), this);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mCurrentFirebaseUser = mFirebaseAuth.getCurrentUser();
        mAlreadyRequestedUserProfile = false;

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

        mImageViewChangeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUserInfoUpdateDialog("name");
            }
        });
        mImageViewChangeEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUserInfoUpdateDialog("email");
            }
        });
        mImageViewChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUserInfoUpdateDialog("password");
            }
        });

        mCheckBoxLimitToCountry.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mLimitToCountry = b;
            }
        });

        Utilities.hideSoftKeyboard(this);
    }
    private void updateUserInfoShownToUser() {
        mTextViewUserName.setText(mNameFromFirebase);
        mTextViewUserEmail.setText(mEmailFromFirebase);
    }
    private void updatePreferencesShownToUser() {

        mAgeSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerAge, mUser.getAP());
        mSizeSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerSize, mUser.getSP());
        mGenderSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerGender, mUser.getGP());
        mRaceSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerRace, mUser.getRP());
        mBehaviorSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerBehavior, mUser.getBP());
        mInteractionsSpinnerPosition = Utilities.getSpinnerPositionFromText(mSpinnerInteractions, mUser.getIP());

        mSpinnerAge.setSelection(mAgeSpinnerPosition);
        mSpinnerSize.setSelection(mSizeSpinnerPosition);
        mSpinnerGender.setSelection(mGenderSpinnerPosition);
        mSpinnerRace.setSelection(mRaceSpinnerPosition);
        mSpinnerBehavior.setSelection(mBehaviorSpinnerPosition);
        mSpinnerInteractions.setSelection(mInteractionsSpinnerPosition);

        mLimitToCountry = mUser.getLC();
        mCheckBoxLimitToCountry.setChecked(mLimitToCountry);

    }
    private void getUserInfoFromFirebase() {
        if (mCurrentFirebaseUser != null) {
            mNameFromFirebase = mCurrentFirebaseUser.getDisplayName();
            mEmailFromFirebase = mCurrentFirebaseUser.getEmail();
            //mPhotoUriFromFirebase = mCurrentFirebaseUser.getPhotoUrl();
            mFirebaseUid = mCurrentFirebaseUser.getUid();
        }
    }
    private void getTinDogUserProfileFromFirebase() {
        mCurrentFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mCurrentFirebaseUser != null && !mAlreadyRequestedUserProfile) {
            mUser.setUI(mFirebaseUid);
            if (!mUserFound) {
                mFirebaseDao.getUniqueObjectFromFirebaseDbOrCreateIt(mUser, true);
                mAlreadyRequestedUserProfile = true;
            }
        }
        if (mCurrentFirebaseUser != null) {
            Toast.makeText(getApplicationContext(), R.string.please_sign_in_to_see_preferences, Toast.LENGTH_SHORT).show();
        }
    }
    private void updatePreferencesWithUserInput() {

        mUser.setEm(mEmailFromFirebase);
        mUser.setNm(mNameFromFirebase);
        mUser.setUI(mFirebaseUid);

        mUser.setAP(mSpinnerAge.getSelectedItem().toString());
        mUser.setSP(mSpinnerSize.getSelectedItem().toString());
        mUser.setGP(mSpinnerGender.getSelectedItem().toString());
        mUser.setRP(mSpinnerAge.getSelectedItem().toString());
        mUser.setBP(mSpinnerBehavior.getSelectedItem().toString());
        mUser.setIP(mSpinnerInteractions.getSelectedItem().toString());
        mUser.setLC(mCheckBoxLimitToCountry.isChecked());

        if (TextUtils.isEmpty(mUser.getUI())) Log.i(DEBUG_TAG, "Error: TinDog User has empty unique ID!");
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
                    getUserInfoFromFirebase();
                    updateUserInfoShownToUser();
                    if (mSavedInstanceState==null) updatePreferencesShownToUser();
                    getTinDogUserProfileFromFirebase();
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_in:" + mCurrentFirebaseUser.getUid());
                } else {
                    // TinDogUser is signed out
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_out");
                    //Showing the sign-in screen
                    if (Utilities.getAppPreferenceUserHasNotRefusedSignIn(getApplicationContext())) {
                        mSavedInstanceState = null;
                        Utilities.showSignInScreen(PreferencesActivity.this);
                    }
                }
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private void showUserInfoUpdateDialog(final String infoType) {

        //Get the dialog view
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_enter_information, null);
        final EditText inputTextOld = dialogView.findViewById(R.id.dialog_user_info_input_text_old);
        final EditText inputTextNew = dialogView.findViewById(R.id.dialog_user_info_input_text_new);
        final TextInputLayout inputTextOldParent = dialogView.findViewById(R.id.dialog_user_info_input_text_old_parent);
        final TextInputLayout inputTextNewParent = dialogView.findViewById(R.id.dialog_user_info_input_text_new_parent);

        //Building the dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        switch (infoType) {
            case "name":
                builder.setTitle(R.string.please_enter_your_name);
                inputTextOld.setText("");
                inputTextOld.setEnabled(true);
                inputTextNew.setInputType(InputType.TYPE_CLASS_TEXT);
                inputTextOldParent.setHint(getString(R.string.current_password));
                inputTextNewParent.setHint("Enter new name (current: " + mNameFromFirebase + ")");
                break;
            case "email":
                builder.setTitle(R.string.please_enter_your_email);
                inputTextOld.setText("");
                inputTextOld.setEnabled(true);
                inputTextNew.setInputType(InputType.TYPE_CLASS_TEXT);
                inputTextOldParent.setHint(getString(R.string.current_password));
                inputTextNewParent.setHint("Enter new email (current: " + mEmailFromFirebase + ")");
                break;
            case "password":
                builder.setTitle(R.string.please_enter_your_password);
                inputTextOld.setText("");
                inputTextOld.setEnabled(true);
                inputTextNew.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                inputTextOldParent.setHint(getString(R.string.current_password));
                inputTextNewParent.setHint(getString(R.string.new_password));
                break;
        }

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                String currentPassword = inputTextOld.getText().toString();
                final String newInfo = inputTextNew.getText().toString();

                switch (infoType) {
                    case "name":
                        if (mCurrentFirebaseUser!=null) {
                            Utilities.updateFirebaseUserName(getApplicationContext(), mCurrentFirebaseUser, currentPassword, newInfo);
                            mTextViewUserName.setText(newInfo);
                        }
                        break;
                    case "email":
                        if (mCurrentFirebaseUser!=null) {
                            Utilities.updateFirebaseUserEmail(getApplicationContext(), mCurrentFirebaseUser, currentPassword, newInfo);
                            mTextViewUserEmail.setText(newInfo);
                        }
                        break;
                    case "password":
                        if (mCurrentFirebaseUser!=null) {
                            Utilities.updateFirebaseUserPassword(getApplicationContext(), mCurrentFirebaseUser, currentPassword, newInfo);
                        }
                        break;
                }

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
    private void startSearchResultsActivity(String profileType) {
        Intent intent = new Intent(this, SearchResultsActivity.class);
        intent.putExtra(getString(R.string.profile_type), profileType);
        startActivity(intent);
    }
    private void removeListeners() {
        mFirebaseDao.removeListeners();
        mImageViewChangeName.setOnClickListener(null);
        mImageViewChangeEmail.setOnClickListener(null);
        mImageViewChangePassword.setOnClickListener(null);
    }


    //Communication with other activities/fragments:

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {

    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {

    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {

    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

        mAlreadyRequestedUserProfile = false;

        if (usersList.size()==1) {
            if (usersList.get(0) != null) {
                mUser = usersList.get(0);
                mUserFound = true;
            }
        }
        else if (usersList.size()>1) {
            mUser = usersList.get(0);
            mUserFound = true;
            Toast.makeText(getBaseContext(), R.string.warning_multiple_users, Toast.LENGTH_SHORT).show();
        }
        else {
            mUser = new TinDogUser(mFirebaseUid);
            //Toast.makeText(getBaseContext(), "No user found for your entered email, press DONE to create a new user.", Toast.LENGTH_SHORT).show();
        }

        updateUserInfoShownToUser();
        if (mSavedInstanceState==null) {
            updatePreferencesShownToUser();
        }
    }
    @Override public void onMapMarkerListFound(List<MapMarker> markersList) {

    }
    @Override public void onImageAvailable(Uri imageUri, String imageName) {

    }
    @Override public void onImageUploaded(List<String> uploadTimes) {

    }

    //Communication with spinner adapters
    @Override public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        switch (adapterView.getId()) {
            case R.id.preferences_age_spinner:
                mAgeSpinnerPosition = pos;
                mUser.setAP((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.preferences_size_spinner:
                mSizeSpinnerPosition = pos;
                mUser.setSP((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.preferences_gender_spinner:
                mGenderSpinnerPosition = pos;
                mUser.setGP((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.preferences_race_spinner:
                mRaceSpinnerPosition = pos;
                mUser.setRP((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.preferences_behavior_spinner:
                mBehaviorSpinnerPosition = pos;
                mUser.setBP((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.preferences_interactions_spinner:
                mInteractionsSpinnerPosition = pos;
                mUser.setIP((String) adapterView.getItemAtPosition(pos));
                break;
        }
    }
    @Override public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
