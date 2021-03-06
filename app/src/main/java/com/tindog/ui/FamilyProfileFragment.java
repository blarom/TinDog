package com.tindog.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.tindog.R;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Family;
import com.tindog.resources.ImageSyncAsyncTaskLoader;
import com.tindog.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
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
    @BindView(R.id.family_profile_scroll_container) NestedScrollView mScrollContainer;
    private ImagesRecycleViewAdapter mImagesRecycleViewAdapter;
    private Unbinder mBinding;
    private Family mFamily;
    private List<Uri> mDisplayedImageList;
    private String mClickedImageUriString;
    private ImageSyncAsyncTaskLoader mImageSyncAsyncTaskLoader;
    private boolean mAlreadyLoadedImages;
    private int mScrollPosition;
    private int mImagesRecyclerViewPosition;
    //endregion


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getExtras();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_family_profile, container, false);

        initializeViews(rootView);
        if (savedInstanceState!=null) restoreFragmentParameters(savedInstanceState);
        updateProfileFieldsOnScreen();
        displayImages();
        restoreLayoutParameters();
        if (savedInstanceState==null) startImageSyncThread();

        return rootView;
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
    }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(getString(R.string.saved_profile_scroll_position), mScrollContainer.getScrollY());
        outState.putParcelable(getString(R.string.saved_profile_state), mFamily);
        if (mRecyclerViewImages!=null) {
            mImagesRecyclerViewPosition = Utilities.getLinearRecyclerViewPosition(mRecyclerViewImages);
            outState.putInt(getString(R.string.saved_profile_images_rv_position), mImagesRecyclerViewPosition);
        }
        outState.putBoolean(getString(R.string.saved_profile_images_loaded_state), mAlreadyLoadedImages);
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        saveLayoutParameters();
        mBinding.unbind();
        if (mImageSyncAsyncTaskLoader!=null) mImageSyncAsyncTaskLoader.stopUpdatingImagesForObjects();
    }
    @Override public void onDetach() {
        super.onDetach();
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
    }
    private void setupImagesRecyclerView() {
        mRecyclerViewImages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mRecyclerViewImages.setNestedScrollingEnabled(true);
        mImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(getContext(), this, null);
        mRecyclerViewImages.setAdapter(mImagesRecycleViewAdapter);
        mRecyclerViewImages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mImagesRecyclerViewPosition = Utilities.getLinearRecyclerViewPosition(mRecyclerViewImages);
            }
        });
    }
    private void restoreFragmentParameters(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mFamily = savedInstanceState.getParcelable(getString(R.string.saved_profile_state));
            mAlreadyLoadedImages = savedInstanceState.getBoolean(getString(R.string.saved_profile_images_loaded_state));
        }
    }
    private void restoreLayoutParameters() {
        if (getContext()==null) return;
        String savedId = Utilities.getAppPreferenceProfileId(getContext());
        if (TextUtils.isEmpty(savedId) || !mFamily.getUI().equals(savedId)) return;

        mScrollPosition = Utilities.getAppPreferenceProfileScrollPosition(getContext());
        mImagesRecyclerViewPosition = Utilities.getAppPreferenceProfileImagesRvPosition(getContext());
        if (mScrollContainer!=null) {
            mScrollContainer.post(new Runnable() {
                @Override
                public void run() {
                    if (mScrollContainer!=null) mScrollContainer.scrollTo(0, mScrollPosition);
                }
            });
        }
        if (mRecyclerViewImages!=null) {
            mRecyclerViewImages.post(new Runnable() {
                @Override
                public void run() {
                    if (mRecyclerViewImages!=null) mRecyclerViewImages.scrollToPosition(mImagesRecyclerViewPosition);
                }
            });
        }
    }
    private void updateProfileFieldsOnScreen() {

        mTextViewFamilyPseudonym.setText(mFamily.getPn());

        String address = Utilities.getAddressStringFromComponents(null, mFamily.getSt(), mFamily.getCt(), mFamily.getSe(), mFamily.getCn());
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

        mScrollContainer.scrollTo(0, mScrollPosition);
    }
    private void displayImages() {
        if (getContext()==null) return;
        Utilities.displayObjectImageInImageView(getContext(), mFamily, "mainImage", mImageViewMainImage);
        mDisplayedImageList = Utilities.getExistingImageUriListForObject(getContext(), mFamily, false);
        mImagesRecycleViewAdapter.setContents(mDisplayedImageList);

        if (mRecyclerViewImages!=null) {
            mRecyclerViewImages.scrollToPosition(mImagesRecyclerViewPosition);
        }
    }
    private void saveLayoutParameters() {
        //SharedPreferences are used here instead of state restoration, because of the FragmentPager refresh that resets the parameters
        mScrollPosition = mScrollContainer.getScrollY();
        if (mRecyclerViewImages!=null)  mImagesRecyclerViewPosition = Utilities.getLinearRecyclerViewPosition(mRecyclerViewImages);
        if (mScrollPosition>0) {
            Utilities.setAppPreferenceProfileScrollPosition(getContext(), mScrollPosition);
            Utilities.setAppPreferenceProfileId(getContext(), mFamily.getUI());
        }
        if (mImagesRecyclerViewPosition>0) {
            Utilities.setAppPreferenceProfileImagesRvPosition(getContext(), mImagesRecyclerViewPosition);
        }
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


    //View click listeners
    @OnClick(R.id.family_profile_share_fab) public void shareProfile() {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        StringBuilder builder = new StringBuilder("");
        builder.append(mFamily.getPn());
        builder.append("\n");
        builder.append(Utilities.getAddressStringFromComponents(null, mFamily.getSt(), mFamily.getCt(), mFamily.getSe(), null));
        shareIntent.putExtra(Intent.EXTRA_TEXT, builder.toString());

        Uri imageUri = Utilities.getImageUriForObjectWithFileProvider(getContext(), mFamily, Utilities.getImageNameFromUri(mClickedImageUriString));
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.setType("image/*");

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share images..."));

    }


    //Communication with other activities/fragments:

    //Communication with RecyclerView adapters
    @Override public void onImageClick(int clickedItemIndex) {
        mClickedImageUriString = mDisplayedImageList.get(clickedItemIndex).toString();
        Picasso.with(getContext())
                .load(mClickedImageUriString)
                .placeholder(mImageViewMainImage.getDrawable()) //inspired by: https://github.com/square/picasso/issues/257
                //.error(R.drawable.ic_image_not_available)
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

}
