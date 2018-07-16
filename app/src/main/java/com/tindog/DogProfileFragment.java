package com.tindog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.resources.SharedMethods;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DogProfileFragment extends Fragment implements ImagesRecycleViewAdapter.ImageClickHandler {

    @BindView(R.id.dog_profile_main_image) ImageView mImageViewMainImage;
    @BindView(R.id.dog_profile_recyclerview_images) RecyclerView mRecyclerViewImages;
    @BindView(R.id.dog_profile_dog_name) TextView mTextViewDogName;
    @BindView(R.id.dog_profile_value_age) TextView mTextViewDogAge;
    @BindView(R.id.dog_profile_value_size) TextView mTextViewDogSize;
    @BindView(R.id.dog_profile_value_gender) TextView mTextViewDogGender;
    @BindView(R.id.dog_profile_value_behavior) TextView mTextViewDogBehavior;
    @BindView(R.id.dog_profile_value_interactions) TextView mTextViewDogInteractions;
    @BindView(R.id.dog_profile_value_history) TextView mTextViewDogHistory;
    private ImagesRecycleViewAdapter mImagesRecycleViewAdapter;
    private Unbinder mBinding;
    private Dog mDog;
    private List<Uri> mDisplayedImageList;


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
        updateProfileFieldsOnScreen();

        return rootView;
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
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
    }
    private void setupImagesRecyclerView() {
        mRecyclerViewImages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, true));
        mRecyclerViewImages.setNestedScrollingEnabled(true);
        mImagesRecycleViewAdapter = new ImagesRecycleViewAdapter(getContext(), this, null);
        mRecyclerViewImages.setAdapter(mImagesRecycleViewAdapter);
    }
    private void updateProfileFieldsOnScreen() {

        mTextViewDogName.setText(mDog.getNm());

        mTextViewDogAge.setText(mDog.getAg());
        mTextViewDogSize.setText(mDog.getSz());
        mTextViewDogGender.setText(mDog.getGn());
        mTextViewDogBehavior.setText(mDog.getBh());
        mTextViewDogInteractions.setText(mDog.getIt());

        if (mDog.getHs().equals("")) mTextViewDogHistory.setText(R.string.no_history_available);
        else mTextViewDogHistory.setText(mDog.getHs());

        if (getContext()==null) return;
        String imagesDirectory = getContext().getFilesDir() + "/dogs/" + mDog.getUI() + "/images/";
        SharedMethods.displayImages(getContext(), imagesDirectory, "mainImage", mImageViewMainImage, mImagesRecycleViewAdapter);

        //Updating the images with the video links to display to the user
        mDisplayedImageList = SharedMethods.getExistingImageUris(imagesDirectory, false);
        //TODO: find out why the main image is not shown next to the others
        List<String> videoUrls = mDog.getVU();
        for (String videoUrl : videoUrls) {
            mDisplayedImageList.add(Uri.parse(videoUrl));
        }
        mImagesRecycleViewAdapter.setContents(mDisplayedImageList);
    }


    //Communication with other activities/fragments:

    //Communication with RecyclerView adapters
    @Override public void onImageClick(int clickedItemIndex) {
        String clickedImageUri = mDisplayedImageList.get(clickedItemIndex).toString();
        if (URLUtil.isNetworkUrl(clickedImageUri)) {
            playVideoInBrowser(clickedItemIndex);
        }
        else {
            Picasso.with(getContext())
                    .load(clickedImageUri)
                    .error(R.drawable.ic_image_not_available)
                    .into(mImageViewMainImage);
        }
    }

    private void playVideoInBrowser(int clickedItemIndex) {
        Intent webIntent = new Intent(Intent.ACTION_VIEW, mDisplayedImageList.get(clickedItemIndex));
        if (getContext()!=null) getContext().startActivity(webIntent);
    }
}
