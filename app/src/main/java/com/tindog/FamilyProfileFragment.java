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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.tindog.adapters.ImagesRecycleViewAdapter;
import com.tindog.data.Family;
import com.tindog.resources.SharedMethods;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


public class FamilyProfileFragment extends Fragment implements ImagesRecycleViewAdapter.ImageClickHandler {


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
    private ImagesRecycleViewAdapter mImagesRecycleViewAdapter;
    private Unbinder mBinding;
    private Family mFamily;
    private List<Uri> mDisplayedImageList;


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
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }


    //Structural methods
    private void getExtras() {
        if (getArguments() != null) {
            mFamily = getArguments().getParcelable(getString(R.string.profile_parcelable));
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

        if (getContext()==null) return;
        SharedMethods.displayObjectImageInImageView(getContext(), mFamily, "mainImage", mImageViewMainImage);
        List<Uri> uris = SharedMethods.getExistingImageUriListForObject(getContext(), mFamily, true);
        mImagesRecycleViewAdapter.setContents(uris);
    }
    private void storeFragmentLayout() {
        if (mRecyclerViewImages!=null) {
            int imagesRecyclerViewPosition = SharedMethods.getImagesRecyclerViewPosition(mRecyclerViewImages);
            onFamilyProfileFragmentOperationsHandler.onFamilyLayoutParametersCalculated(imagesRecyclerViewPosition);
        }
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
    private OnFamilyProfileFragmentOperationsHandler onFamilyProfileFragmentOperationsHandler;
    public interface OnFamilyProfileFragmentOperationsHandler {
        void onFamilyLayoutParametersCalculated(int imagesRecyclerViewPosition);
    }
    public void setImagesRecyclerViewPosition(int mStoredImagesRecyclerViewPosition) {
        if (mRecyclerViewImages!=null) mRecyclerViewImages.scrollToPosition(mStoredImagesRecyclerViewPosition);
    }
}
