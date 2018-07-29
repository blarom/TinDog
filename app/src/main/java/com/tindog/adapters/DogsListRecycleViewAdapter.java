package com.tindog.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tindog.R;
import com.tindog.data.Dog;
import com.tindog.resources.Utilities;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DogsListRecycleViewAdapter extends RecyclerView.Adapter<DogsListRecycleViewAdapter.DogViewHolder> {

    private final Context mContext;
    private List<Dog> mDogs;
    final private DogsListItemClickHandler mOnClickHandler;
    private int mSelectedProfileIndex;

    public DogsListRecycleViewAdapter(Context context, DogsListItemClickHandler listener, List<Dog> dogs) {
        this.mContext = context;
        this.mOnClickHandler = listener;
        this.mDogs = dogs;
    }

    @NonNull @Override public DogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.list_item_profiles, parent, false);
        view.setFocusable(true);
        return new DogViewHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull DogViewHolder holder, int position) {
        updateItemDescription(holder, position);
        updateItemImage(holder, position);
        updateBackground(holder, position);
    }
    private void updateItemDescription(DogViewHolder holder, int position) {
        Dog dog = mDogs.get(position);
        TextView nameTV = holder.nameInRecycleView;
        nameTV.setText(dog.getNm());
        TextView detailsTV = holder.detailsInRecycleView;
        String details = dog.getGn() + ", " + dog.getRc();
        detailsTV.setText(details);
    }
    private void updateItemImage(final DogViewHolder holder, int position) {
        Utilities.displayObjectImageInImageView(mContext, mDogs.get(position), "mainImage", holder.imageInRecycleView);
    }
    private void updateBackground(DogViewHolder holder, int position) {
        if (position== mSelectedProfileIndex) {
            holder.container.setBackgroundColor(mContext.getResources().getColor(R.color.selected_item_background_color));
        }
        else {
            //Use the default android background color
            TypedValue typedValue = new TypedValue();
            mContext.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
            holder.container.setBackgroundColor(typedValue.data);
        }
    }
    public void setSelectedProfile(int selectedProfileIndex) {
        if (mSelectedProfileIndex != selectedProfileIndex) {
            mSelectedProfileIndex = selectedProfileIndex;
            this.notifyDataSetChanged();
        }
    }

    @Override public int getItemCount() {
        return (mDogs == null) ? 0 : mDogs.size();
    }

    public void setContents(List<Dog> dog) {
        mDogs = dog;
        if (dog != null) {
            this.notifyDataSetChanged();
        }
    }

    public class DogViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.list_item_profiles_name) TextView nameInRecycleView;
        @BindView(R.id.list_item_profiles_details) TextView detailsInRecycleView;
        @BindView(R.id.list_item_profiles_image) ImageView imageInRecycleView;
        @BindView(R.id.list_item_profiles_container) ConstraintLayout container;

        DogViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            mOnClickHandler.onDogsListItemClick(clickedPosition);
        }
    }

    public interface DogsListItemClickHandler {
        void onDogsListItemClick(int clickedItemIndex);
    }
}
