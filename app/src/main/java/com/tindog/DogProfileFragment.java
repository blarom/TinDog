package com.tindog;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DogProfileFragment extends Fragment {

    @BindView(R.id.dog_profile_value_age) TextView mTextViewDogAge;
    @BindView(R.id.dog_profile_value_size) TextView mTextViewDogSize;
    @BindView(R.id.dog_profile_value_gender) TextView mTextViewDogGender;
    @BindView(R.id.dog_profile_value_behavior) TextView mTextViewDogBehavior;
    @BindView(R.id.dog_profile_value_interactions) TextView mTextViewDogInteractions;
    @BindView(R.id.dog_profile_value_history) TextView mTextViewDogHistory;

    private int mSelectedProfileIndex;
    private StorageReference mStorageRef;

    public DogProfileFragment() {
        // Required empty public constructor
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSelectedProfileIndex = getArguments().getInt(getString(R.string.selected_profile_index));
        }
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dog_profile, container, false);

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
