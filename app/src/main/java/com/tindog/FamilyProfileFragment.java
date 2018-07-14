package com.tindog;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


public class FamilyProfileFragment extends Fragment {

    @BindView(R.id.family_profile_pseudonym) TextView mTextViewFamilyPseudonym;
    @BindView(R.id.family_profile_address) TextView mTextViewFamilyAddress;
    @BindView(R.id.family_profile_value_experience) TextView mTextViewFamilyExperience;
    private int mSelectedProfileIndex;
    private Unbinder mBinding;

    public FamilyProfileFragment() {
        // Required empty public constructor
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSelectedProfileIndex = getArguments().getInt(getString(R.string.selected_profile_index));
        }
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_family_profile, container, false);
        mBinding = ButterKnife.bind(this, rootView);
        return rootView;
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
    }
    @Override public void onDetach() {
        super.onDetach();
    }

}
