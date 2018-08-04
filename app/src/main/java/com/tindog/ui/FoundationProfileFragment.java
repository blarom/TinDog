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
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.tindog.R;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Foundation;
import com.tindog.resources.ImageSyncAsyncTaskLoader;
import com.tindog.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


public class FoundationProfileFragment extends Fragment implements
        ImagesRecycleViewAdapter.ImageClickHandler,
        LoaderManager.LoaderCallbacks<String>,
        ImageSyncAsyncTaskLoader.OnImageSyncOperationsHandler {

    //region Parameters
    private static final int SINGLE_OBJECT_IMAGES_SYNC_LOADER = 8522;
    @BindView(R.id.foundation_profile_main_image) ImageView mImageViewMainImage;
    @BindView(R.id.foundation_profile_recyclerview_images) RecyclerView mRecyclerViewImages;
    @BindView(R.id.foundation_profile_foundation_name) TextView mTextViewFoundationName;
    @BindView(R.id.foundation_profile_address) TextView mTextViewFoundationAddress;
    @BindView(R.id.foundation_profile_phone_number) TextView mTextViewFoundationPhoneNumber;
    @BindView(R.id.foundation_profile_email) TextView mTextViewFoundationEmail;
    @BindView(R.id.foundation_profile_website) TextView mTextViewFoundationWebsite;
    @BindView(R.id.foundation_profile_scroll_container) NestedScrollView mScrollContainer;
    private ImagesRecycleViewAdapter mImagesRecycleViewAdapter;
    private Unbinder mBinding;
    private Foundation mFoundation;
    private List<Uri> mDisplayedImageList;
    private String mClickedImageUriString;
    private ImageSyncAsyncTaskLoader mImageSyncAsyncTaskLoader;
    private boolean mAlreadyLoadedImages;
    private int mImagesRecyclerViewPosition;
    private int mScrollPosition;
    //endregion


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getExtras();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_foundation_profile, container, false);

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
        outState.putParcelable(getString(R.string.saved_profile_state), mFoundation);
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
            mFoundation = getArguments().getParcelable(getString(R.string.profile_parcelable));
        }
    }
    private void initializeViews(View rootView) {
        mBinding = ButterKnife.bind(this, rootView);
        mClickedImageUriString = Utilities.getImageUriForObjectWithFileProvider(getContext(), mFoundation, "mainImage").toString();
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
            mFoundation = savedInstanceState.getParcelable(getString(R.string.saved_profile_state));
            mAlreadyLoadedImages = savedInstanceState.getBoolean(getString(R.string.saved_profile_images_loaded_state));
        }
    }
    private void restoreLayoutParameters() {
        if (getContext()==null) return;
        String savedId = Utilities.getAppPreferenceProfileId(getContext());
        if (TextUtils.isEmpty(savedId) || !mFoundation.getUI().equals(savedId)) return;

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

        mTextViewFoundationName.setText(mFoundation.getNm());

        String address = Utilities.getAddressStringFromComponents(mFoundation.getStN(), mFoundation.getSt(), mFoundation.getCt(), mFoundation.getSe(), mFoundation.getCn());
        mTextViewFoundationAddress.setText(address);
        mTextViewFoundationPhoneNumber.setText(mFoundation.getCP());

        String foundationContactPhone = mFoundation.getCP();
        if (!TextUtils.isEmpty(foundationContactPhone)) {
            SpannableString foundationSpan = new SpannableString(foundationContactPhone);
            foundationSpan.setSpan(new URLSpan(""), 0, foundationSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTextViewFoundationPhoneNumber.setText(foundationSpan);
            mTextViewFoundationPhoneNumber.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openPhoneDialer();
                }
            });
        }

        String foundationEmail = mFoundation.getCE();
        if (!TextUtils.isEmpty(foundationEmail)) {
            SpannableString foundationSpan = new SpannableString(foundationEmail);
            foundationSpan.setSpan(new URLSpan(""), 0, foundationSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTextViewFoundationEmail.setText(foundationSpan);
            mTextViewFoundationEmail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendAnEmail();
                }
            });
        }

        String foundationWebsite = mFoundation.getWb();
        if (!TextUtils.isEmpty(foundationWebsite)) {
            SpannableString foundationSpan = new SpannableString(foundationWebsite);
            foundationSpan.setSpan(new URLSpan(""), 0, foundationSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTextViewFoundationWebsite.setText(foundationSpan);
            mTextViewFoundationWebsite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openWebsite();
                }
            });
        }


        mImagesRecyclerViewPosition = Utilities.getLinearRecyclerViewPosition(mRecyclerViewImages);
    }
    private void displayImages() {
        if (getContext()==null) return;
        Utilities.displayObjectImageInImageView(getContext(), mFoundation, "mainImage", mImageViewMainImage);
        mDisplayedImageList = Utilities.getExistingImageUriListForObject(getContext(), mFoundation, false);
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
            Utilities.setAppPreferenceProfileId(getContext(), mFoundation.getUI());
        }
        if (mImagesRecyclerViewPosition>0) {
            Utilities.setAppPreferenceProfileImagesRvPosition(getContext(), mImagesRecyclerViewPosition);
        }
    }
    private void openPhoneDialer() {
        //inspired by: https://stackoverflow.com/questions/36309049/how-to-open-dialer-on-phone-with-a-selected-number-in-android
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:"+mFoundation.getCP()));
        startActivity(intent);
    }
    private void sendAnEmail() {
        //inspired by: https://stackoverflow.com/questions/8701634/send-email-intent
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + mFoundation.getCE()));
        //emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        //emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(emailIntent, ""));
    }
    private void openWebsite() {
        String url = mFoundation.getWb();
        Utilities.goToWebLink(getContext(), url);
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
    @OnClick(R.id.foundation_profile_share_fab) public void shareProfile() {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        StringBuilder builder = new StringBuilder("");
        builder.append(mFoundation.getNm());
        builder.append("\n\n");
        builder.append("Address:\n");
        builder.append(Utilities.getAddressStringFromComponents(mFoundation.getStN(), mFoundation.getSt(), mFoundation.getCt(), mFoundation.getSe(), null));
        if (!TextUtils.isEmpty(mFoundation.getCP())) { builder.append("\ntel. "); builder.append(mFoundation.getCP()); }
        if (!TextUtils.isEmpty(mFoundation.getWb())) { builder.append("\n"); builder.append(mFoundation.getWb()); }
        if (!TextUtils.isEmpty(mFoundation.getCE())) { builder.append("\n"); builder.append(mFoundation.getCE()); }
        shareIntent.putExtra(Intent.EXTRA_TEXT, builder.toString());

        Uri imageUri = Utilities.getImageUriForObjectWithFileProvider(getContext(), mFoundation, Utilities.getImageNameFromUri(mClickedImageUriString));
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
            List<Foundation> foundationList = new ArrayList<>();
            foundationList.add(mFoundation);
            mImageSyncAsyncTaskLoader =  new ImageSyncAsyncTaskLoader(getContext(), getString(R.string.task_sync_single_object_images),
                    getString(R.string.dog_profile), null, null, foundationList, this);
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
