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
    private ImagesRecycleViewAdapter mImagesRecycleViewAdapter;
    private Unbinder mBinding;
    private Foundation mFoundation;
    private List<Uri> mDisplayedImageList;
    private String mClickedImageUriString;
    private ImageSyncAsyncTaskLoader mImageSyncAsyncTaskLoader;
    private boolean mAlreadyLoadedImages;
    //endregion


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getExtras();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_foundation_profile, container, false);

        initializeViews(rootView);
        startImageSyncThread();
        updateProfileFieldsOnScreen();

        return rootView;
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        onFoundationProfileFragmentOperationsHandler = (OnFoundationProfileFragmentOperationsHandler) context;
    }
    @Override public void onDetach() {
        super.onDetach();
        storeFragmentLayout();
        onFoundationProfileFragmentOperationsHandler = null;
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

        displayImages();
    }
    private void displayImages() {
        if (getContext()==null) return;
        Utilities.displayObjectImageInImageView(getContext(), mFoundation, "mainImage", mImageViewMainImage);
        mDisplayedImageList = Utilities.getExistingImageUriListForObject(getContext(), mFoundation, false);
        mImagesRecycleViewAdapter.setContents(mDisplayedImageList);
    }
    private void storeFragmentLayout() {
        if (mRecyclerViewImages!=null) {
            int imagesRecyclerViewPosition = Utilities.getImagesRecyclerViewPosition(mRecyclerViewImages);
            onFoundationProfileFragmentOperationsHandler.onFoundationLayoutParametersCalculated(imagesRecyclerViewPosition);
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
    @OnClick(R.id.dog_profile_share_fab) public void shareProfile() {

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
                .error(R.drawable.ic_image_not_available)
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

    //Communication with parent activity
    private OnFoundationProfileFragmentOperationsHandler onFoundationProfileFragmentOperationsHandler;
    public interface OnFoundationProfileFragmentOperationsHandler {
        void onFoundationLayoutParametersCalculated(int imagesRecyclerViewPosition);
    }
    public void setImagesRecyclerViewPosition(int mStoredImagesRecyclerViewPosition) {
        if (mRecyclerViewImages!=null) mRecyclerViewImages.scrollToPosition(mStoredImagesRecyclerViewPosition);
    }
}
