package com.tindog;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tindog.data.DatabaseUtilities;
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
    @BindView(R.id.preferences_change_name) ImageView mImageViewChangeName;
    @BindView(R.id.preferences_change_email) ImageView mImageViewChangeEmail;
    @BindView(R.id.preferences_change_password) ImageView mImageViewChangePassword;
    @BindView(R.id.preferences_search_country_only_checkbox) CheckBox mCheckBoxLimitToCountry;
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
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.preferences_update_menu, menu);
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
                return true;
            case R.id.action_done:
                updatePreferencesWithUserInput();
                finish();
                return true;
            case R.id.action_edit_family_profile:
                Intent intent = new Intent(this, UpdateMyFamilyActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //Structural methods
    private void initializeParameters() {

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.preferences);
        }
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
    }
    private void updateProfileFields() {
        mTextViewUserName.setText(mNameFromFirebase);
        mTextViewUserEmail.setText(mEmailFromFirebase);
        mSpinnerAge.setSelection(SharedMethods.getSpinnerPositionFromText(getApplicationContext(), mSpinnerAge, mUser.getAgePref()));
        mSpinnerSize.setSelection(SharedMethods.getSpinnerPositionFromText(getApplicationContext(), mSpinnerSize, mUser.getAgePref()));
        mSpinnerGender.setSelection(SharedMethods.getSpinnerPositionFromText(getApplicationContext(), mSpinnerGender, mUser.getAgePref()));
        mSpinnerRace.setSelection(SharedMethods.getSpinnerPositionFromText(getApplicationContext(), mSpinnerRace, mUser.getAgePref()));
        mSpinnerBehavior.setSelection(SharedMethods.getSpinnerPositionFromText(getApplicationContext(), mSpinnerBehavior, mUser.getAgePref()));
        mSpinnerInteractions.setSelection(SharedMethods.getSpinnerPositionFromText(getApplicationContext(), mSpinnerInteractions, mUser.getAgePref()));
        mCheckBoxLimitToCountry.setChecked(mUser.getLimitQueryToCountry());
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
        mUser.setLimitQueryToCountry(mCheckBoxLimitToCountry.isChecked());

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
    private void showUserInfoUpdateDialog(final String infoType) {

        //Get the dialog view
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_enter_information, null);
        final EditText inputTextOld = dialogView.findViewById(R.id.input_text_old);
        final EditText inputTextNew = dialogView.findViewById(R.id.input_text_new);
        final TextInputLayout inputTextOldParent = dialogView.findViewById(R.id.dialog_input_text_old_parent);
        final TextInputLayout inputTextNewParent = dialogView.findViewById(R.id.dialog_input_text_new_parent);

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
                            DatabaseUtilities.updateFirebaseUserName(getApplicationContext(), mCurrentFirebaseUser, currentPassword, newInfo);
                            mTextViewUserName.setText(newInfo);
                        }
                        break;
                    case "email":
                        if (mCurrentFirebaseUser!=null) {
                            DatabaseUtilities.updateFirebaseUserEmail(getApplicationContext(), mCurrentFirebaseUser, currentPassword, newInfo);
                            mTextViewUserEmail.setText(newInfo);
                        }
                        break;
                    case "password":
                        if (mCurrentFirebaseUser!=null) {
                            DatabaseUtilities.updateFirebaseUserPassword(getApplicationContext(), mCurrentFirebaseUser, currentPassword, newInfo);
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
