package com.tindog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.resources.ImageSyncAsyncTaskLoader;
import com.tindog.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


public class FamilyProfileFragment extends Fragment implements
        ImagesRecycleViewAdapter.ImageClickHandler,
        LoaderManager.LoaderCallbacks<String>,
        ImageSyncAsyncTaskLoader.OnImageSyncOperationsHandler {

    //region Parameters
    private static final int SINGLE_OBJECT_IMAGES_SYNC_LOADER = 8521;
    @BindView(R.id.family_profile_main_image) ImageView mImageViewMainImage;
    @BindView(R.id.family_profile_recyclerview_images) RecyclerView mRecyclerViewImages;
    @BindView(R.id.family_profile_pseudonym) TextView mTextViewFamilyPseudonym;
    @BindView(R.id.family_profile_address) TextView mTextViewFamilyAddress;
    @BindView(R.id.family_profile_value_experience) TextView mTextViewFamilyExperience;
    @BindView(R.id.family_profile_checkbox_foster) CheckBox mCheckBoxFoster;
    @BindView(R.id.family_profile_checkbox_adopt) CheckBox mCheckBoxAdopt;
    @BindView(R.id.family_profile_checkbox_foster_and_adopt) CheckBox mCheckBoxFosterAndAdopt;
    @BindView(R.id.family_profile_value_foster_period) TextView mTextViewFosterPeriod;
    @BindView(R.id.family_profile_checkbox_help_organize_move_equipment) CheckBox mCheckBoxHelpOrganizeMovingEquipment;
    @BindView(R.id.family_profile_checkbox_help_organize_move_dogs) CheckBox mCheckBoxHelpOrganizeMovingDogs;
    @BindView(R.id.family_profile_checkbox_help_organize_coordinating) CheckBox mCheckBoxHelpOrganizeCoordinating;
    @BindView(R.id.family_profile_checkbox_help_organize_lending_hand) CheckBox mCheckBoxHelpOrganizeLendingHand;
    @BindView(R.id.family_profile_checkbox_dogwalking) CheckBox mCheckBoxDogWalking;
    @BindView(R.id.family_profile_value_where_dogwalking) TextView mTextViewDogWalkingWhere;
    @BindView(R.id.family_profile_checkbox_dogwalking_afternoon) CheckBox mCheckBoxDogWalkingAfternoon;
    @BindView(R.id.family_profile_checkbox_dogwalking_evening) CheckBox mCheckBoxDogWalkingEvening;
    @BindView(R.id.family_profile_checkbox_dogwalking_morning) CheckBox mCheckBoxDogWalkingMorning;
    @BindView(R.id.family_profile_checkbox_dogwalking_noon) CheckBox mCheckBoxDogWalkingNoon;
    @BindView(R.id.family_profile_share_fab) FloatingActionButton mFabShare;
    private ImagesRecycleViewAdapter mImagesRecycleViewAdapter;
    private Unbinder mBinding;
    private Family mFamily;
    private List<Uri> mDisplayedImageList;
    private String mClickedImageUriString;
    private ImageSyncAsyncTaskLoader mImageSyncAsyncTaskLoader;
    private boolean mAlreadyLoadedImages;
    //endregion


    public FamilyProfileFragment() {
        // Required empty public constructor
    }


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getExtras();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_family_profile, container, false);

        initializeViews(rootView);
        startImageSyncThread();
        updateProfileFieldsOnScreen();

        return rootView;
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        onFamilyProfileFragmentOperationsHandler = (OnFamilyProfileFragmentOperationsHandler) context;
    }
    @Override public void onDetach() {
        super.onDetach();
        storeFragmentLayout();
        onFamilyProfileFragmentOperationsHandler = null;
        if (mImageSyncAsyncTaskLoader!=null) mImageSyncAsyncTaskLoader.stopUpdatingImagesForObjects();
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
        if (mImageSyncAsyncTaskLoader!=null) mImageSyncAsyncTaskLoader.stopUpdatingImagesForObjects();
    }


    //Structural methods
    private void getExtras() {
        if (getArguments() != null) {
            mFamily = getArguments().getParcelable(getString(R.string.profile_parcelable));
        }
    }
    private void initializeViews(View rootView) {
        mBinding = ButterKnife.bind(this, rootView);
        mClickedImageUriString = Utilities.getImageUriForObjectWithFileProvider(getContext(), mFamily, "mainImage").toString();
        setupImagesRecyclerView();
        mFabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareProfile();
            }
        });
    }
    private void setupImagesRecyclerView() {
        mRecyclerViewImages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mRecyclerViewImages.setNestedScrollingEnabled(true);
        mImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(getContext(), this, null);
        mRecyclerViewImages.setAdapter(mImagesRecycleViewAdapter);
    }
    private void updateProfileFieldsOnScreen() {


        mTextViewFamilyPseudonym.setText(mFamily.getPn());

        String address = mFamily.getSt() + ", " + mFamily.getCt() + ", " + mFamily.getCn();
        mTextViewFamilyAddress.setText(address);
        mTextViewFamilyExperience.setText(mFamily.getXp());

        if (mFamily.getXp().equals("")) mTextViewFamilyExperience.setText(R.string.no_exp_shared);
        else mTextViewFamilyExperience.setText(mFamily.getXp());

        mCheckBoxFoster.setChecked(mFamily.getFD());
        mCheckBoxAdopt.setChecked(mFamily.getAD());
        mCheckBoxFosterAndAdopt.setChecked(mFamily.getFAD());
        mTextViewFosterPeriod.setText(mFamily.getFP());
        mCheckBoxHelpOrganizeMovingEquipment.setChecked(mFamily.getHOE());
        mCheckBoxHelpOrganizeMovingDogs.setChecked(mFamily.getHOD());
        mCheckBoxHelpOrganizeCoordinating.setChecked(mFamily.getHOC());
        mCheckBoxHelpOrganizeLendingHand.setChecked(mFamily.getHOL());
        mCheckBoxDogWalking.setChecked(mFamily.getHD());
        mTextViewDogWalkingWhere.setText(mFamily.getHDW());
        mCheckBoxDogWalkingMorning.setChecked(mFamily.getHDM());
        mCheckBoxDogWalkingNoon.setChecked(mFamily.getHDN());
        mCheckBoxDogWalkingAfternoon.setChecked(mFamily.setHDA());
        mCheckBoxDogWalkingEvening.setChecked(mFamily.getHDE());

        displayImages();
    }
    private void displayImages() {
        if (getContext()==null) return;
        Utilities.displayObjectImageInImageView(getContext(), mFamily, "mainImage", mImageViewMainImage);
        mDisplayedImageList = Utilities.getExistingImageUriListForObject(getContext(), mFamily, false);
        mImagesRecycleViewAdapter.setContents(mDisplayedImageList);
    }
    private void storeFragmentLayout() {
        if (mRecyclerViewImages!=null) {
            int imagesRecyclerViewPosition = Utilities.getImagesRecyclerViewPosition(mRecyclerViewImages);
            onFamilyProfileFragmentOperationsHandler.onFamilyLayoutParametersCalculated(imagesRecyclerViewPosition);
        }
    }
    private void shareProfile() {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        StringBuilder builder = new StringBuilder("");
        builder.append(mFamily.getPn());
        builder.append("\n");
        builder.append(Utilities.getAddressStringFromComponents(null, mFamily.getSt(), mFamily.getCt(), null));
        shareIntent.putExtra(Intent.EXTRA_TEXT, builder.toString());

        Uri imageUri = Utilities.getImageUriForObjectWithFileProvider(getContext(), mFamily, Utilities.getImageNameFromUri(mClickedImageUriString));
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.setType("image/*");

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share images..."));

    }
    private void startImageSyncThread() {

        mAlreadyLoadedImages = false;
        if (getActivity()!=null) {
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> imageSyncAsyncTaskLoader = loaderManager.getLoader(SINGLE_OBJECT_IMAGES_SYNC_LOADER);
            if (imageSyncAsyncTaskLoader == null) {
                loaderManager.initLoader(SINGLE_OBJECT_IMAGES_SYNC_LOADER, null, this);
            }
            else {
                if (mImageSyncAsyncTaskLoader!=null) {
                    mImageSyncAsyncTaskLoader.cancelLoadInBackground();
                    mImageSyncAsyncTaskLoader = null;
                }
                loaderManager.restartLoader(SINGLE_OBJECT_IMAGES_SYNC_LOADER, null, this);
            }
        }

    }


    //Communication with other activities/fragments:

    //Communication with RecyclerView adapters
    @Override public void onImageClick(int clickedItemIndex) {
        mClickedImageUriString = mDisplayedImageList.get(clickedItemIndex).toString();
        Picasso.with(getContext())
                .load(mClickedImageUriString)
                .error(R.drawable.ic_image_not_available)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .into(mImageViewMainImage);
    }

    //Communication with Loader
    @NonNull @Override public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {

        if (id== SINGLE_OBJECT_IMAGES_SYNC_LOADER) {
            List<Family> familyList = new ArrayList<>();
            familyList.add(mFamily);
            mImageSyncAsyncTaskLoader =  new ImageSyncAsyncTaskLoader(getContext(), getString(R.string.task_sync_single_object_images),
                    getString(R.string.dog_profile), null, familyList, null, this);
            return mImageSyncAsyncTaskLoader;
        }
        return new ImageSyncAsyncTaskLoader(getContext(), "", null, null, null, null, this);
    }
    @Override public void onLoadFinished(@NonNull Loader<String> loader, String data) {
        if (loader.getId() == SINGLE_OBJECT_IMAGES_SYNC_LOADER && !mAlreadyLoadedImages) {
            mAlreadyLoadedImages = true;
            if (getContext()!=null) displayImages();
        }
    }
    @Override public void onLoaderReset(@NonNull Loader<String> loader) {

    }

    //Communication with ImageSyncAsyncTaskLoader
    @Override public void onDisplayRefreshRequested() {
        if (getContext()!=null) displayImages();
    }

    //Communication with parent activity
    private OnFamilyProfileFragmentOperationsHandler onFamilyProfileFragmentOperationsHandler;
    public interface OnFamilyProfileFragmentOperationsHandler {
        void onFamilyLayoutParametersCalculated(int imagesRecyclerViewPosition);
    }
    public void setImagesRecyclerViewPosition(int mStoredImagesRecyclerViewPosition) {
        if (mRecyclerViewImages!=null) mRecyclerViewImages.scrollToPosition(mStoredImagesRecyclerViewPosition);
    }
}
