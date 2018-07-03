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


public class FoundationProfileFragment extends Fragment {

    @BindView(R.id.foundation_profile_foundation_name) TextView mTextViewFoundationName;
    @BindView(R.id.foundation_profile_address) TextView mTextViewFoundationAddress;
    @BindView(R.id.foundation_profile_contact_details) TextView mTextViewFoundationContactDeails;
    private int mSelectedProfileIndex;

    public FoundationProfileFragment() {
        // Required empty public constructor
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSelectedProfileIndex = getArguments().getInt(getString(R.string.selected_profile_index));
        }
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_foundation_profile, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
    }
    @Override public void onDetach() {
        super.onDetach();
    }
}
