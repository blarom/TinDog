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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.tindog.R;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.resources.ImageSyncAsyncTaskLoader;
import com.tindog.resources.Utilities;
import com.tindog.services.WidgetUpdateJobIntentService;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class DogProfileFragment extends Fragment implements
        ImagesRecycleViewAdapter.ImageClickHandler,
        LoaderManager.LoaderCallbacks<String>,
        ImageSyncAsyncTaskLoader.OnImageSyncOperationsHandler {

    //regionParameters
    private static final int SINGLE_OBJECT_IMAGES_SYNC_LOADER = 8520;
    @BindView(R.id.dog_profile_main_image) ImageView mImageViewMainImage;
    @BindView(R.id.dog_profile_recyclerview_images) RecyclerView mRecyclerViewImages;
    @BindView(R.id.dog_profile_dog_name) TextView mTextViewDogName;
    @BindView(R.id.dog_profile_value_foundation) TextView mTextViewFoundation;
    @BindView(R.id.dog_profile_value_age) TextView mTextViewDogAge;
    @BindView(R.id.dog_profile_value_size) TextView mTextViewDogSize;
    @BindView(R.id.dog_profile_value_gender) TextView mTextViewDogGender;
    @BindView(R.id.dog_profile_value_behavior) TextView mTextViewDogBehavior;
    @BindView(R.id.dog_profile_value_interactions) TextView mTextViewDogInteractions;
    @BindView(R.id.dog_profile_value_history) TextView mTextViewDogHistory;
    @BindView(R.id.dog_profile_scroll_container) NestedScrollView mScrollContainer;
    private ImagesRecycleViewAdapter mImagesRecycleViewAdapter;
    private Unbinder mBinding;
    private Dog mDog;
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
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dog_profile, container, false);

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
        outState.putParcelable(getString(R.string.saved_profile_state), mDog);
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
            mDog = getArguments().getParcelable(getString(R.string.profile_parcelable));
        }
    }
    private void initializeViews(View rootView) {
        mBinding = ButterKnife.bind(this, rootView);
        setupImagesRecyclerView();
        mClickedImageUriString = Utilities.getImageUriForObjectWithFileProvider(getContext(), mDog, "mainImage").toString();
    }
    private void restoreFragmentParameters(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mDog = savedInstanceState.getParcelable(getString(R.string.saved_profile_state));
            mAlreadyLoadedImages = savedInstanceState.getBoolean(getString(R.string.saved_profile_images_loaded_state));
        }
    }
    private void restoreLayoutParameters() {
        if (getContext()==null) return;
        String savedId = Utilities.getAppPreferenceProfileId(getContext());
        if (TextUtils.isEmpty(savedId) || !mDog.getUI().equals(savedId)) return;

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
    private void updateProfileFieldsOnScreen() {

        mTextViewDogName.setText(mDog.getNm());

        String foundation = mDog.getFN();
        if (!TextUtils.isEmpty(foundation)) {
            //Make the foundation name a hyperlink
            SpannableString foundationSpan = new SpannableString(foundation);
            foundationSpan.setSpan(new URLSpan(""), 0, foundationSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTextViewFoundation.setText(foundationSpan);
            mTextViewFoundation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openFoundationProfile();
                }
            });
        }

        mTextViewDogAge.setText(mDog.getAg());
        mTextViewDogSize.setText(mDog.getSz());
        mTextViewDogGender.setText(mDog.getGn());
        mTextViewDogBehavior.setText(mDog.getBh());
        mTextViewDogInteractions.setText(mDog.getIt());

        if (mDog.getHs().equals("")) mTextViewDogHistory.setText(R.string.no_history_available);
        else mTextViewDogHistory.setText(mDog.getHs());

        mScrollContainer.scrollTo(0, mScrollPosition);

    }
    private void displayImages() {
        if (getContext()==null) return;
        Utilities.displayObjectImageInImageView(getContext(), mDog, "mainImage", mImageViewMainImage);

        //Updating the images with the video links to display to the user
        mDisplayedImageList = Utilities.getExistingImageUriListForObject(getContext(), mDog, false);
        List<String> videoUrls = mDog.getVU();
        for (String videoUrl : videoUrls) {
            mDisplayedImageList.add(Uri.parse(videoUrl));
        }
        mImagesRecycleViewAdapter.setContents(mDisplayedImageList);

        if (mRecyclerViewImages!=null) {
            mRecyclerViewImages.scrollToPosition(mImagesRecyclerViewPosition);
        }
    }
    private void openFoundationProfile() {
        if (getContext()!=null) {
            Intent intent = new Intent(getContext(), SearchResultsActivity.class);
            intent.putExtra(getString(R.string.profile_type), getString(R.string.foundation_profile));
            intent.putExtra(getString(R.string.requested_specific_foundation_profile), mDog.getAFid());
            startActivity(intent);
        }
    }
    private void playVideoInBrowser(int clickedItemIndex) {
        Utilities.goToWebLink(getContext(), mDisplayedImageList.get(clickedItemIndex).toString());
    }
    private void saveLayoutParameters() {
        //SharedPreferences are used here instead of state restoration, because of the FragmentPager refresh that resets the parameters
        mScrollPosition = mScrollContainer.getScrollY();
        if (mRecyclerViewImages!=null)  mImagesRecyclerViewPosition = Utilities.getLinearRecyclerViewPosition(mRecyclerViewImages);
        if (mScrollPosition>0) {
            Utilities.setAppPreferenceProfileScrollPosition(getContext(), mScrollPosition);
            Utilities.setAppPreferenceProfileId(getContext(), mDog.getUI());
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
    @OnClick(R.id.dog_profile_share_fab) public void shareProfile() {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        StringBuilder builder = new StringBuilder("");
        builder.append(mDog.getNm());
        builder.append("\n\n");
        builder.append("Address:\n");
        builder.append(Utilities.getAddressStringFromComponents(mDog.getStN(), mDog.getSt(), mDog.getCt(), mDog.getSe(), null));
        builder.append("\n\n");
        builder.append("Foundation info:\n");
        if (!TextUtils.isEmpty(mDog.getFN())) builder.append(mDog.getFN());
        if (!TextUtils.isEmpty(mDog.getAFCP())) { builder.append("\ntel. "); builder.append(mDog.getAFCP()); }
        shareIntent.putExtra(Intent.EXTRA_TEXT, builder.toString());

        Uri imageUri = Utilities.getImageUriForObjectWithFileProvider(getContext(), mDog, Utilities.getImageNameFromUri(mClickedImageUriString));
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.setType("image/*");

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share images..."));

    }
    @OnClick(R.id.dog_profile_show_in_widget_button) public void updateDogImageWidgetWithCurrentDog() {
        Intent intent = new Intent(getContext(), WidgetUpdateJobIntentService.class);
        intent.putExtra(getString(R.string.intent_extra_specific_dog), mDog);
        intent.setAction(getString(R.string.action_update_widget_specific_dog));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //startService(intent);
        WidgetUpdateJobIntentService.enqueueWork(getContext(), intent);
    }


    //Communication with other classes:

    //Communication with RecyclerView adapters
    @Override public void onImageClick(int clickedItemIndex) {
        mClickedImageUriString = mDisplayedImageList.get(clickedItemIndex).toString();
        if (URLUtil.isNetworkUrl(mClickedImageUriString)) {
            playVideoInBrowser(clickedItemIndex);
        }
        else {
            Picasso.with(getContext())
                    .load(mClickedImageUriString)
                    .placeholder(mImageViewMainImage.getDrawable()) //inspired by: https://github.com/square/picasso/issues/257
                    //.error(R.drawable.ic_image_not_available)
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .into(mImageViewMainImage);
        }
    }

    //Communication with Loader
    @NonNull @Override public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {

        if (id== SINGLE_OBJECT_IMAGES_SYNC_LOADER) {
            List<Dog> dogList = new ArrayList<>();
            dogList.add(mDog);
            mImageSyncAsyncTaskLoader =  new ImageSyncAsyncTaskLoader(getContext(), getString(R.string.task_sync_single_object_images),
                    getString(R.string.dog_profile), dogList, null, null, this);
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
