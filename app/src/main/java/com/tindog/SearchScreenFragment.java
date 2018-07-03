package com.tindog;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tindog.adapters.DogsListRecycleViewAdapter;
import com.tindog.adapters.FamiliesListRecycleViewAdapter;
import com.tindog.adapters.FoundationsListRecycleViewAdapter;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.FirebaseDao;
import com.tindog.data.Foundation;
import com.tindog.data.TinDogUser;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SearchScreenFragment extends Fragment implements DogsListRecycleViewAdapter.DogsListItemClickHandler, FamiliesListRecycleViewAdapter.FamiliesListItemClickHandler, FoundationsListRecycleViewAdapter.FoundationsListItemClickHandler, FirebaseDao.FirebaseOperationsHandler {

    @BindView(R.id.search_screen_magnifying_glass_image) ImageView mImageViewMagnifyingGlass;
    @BindView(R.id.search_screen_profile_selection_recycler_view) RecyclerView mRecyclerViewProfileSelection;
    @BindView(R.id.search_screen_list_selection_tab_layout) TabLayout mTabLayoutListSelection;

    private DogsListRecycleViewAdapter mDogsListRecycleViewAdapter;
    private FamiliesListRecycleViewAdapter mFamiliesListRecycleViewAdapter;
    private FoundationsListRecycleViewAdapter mFoundationsListRecycleViewAdapter;
    private DatabaseReference mFirebaseDbReference;

    public SearchScreenFragment() {
        // Required empty public constructor
    }

    //Lifecyle methods
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        mFirebaseDbReference = firebaseDb.getReference();
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_screen, container, false);
        ButterKnife.bind(this, rootView);

        setupLists();

        return rootView;
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        onProfileSelectedListener = (OnProfileSelectedListener) context;
    }
    @Override public void onDetach() {
        super.onDetach();
        onProfileSelectedListener = null;
    }

    //Functionality methods
    private void setupLists() {

        //Setting up the item lists (results are received through the FirebaseDao interface, see methods below)
        FirebaseDao firebaseDao = new FirebaseDao(getContext(), this);
        firebaseDao.getFullObjectsListFromFirebase(new Dog());
        firebaseDao.getFullObjectsListFromFirebase(new Family());
        firebaseDao.getFullObjectsListFromFirebase(new Foundation());

        //Setting up the RecyclerView adapters
        mRecyclerViewProfileSelection.setLayoutManager(new LinearLayoutManager(getContext()));
        if (mDogsListRecycleViewAdapter==null) mDogsListRecycleViewAdapter = new DogsListRecycleViewAdapter(getContext(), this, null);
        if (mFamiliesListRecycleViewAdapter==null) mFamiliesListRecycleViewAdapter = new FamiliesListRecycleViewAdapter(getContext(), this, null);
        if (mFoundationsListRecycleViewAdapter==null) mFoundationsListRecycleViewAdapter = new FoundationsListRecycleViewAdapter(getContext(), this, null);
        mRecyclerViewProfileSelection.setAdapter(mDogsListRecycleViewAdapter);

        mTabLayoutListSelection.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        mRecyclerViewProfileSelection.setAdapter(mDogsListRecycleViewAdapter);
                        break;
                    case 1:
                        mRecyclerViewProfileSelection.setAdapter(mFamiliesListRecycleViewAdapter);
                        break;
                    case 2:
                        mRecyclerViewProfileSelection.setAdapter(mFoundationsListRecycleViewAdapter);
                        break;
                    default:
                        mRecyclerViewProfileSelection.setAdapter(mDogsListRecycleViewAdapter);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    //Communication with other activities/fragments:

    //Communication with RecyclerView adapters
    private OnProfileSelectedListener onProfileSelectedListener;
    public interface OnProfileSelectedListener {
        void onProfileSelected(String profile, int clickedItemIndex);
    }
    @Override public void onDogsListItemClick(int clickedItemIndex) {
        onProfileSelectedListener.onProfileSelected(getString(R.string.dog_profile), clickedItemIndex);
    }
    @Override public void onFamiliesListItemClick(int clickedItemIndex) {
        onProfileSelectedListener.onProfileSelected(getString(R.string.family_profile), clickedItemIndex);
    }
    @Override public void onFoundationsListItemClick(int clickedItemIndex) {
        onProfileSelectedListener.onProfileSelected(getString(R.string.foundation_profile), clickedItemIndex);
    }

    //Communication with Firebase Dao handler
    @Override public void onDogsListFound(List<Dog> dogsList) {
        mDogsListRecycleViewAdapter.setContents(dogsList);
    }
    @Override public void onFamiliesListFound(List<Family> familiesList) {
        mFamiliesListRecycleViewAdapter.setContents(familiesList);
    }
    @Override public void onFoundationsListFound(List<Foundation> foundationsList) {
        mFoundationsListRecycleViewAdapter.setContents(foundationsList);
    }
    @Override public void onTinDogUserListFound(List<TinDogUser> usersList) {

    }
    @Override public void onImageAvailable(android.net.Uri imageUri, String imageName) {

    }
}
