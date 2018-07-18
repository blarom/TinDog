package com.tindog;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Foundation;
import com.tindog.resources.SharedMethods;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


public class FoundationProfileFragment extends Fragment implements ImagesRecycleViewAdapter.ImageClickHandler {

    @BindView(R.id.foundation_profile_main_image) ImageView mImageViewMainImage;
    @BindView(R.id.foundation_profile_recyclerview_images) RecyclerView mRecyclerViewImages;
    @BindView(R.id.foundation_profile_foundation_name) TextView mTextViewFoundationName;
    @BindView(R.id.foundation_profile_address) TextView mTextViewFoundationAddress;
    @BindView(R.id.foundation_profile_contact_details) TextView mTextViewFoundationContactDetails;
    private ImagesRecycleViewAdapter mImagesRecycleViewAdapter;
    private Unbinder mBinding;
    private Foundation mFoundation;
    private List<Uri> mDisplayedImageList;


    public FoundationProfileFragment() {
        // Required empty public constructor
    }


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getExtras();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_foundation_profile, container, false);

        initializeViews(rootView);
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
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }


    //Structural methods
    private void getExtras() {
        if (getArguments() != null) {
            mFoundation = getArguments().getParcelable(getString(R.string.profile_parcelable));
        }
    }
    private void initializeViews(View rootView) {
        mBinding = ButterKnife.bind(this, rootView);
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

        String address = mFoundation.getSt() + ", " + mFoundation.getCt() + ", " + mFoundation.getCn();
        mTextViewFoundationAddress.setText(address);
        mTextViewFoundationContactDetails.setText(mFoundation.getCP());

        if (getContext()==null) return;
        String imagesDirectory = getContext().getFilesDir() + "/foundations/" + mFoundation.getUI() + "/images/";
        SharedMethods.displayImages(getContext(), imagesDirectory, "mainImage", mImageViewMainImage, mImagesRecycleViewAdapter);

        //Updating the images with the video links to display to the user
        mDisplayedImageList = SharedMethods.getUrisForExistingImages(imagesDirectory, false);
        mImagesRecycleViewAdapter.setContents(mDisplayedImageList);
    }
    private void storeFragmentLayout() {
        if (mRecyclerViewImages!=null) {
            int imagesRecyclerViewPosition = SharedMethods.getImagesRecyclerViewPosition(mRecyclerViewImages);
            onFoundationProfileFragmentOperationsHandler.onFoundationLayoutParametersCalculated(imagesRecyclerViewPosition);
        }
    }


    //Communication with other activities/fragments:

    //Communication with RecyclerView adapters
    @Override public void onImageClick(int clickedItemIndex) {
        String clickedImageUri = mDisplayedImageList.get(clickedItemIndex).toString();
        Picasso.with(getContext())
                .load(clickedImageUri)
                .error(R.drawable.ic_image_not_available)
                .into(mImageViewMainImage);
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
