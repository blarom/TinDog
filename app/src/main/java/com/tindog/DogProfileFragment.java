package com.tindog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
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
    @BindView(R.id.dog_profile_value_foundation) TextView mTextViewFoundation;
    @BindView(R.id.dog_profile_value_age) TextView mTextViewDogAge;
    @BindView(R.id.dog_profile_value_size) TextView mTextViewDogSize;
    @BindView(R.id.dog_profile_value_gender) TextView mTextViewDogGender;
    @BindView(R.id.dog_profile_value_behavior) TextView mTextViewDogBehavior;
    @BindView(R.id.dog_profile_value_interactions) TextView mTextViewDogInteractions;
    @BindView(R.id.dog_profile_value_history) TextView mTextViewDogHistory;
    @BindView(R.id.dog_profile_show_in_widget_button) Button mButtonShowInWidget;
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
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        onDogProfileFragmentOperationsHandler = (OnDogProfileFragmentOperationsHandler) context;
    }
    @Override public void onDetach() {
        super.onDetach();
        storeFragmentLayout();
        onDogProfileFragmentOperationsHandler = null;
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
        mButtonShowInWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateDogImageWidgetWithCurrentDog();
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

        if (getContext()==null) return;
        String imagesDirectory = getContext().getFilesDir() + "/dogs/" + mDog.getUI() + "/images/";
        SharedMethods.displayImages(getContext(), imagesDirectory, "mainImage", mImageViewMainImage, mImagesRecycleViewAdapter);

        //Updating the images with the video links to display to the user
        mDisplayedImageList = SharedMethods.getUrisForExistingImages(imagesDirectory, false);
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
            intent.putExtra(getString(R.string.foundation_profile_requested_by_user), mDog.getAFid());
            startActivity(intent);
        }
    }
    private void playVideoInBrowser(int clickedItemIndex) {
        SharedMethods.goToWebLink(getContext(), mDisplayedImageList.get(clickedItemIndex).toString());
    }
    private void storeFragmentLayout() {
        if (mRecyclerViewImages!=null) {
            int imagesRecyclerViewPosition = SharedMethods.getImagesRecyclerViewPosition(mRecyclerViewImages);
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


    //Communication with other activities/fragments:

    //Communication with RecyclerView adapters
    @Override public void onImageClick(int clickedItemIndex) {
        final String clickedImageUri = mDisplayedImageList.get(clickedItemIndex).toString();
        if (URLUtil.isNetworkUrl(clickedImageUri)) {
            playVideoInBrowser(clickedItemIndex);
        }
        else {
            Picasso.with(getContext())
                    .load(clickedImageUri)
                    .error(R.drawable.ic_image_not_available)
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .into(mImageViewMainImage);
        }
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
