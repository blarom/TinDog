package com.tindog;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class PreferencesActivity extends AppCompatActivity implements FirebaseDao.FirebaseOperationsHandler, AdapterView.OnItemSelectedListener {

    private static final String DEBUG_TAG = "TinDog Preferences";
    @BindView(R.id.preferences_find) Button mButtonSearchForDog;
    @BindView(R.id.preferences_age_spinner) Spinner mSpinnerAge;
    @BindView(R.id.preferences_size_spinner) Spinner mSpinnerSize;
    @BindView(R.id.preferences_gender_spinner) Spinner mSpinnerGender;
    @BindView(R.id.preferences_race_spinner) Spinner mSpinnerRace;
    @BindView(R.id.preferences_behavior_spinner) Spinner mSpinnerBehavior;
    @BindView(R.id.preferences_interactions_spinner) Spinner mSpinnerInteractions;
    @BindView(R.id.preferences_skip) Button mButtonSkipDogPrefsSetting;
    @BindView(R.id.preferences_name_value) TextView mTextViewUserName;
    @BindView(R.id.preferences_email_value) TextView mTextViewUserEmail;
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

    //TODO setup name/email/password edit options for Firebase

    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        initializeParameters();
        updateProfileFields();
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
        if (requestCode == SharedMethods.FIREBASE_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                getUserInfoFromFirebase();
                mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mUser);
                getTinDogUserProfileFromFirebase();
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
        getMenuInflater().inflate(R.menu.dog_prefs_update_menu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {
            case R.id.action_done:
                updatePreferencesWithUserInput();
                finish();
                return true;
            case R.id.action_edit_family_profile:
                //TODO finish this
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Structural methods
    private void initializeParameters() {
        ButterKnife.bind(this);
        mUser = new TinDogUser();
        mFirebaseDao = new FirebaseDao(getBaseContext(), this);
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
    private void updateProfileFields() {
        mTextViewUserName.setText(mNameFromFirebase);
        mTextViewUserEmail.setText(mEmailFromFirebase);
    }
    private void getUserInfoFromFirebase() {
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
        }
    }
    private void getTinDogUserProfileFromFirebase() {
        if (mCurrentFirebaseUser != null) {
            mUser.setEmail(mEmailFromFirebase);
            mUser.setName(mNameFromFirebase);
            mUser.setUniqueIdentifier(mFirebaseUid);
            mFirebaseDao.getUniqueObjectFromFirebaseDb(mUser);
        }
    }
    private void updatePreferencesWithUserInput() {

        mUser.setAgePref(mSpinnerAge.getSelectedItem().toString());
        mUser.setSizePref(mSpinnerSize.getSelectedItem().toString());
        mUser.setGenderPref(mSpinnerGender.getSelectedItem().toString());
        mUser.setRacePref(mSpinnerAge.getSelectedItem().toString());
        mUser.setBehaviorPref(mSpinnerBehavior.getSelectedItem().toString());
        mUser.setInteractionsPref(mSpinnerInteractions.getSelectedItem().toString());

        mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mUser);
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
                    getUserInfoFromFirebase();
                    mFirebaseDao.updateObjectOrCreateItInFirebaseDb(mUser);
                    getTinDogUserProfileFromFirebase();
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

    //Communication with other activities/fragments:

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {

    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {

    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {

    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

        if (usersList.size()==1) {
            if (usersList.get(0) != null) mUser = usersList.get(0);
        }
        else if (usersList.size()>1) {
            mUser = usersList.get(0);
            Toast.makeText(getBaseContext(), "Warning! Multiple users found for your entered email.", Toast.LENGTH_SHORT).show();
        }
        else {
            mUser = new TinDogUser(mFirebaseUid);
            //Toast.makeText(getBaseContext(), "No user found for your entered email, press DONE to create a new user.", Toast.LENGTH_SHORT).show();
        }

        //getUserInfoFromFirebase();
        updateProfileFields();
    }
    @Override public void onImageAvailable(Uri imageUri, String imageName) {

    }

    //Communication with spinner adapters
    @Override public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        switch (adapterView.getId()) {
            case R.id.preferences_age_spinner:
                mUser.setAgePref((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.preferences_size_spinner:
                mUser.setSizePref((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.preferences_gender_spinner:
                mUser.setGenderPref((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.preferences_race_spinner:
                mUser.setRacePref((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.preferences_behavior_spinner:
                mUser.setBehaviorPref((String) adapterView.getItemAtPosition(pos));
                break;
            case R.id.preferences_interactions_spinner:
                mUser.setInteractionsPref((String) adapterView.getItemAtPosition(pos));
                break;
        }
    }
    @Override public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
