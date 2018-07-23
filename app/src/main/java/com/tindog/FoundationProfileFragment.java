package com.tindog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Foundation;
import com.tindog.resources.Utilities;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


public class FoundationProfileFragment extends Fragment implements ImagesRecycleViewAdapter.ImageClickHandler {

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

        if (getContext()==null) return;
        Utilities.displayObjectImageInImageView(getContext(), mFoundation, "mainImage", mImageViewMainImage);
        List<Uri> uris = Utilities.getExistingImageUriListForObject(getContext(), mFoundation, true);
        mImagesRecycleViewAdapter.setContents(uris);
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


    //Communication with other activities/fragments:

    //Communication with RecyclerView adapters
    @Override public void onImageClick(int clickedItemIndex) {
        String clickedImageUri = mDisplayedImageList.get(clickedItemIndex).toString();
        Picasso.with(getContext())
                .load(clickedImageUri)
                .error(R.drawable.ic_image_not_available)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
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
