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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.resources.ImageSyncAsyncTaskLoader;
import com.tindog.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
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
    @BindView(R.id.dog_profile_show_in_widget_button) Button mButtonShowInWidget;
    @BindView(R.id.dog_profile_share_fab) FloatingActionButton mFabShare;
    private ImagesRecycleViewAdapter mImagesRecycleViewAdapter;
    private Unbinder mBinding;
    private Dog mDog;
    private List<Uri> mDisplayedImageList;
    private String mClickedImageUriString;
    private ImageSyncAsyncTaskLoader mImageSyncAsyncTaskLoader;
    private boolean mAlreadyLoadedImages;
    //endregion


    public DogProfileFragment() {
        // Required empty public constructor
    }


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getExtras();
    }
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dog_profile, container, false);

        initializeViews(rootView);
        startImageSyncThread();
        updateProfileFieldsOnScreen();

        return rootView;
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        onDogProfileFragmentOperationsHandler = (OnDogProfileFragmentOperationsHandler) context;
    }
    @Override public void onDetach() {
        super.onDetach();
        storeFragmentLayout();
        onDogProfileFragmentOperationsHandler = null;
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
            mDog = getArguments().getParcelable(getString(R.string.profile_parcelable));
        }
    }
    private void initializeViews(View rootView) {
        mBinding = ButterKnife.bind(this, rootView);
        setupImagesRecyclerView();
        mClickedImageUriString = Utilities.getImageUriForObjectWithFileProvider(getContext(), mDog, "mainImage").toString();
        mButtonShowInWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateDogImageWidgetWithCurrentDog();
            }
        });
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
                    showFoundation();
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

        displayImages();
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
    }
    private void showFoundation() {
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
    private void storeFragmentLayout() {
        if (mRecyclerViewImages!=null) {
            int imagesRecyclerViewPosition = Utilities.getImagesRecyclerViewPosition(mRecyclerViewImages);
            onDogProfileFragmentOperationsHandler.onDogLayoutParametersCalculated(imagesRecyclerViewPosition);
        }
    }
    private void updateDogImageWidgetWithCurrentDog() {
        Intent intent = new Intent(getContext(), WidgetUpdateJobIntentService.class);
        intent.putExtra(getString(R.string.intent_extra_specific_dog), mDog);
        intent.setAction(getString(R.string.action_update_widget_specific_dog));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //startService(intent);
        WidgetUpdateJobIntentService.enqueueWork(getContext(), intent);
    }
    private void shareProfile() {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        StringBuilder builder = new StringBuilder("");
        builder.append(mDog.getNm());
        builder.append("\n\n");
        builder.append("Address:\n");
        builder.append(Utilities.getAddressStringFromComponents(mDog.getStN(), mDog.getSt(), mDog.getCt(), null));
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
                    .error(R.drawable.ic_image_not_available)
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

    //Communication with parent activity
    private OnDogProfileFragmentOperationsHandler onDogProfileFragmentOperationsHandler;
    public interface OnDogProfileFragmentOperationsHandler {
        void onDogLayoutParametersCalculated(int imagesRecyclerViewPosition);
    }
    public void setImagesRecyclerViewPosition(int mStoredImagesRecyclerViewPosition) {
        if (mRecyclerViewImages!=null) mRecyclerViewImages.scrollToPosition(mStoredImagesRecyclerViewPosition);
    }
}
