package com.tindog;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tindog.adapters.DogsListRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.TinDogUser;
import com.tindog.resources.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class UpdateMyDogsListActivity extends AppCompatActivity implements
        DogsListRecycleViewAdapter.DogsListItemClickHandler, FirebaseDao.FirebaseOperationsHandler {

    //region Parameters
    private static final String DEBUG_TAG = "TinDog DogsList";
    private static final int UPDATE_DOG_KEY = 222;
    @BindView(R.id.update_my_dogs_results_list) RecyclerView mRecyclerViewProfileSelection;
    @BindView(R.id.update_my_dogs_user_input) TextInputEditText mEditTextUserInput;
    @BindView(R.id.update_my_dogs_add_fab) FloatingActionButton mFabAddDog;
    private DogsListRecycleViewAdapter mDogsListRecycleViewAdapter;
    private DatabaseReference mFirebaseDbReference;
    private FirebaseDao mFirebaseDao;
    private FirebaseUser mCurrentFirebaseUser;
    private FirebaseAuth mFirebaseAuth;
    private String mFirebaseUid;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private List<Dog> mDogsList;
    private List<Dog> mDogsListShownToUser;
    private Unbinder mBinding;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_my_dogs_list);

        initializeParameters();
        setupRecyclerView();
        getFoundationFirebaseId();
    }
    @Override public void onStart() {
        super.onStart();
        setupFirebaseAuthentication();
    }
    @Override protected void onResume() {
        super.onResume();
        getListsFromFirebase();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        mBinding.unbind();
        mFirebaseDao.removeListeners();
    }
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Utilities.FIREBASE_SIGN_IN_KEY) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                getFoundationFirebaseId();
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
        else if (requestCode == UPDATE_DOG_KEY) {
            //Delay the search so that Firebase can have enough time to add/update the dog in the database
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    getListsFromFirebase();
                }
            };
            Handler handler = new Handler();
            handler.postDelayed(runnable, 500);
        }
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.update_my_dogs_list_menu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_search:
                getListsFromFirebase();
                return true;
            case R.id.action_done:
                finish();
                return true;
            case R.id.action_add:
                openDogProfile(new Dog());
                return true;
            case R.id.action_remove:
                Toast.makeText(getApplicationContext(), R.string.slide_dog_to_remove, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_edit_preferences:
                Intent intent = new Intent(this, PreferencesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //Functionality methods
    private void initializeParameters() {
        if (getSupportActionBar()!=null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mBinding =  ButterKnife.bind(this);
        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        mFirebaseDbReference = firebaseDb.getReference();
        mFirebaseDao = new FirebaseDao(this, this);
        mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mFabAddDog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDogProfile(new Dog());
            }
        });
    }
    private void setupRecyclerView() {
        mRecyclerViewProfileSelection.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        if (mDogsListRecycleViewAdapter==null) mDogsListRecycleViewAdapter = new DogsListRecycleViewAdapter(this, this, null);
        mRecyclerViewProfileSelection.setAdapter(mDogsListRecycleViewAdapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                Dog dog = mDogsList.get(viewHolder.getLayoutPosition());
                showDeleteDialog(dog);
            }

        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerViewProfileSelection);
    }
    private void showDeleteDialog(final Dog dog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.are_you_sure_delete_dog);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mDogsListShownToUser.remove(dog);
                mDogsListRecycleViewAdapter.setContents(mDogsListShownToUser);
                mFirebaseDao.deleteObjectFromFirebaseDb(dog);
                mFirebaseDao.deleteAllObjectImagesFromFirebaseStorage(dog);
                Utilities.deleteAllLocalObjectImages(getApplicationContext(), dog);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void getListsFromFirebase() {
        mFirebaseDao.getObjectsByKeyValuePairFromFirebaseDb(new Dog(), "afid", mFirebaseUid);
    }
    private void getFoundationFirebaseId() {
        if (mCurrentFirebaseUser != null) {
            //Since the foundation Id is the same as the User Id, then use that.
            mFirebaseUid = mCurrentFirebaseUser.getUid();
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
                    getFoundationFirebaseId();
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
                            Utilities.FIREBASE_SIGN_IN_KEY);
                }
            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private void getDogsListSubsetAccordingToUserInput() {
        mDogsListShownToUser = new ArrayList<>();
        String userInput = mEditTextUserInput.getText().toString();
        if (TextUtils.isEmpty(userInput)) {
            mDogsListShownToUser = mDogsList;
        }
        else {
            for (Dog dog : mDogsList) {
                if (dog.getNm().contains(userInput)) {
                    mDogsListShownToUser.add(dog);
                }
            }
        }
        mDogsListRecycleViewAdapter.setContents(mDogsListShownToUser);
    }
    private void openDogProfile(Dog dog) {
        Intent intent = new Intent(this, UpdateDogActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.selected_dog_id), dog.getUI());
        intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, UPDATE_DOG_KEY);
    }


    //Communication with other activities/fragments:

    //Communication with RecyclerView adapters
    @Override public void onDogsListItemClick(int clickedItemIndex) {
        openDogProfile(mDogsListShownToUser.get(clickedItemIndex));
    }

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {
        mDogsList = dogsList;
        getDogsListSubsetAccordingToUserInput();
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {
    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onImageAvailable(android.net.Uri imageUri, String imageName) {

    }
    @Override public void onImageUploaded(List<String> uploadTimes) {

    }

}
