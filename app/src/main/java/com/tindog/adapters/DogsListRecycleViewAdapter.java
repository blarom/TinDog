package com.tindog.adapters;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tindog.R;
import com.tindog.data.DatabaseUtilities;
import com.tindog.data.Dog;
import com.tindog.data.Family;
import com.tindog.data.Foundation;
import com.tindog.resources.SharedMethods;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DogsListRecycleViewAdapter extends RecyclerView.Adapter<DogsListRecycleViewAdapter.DogViewHolder> {

    private final Context mContext;
    private List<Dog> mDogs;
    final private DogsListItemClickHandler mOnClickHandler;

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
    }
    private void updateItemDescription(DogViewHolder holder, int position) {
        Dog dog = mDogs.get(position);
        TextView nameTV = holder.nameInRecycleView;
        nameTV.setText(dog.getName());
        TextView detailsTV = holder.detailsInRecycleView;
        String details = dog.getGender() + ", " + dog.getRace();
        detailsTV.setText(details);
    }
    private void updateItemImage(final DogViewHolder holder, int position) {

        Picasso.with(mContext)
                .load(DatabaseUtilities.getImageUri(mContext, mDogs.get(position), "mainImage"))
                .error(R.drawable.ic_image_not_available)
                .into(holder.imageInRecycleView);
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
