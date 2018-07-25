package com.tindog.resources;

import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.net.Uri;

import com.tindog.R;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.TinDogUser;

import java.util.List;

public class ImageSyncAsyncTaskLoader extends AsyncTaskLoader<String> implements
        FirebaseDao.FirebaseOperationsHandler {


    private final static int NUM_OBJECTS_TO_UPDATE_BEFORE_DISPLAYING_IMAGES = 6;
    private final String mTask;
    private List<Dog> mDogsList;
    private List<Family> mFamiliesList;
    private List<Foundation> mFoundationsList;
    private String mProfileType;
    private String mCurrentImage;
    private int mPositionInObjectsList;
    private FirebaseDao mFirebaseDao;
    private boolean isCancelled;

    public ImageSyncAsyncTaskLoader(Context context,
                                    String task,
                                    String profileType,
                                    List<Dog> dogsList,
                                    List<Family> familiesList,
                                    List<Foundation> foundationsList,
                                    OnImageSyncOperationsHandler onImageSyncOperationsHandler) {
        super(context);
        this.mTask = task;
        this.mProfileType = profileType;
        this.mDogsList = dogsList;
        this.mFamiliesList = familiesList;
        this.mFoundationsList = foundationsList;
        this.onImageSyncOperationsHandler = onImageSyncOperationsHandler;
    }

    //Service methods
    @Override protected void onStartLoading() {
        if (mProfileType==null) return;

        isCancelled = false;
        mFirebaseDao = new FirebaseDao(getContext(), this);
        forceLoad();
    }
    @Override public String loadInBackground() {
        if (!isCancelled) startUpdatingImagesForObjects();
        return null;
    }


    //Functional methods
    private void startUpdatingImagesForObjects() {

        mCurrentImage = "mainImage";
        mPositionInObjectsList = 0;

        if (mProfileType.equals(getContext().getString(R.string.dog_profile)) && mDogsList!= null && mDogsList.size()>0) {
            mFirebaseDao.getImageFromFirebaseStorage(mDogsList.get(0), mCurrentImage);
        }
        else if (mProfileType.equals(getContext().getString(R.string.family_profile)) && mFamiliesList!= null && mFamiliesList.size()>0) {
            mFirebaseDao.getImageFromFirebaseStorage(mFamiliesList.get(0), mCurrentImage);
        }
        else if (mProfileType.equals(getContext().getString(R.string.foundation_profile)) && mFoundationsList!= null && mFoundationsList.size()>0) {
            mFirebaseDao.getImageFromFirebaseStorage(mFoundationsList.get(0), mCurrentImage);
        }

    }
    public void stopUpdatingImagesForObjects() {
        isCancelled = true;
        mFirebaseDao.removeListeners();
    }
    private void updateLocalImageForCurrentObject(String imageName, Uri imageUri) {

        if (mPositionInObjectsList == objectsListSize()) return;

        Object listElement = null;
        if (mProfileType.equals(getContext().getString(R.string.dog_profile))) {
            listElement = mDogsList.get(mPositionInObjectsList);
        }
        else if (mProfileType.equals(getContext().getString(R.string.family_profile))) {
            listElement = mFamiliesList.get(mPositionInObjectsList);
        }
        else if (mProfileType.equals(getContext().getString(R.string.foundation_profile))) {
            listElement = mFoundationsList.get(mPositionInObjectsList);
        }

        Utilities.updateImageOnLocalDevice(getContext(), listElement, mFirebaseDao, imageName, imageUri);
    }
    private int objectsListSize() {
        if (mProfileType.equals(getContext().getString(R.string.dog_profile))) {
            return mDogsList.size();
        }
        else if (mProfileType.equals(getContext().getString(R.string.family_profile))) {
            return mFamiliesList.size();
        }
        else if (mProfileType.equals(getContext().getString(R.string.foundation_profile))) {
            return mFoundationsList.size();
        }
        return 0;
    }
    private void getNextImage(String currentImageName) {

        if (mPositionInObjectsList == objectsListSize()) return;

        if (mTask.equals(getContext().getString(R.string.task_sync_list_main_images))) {
            if (mPositionInObjectsList < objectsListSize()) {
                mPositionInObjectsList++;
                if (mPositionInObjectsList < objectsListSize()) requestImageFromFirebase();
            }
            if (mPositionInObjectsList > 0 && mPositionInObjectsList % NUM_OBJECTS_TO_UPDATE_BEFORE_DISPLAYING_IMAGES == 0) {
                tellCallingClassToUpdateImageDisplay();
            }
        }
        else if (mTask.equals(getContext().getString(R.string.task_sync_single_object_images))) {
            switch (currentImageName) {
                case "mainImage": {
                    mCurrentImage = "image1";
                    requestImageFromFirebase();
                    break;
                }
                case "image1": {
                    mCurrentImage = "image2";
                    requestImageFromFirebase();
                    break;
                }
                case "image2": {
                    mCurrentImage = "image3";
                    requestImageFromFirebase();
                    break;
                }
                case "image3": {
                    mCurrentImage = "image4";
                    requestImageFromFirebase();
                    break;
                }
                case "image4": {
                    mCurrentImage = "image5";
                    requestImageFromFirebase();
                    break;
                }
                case "image5": {
                    tellCallingClassToUpdateImageDisplay();
                    break;
                }
            }
        }


    }
    private void tellCallingClassToUpdateImageDisplay() {
        onImageSyncOperationsHandler.onDisplayRefreshRequested();
    }
    private void requestImageFromFirebase() {

        if (mPositionInObjectsList == objectsListSize()) return;

        if (mProfileType.equals(getContext().getString(R.string.dog_profile))) {
            mFirebaseDao.getImageFromFirebaseStorage(mDogsList.get(mPositionInObjectsList), mCurrentImage);
        }
        else if (mProfileType.equals(getContext().getString(R.string.family_profile))) {
            mFirebaseDao.getImageFromFirebaseStorage(mFamiliesList.get(mPositionInObjectsList), mCurrentImage);
        }
        else if (mProfileType.equals(getContext().getString(R.string.foundation_profile))) {
            mFirebaseDao.getImageFromFirebaseStorage(mFoundationsList.get(mPositionInObjectsList), mCurrentImage);
        }
    }


    //Communication with other classes:

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {
    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {
    }
    @Override public void onImageAvailable(Uri imageUri, String currentImageName) {

        if (isCancelled) return;

        updateLocalImageForCurrentObject(currentImageName, imageUri);
        getNextImage(currentImageName);
    }
    @Override public void onImageUploaded(List<String> uploadTimes) {

    }

    //Communication with parent activity
    private OnImageSyncOperationsHandler onImageSyncOperationsHandler;
    public interface OnImageSyncOperationsHandler {
        void onDisplayRefreshRequested();
    }
}
