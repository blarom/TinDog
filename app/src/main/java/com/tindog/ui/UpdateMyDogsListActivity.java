package com.tindog.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tindog.R;
import com.tindog.adapters.DogsListRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.MapMarker;
import com.tindog.data.TinDogUser;
import com.tindog.resources.ImageSyncAsyncTaskLoader;
import com.tindog.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class UpdateMyDogsListActivity extends AppCompatActivity implements
        DogsListRecycleViewAdapter.DogsListItemClickHandler,
        FirebaseDao.FirebaseOperationsHandler,
        LoaderManager.LoaderCallbacks<String>,
        ImageSyncAsyncTaskLoader.OnImageSyncOperationsHandler {

    //region Parameters
    private static final String DEBUG_TAG = "TinDog DogsList";
    private static final int UPDATE_DOG_KEY = 222;
    private static final int DOGS_LIST_MAIN_IMAGES_SYNC_LOADER = 9512;
    @BindView(R.id.update_my_dogs_results_list) RecyclerView mRecyclerViewProfileSelection;
    @BindView(R.id.update_my_dogs_user_input) TextInputEditText mEditTextUserInput;
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
    private Menu mMenu;
    private ImageSyncAsyncTaskLoader mImageSyncAsyncTaskLoader;
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
                Utilities.updateSignInMenuItem(mMenu, this, true);
                getFoundationFirebaseId();
                // ...
            } else {
                Utilities.updateSignInMenuItem(mMenu, this, false);
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
            case R.id.action_signin:
                if (mCurrentFirebaseUser==null) {
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                    Utilities.showSignInScreen(UpdateMyDogsListActivity.this);
                }
                else {
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), false);
                    mFirebaseAuth.signOut();
                    mDogsListRecycleViewAdapter.setContents(new ArrayList<Dog>());
                    Utilities.updateSignInMenuItem(mMenu, this, false);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //Functionality methods
    private void initializeParameters() {
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.my_foundation_dogs);
        }
        mBinding =  ButterKnife.bind(this);
        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        mFirebaseDbReference = firebaseDb.getReference();
        mFirebaseDao = new FirebaseDao(this, this);
        mCurrentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseAuth = FirebaseAuth.getInstance();

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
        mFirebaseDao.getObjectsByKeyValuePairFromFirebaseDb(new Dog(), "afid", mFirebaseUid, true);
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
                    Utilities.setAppPreferenceUserHasNotRefusedSignIn(getApplicationContext(), true);
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_in:" + mCurrentFirebaseUser.getUid());
                    getFoundationFirebaseId();
                } else {
                    // TinDogUser is signed out
                    Log.d(DEBUG_TAG, "onAuthStateChanged:signed_out");
                    //Showing the sign-in screen
                    boolean firstTime = Utilities.getAppPreferenceFirstTimeUsingApp(getApplicationContext());
                    if (!firstTime && Utilities.getAppPreferenceUserHasNotRefusedSignIn(getApplicationContext()))
                        Utilities.showSignInScreen(UpdateMyDogsListActivity.this);
                }

            }
        };
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
    private void getDogsListSubsetAccordingToUserInput() {

        if (mEditTextUserInput==null) return;

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

        startImageSyncThread();
    }
    private void openDogProfile(Dog dog) {
        Intent intent = new Intent(this, UpdateDogActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.selected_dog_id), dog.getUI());
        intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, UPDATE_DOG_KEY);
    }
    private void startImageSyncThread() {

        Log.i(DEBUG_TAG, "Called startImageSyncThread");
        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<String> imageSyncAsyncTaskLoader = loaderManager.getLoader(DOGS_LIST_MAIN_IMAGES_SYNC_LOADER);
        if (imageSyncAsyncTaskLoader == null) {
            loaderManager.initLoader(DOGS_LIST_MAIN_IMAGES_SYNC_LOADER, null, this);
        }
        else {
            if (mImageSyncAsyncTaskLoader!=null) {
                mImageSyncAsyncTaskLoader.stopUpdatingImagesForObjects();
                mImageSyncAsyncTaskLoader.cancelLoadInBackground();
                mImageSyncAsyncTaskLoader = null;
            }
            loaderManager.restartLoader(DOGS_LIST_MAIN_IMAGES_SYNC_LOADER, null, this);
        }

    }
    public void stopImageSyncThread() {
        if (mImageSyncAsyncTaskLoader!=null) {
            mImageSyncAsyncTaskLoader.stopUpdatingImagesForObjects();
            getLoaderManager().destroyLoader(DOGS_LIST_MAIN_IMAGES_SYNC_LOADER);
        }
    }


    //View click listeners
    @OnClick(R.id.update_my_dogs_add_fab) public void onAddDogFabClick() {
        openDogProfile(new Dog());
    }


    //Communication with other classes:

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
    @Override public void onMapMarkerListFound(List<MapMarker> markersList) {

    }
    @Override public void onImageAvailable(android.net.Uri imageUri, String imageName) {

    }
    @Override public void onImageUploaded(List<String> uploadTimes) {

    }

    //Communication with Loader
    @NonNull @Override public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {

        if (id == DOGS_LIST_MAIN_IMAGES_SYNC_LOADER && mImageSyncAsyncTaskLoader==null) {
            mImageSyncAsyncTaskLoader =  new ImageSyncAsyncTaskLoader(this, getString(R.string.task_sync_list_main_images),
                    getString(R.string.dog_profile), mDogsListShownToUser, null, null, this);
            return mImageSyncAsyncTaskLoader;
        }
        return new ImageSyncAsyncTaskLoader(this, "", null, null, null, null, this);
    }
    @Override public void onLoadFinished(@NonNull Loader<String> loader, String data) {
        if (loader.getId() == DOGS_LIST_MAIN_IMAGES_SYNC_LOADER) {
            mDogsListRecycleViewAdapter.notifyDataSetChanged();
            stopImageSyncThread();
        }
    }
    @Override public void onLoaderReset(@NonNull Loader<String> loader) {

    }

    //Communication with ImageSyncAsyncTaskLoader
    @Override public void onDisplayRefreshRequested() {
        mDogsListRecycleViewAdapter.notifyDataSetChanged();
    }
}
