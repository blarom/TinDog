package com.tindog.adapters;

import android.content.Context;
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
import com.tindog.data.Family;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FamiliesListRecycleViewAdapter extends RecyclerView.Adapter<FamiliesListRecycleViewAdapter.FamilyViewHolder> {

    private final Context mContext;
    private List<Family> mFamilies;
    final private FamiliesListItemClickHandler mOnClickHandler;

    public FamiliesListRecycleViewAdapter(Context context, FamiliesListItemClickHandler listener, List<Family> families) {
        this.mContext = context;
        this.mOnClickHandler = listener;
        this.mFamilies = families;
    }

    @NonNull
    @Override
    public FamilyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.list_item_profiles, parent, false);
        view.setFocusable(true);
        return new FamilyViewHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull FamilyViewHolder holder, int position) {
        updateItemDescription(holder, position);
        updateItemImage(holder, position);
    }
    private void updateItemDescription(FamilyViewHolder holder, int position) {
        Family family = mFamilies.get(position);
        TextView nameTV = holder.nameInRecycleView;
        nameTV.setText(family.getPn());
        TextView detailsTV = holder.detailsInRecycleView;
        String details = family.getCt();
        detailsTV.setText(details);
    }
    private void updateItemImage(final FamilyViewHolder holder, int position) {

        Picasso.with(mContext)
                .load(DatabaseUtilities.getImageUri(mContext, mFamilies.get(position), "mainImage"))
                .error(R.drawable.ic_image_not_available)
                .into(holder.imageInRecycleView);
    }

    @Override
    public int getItemCount() {
        return (mFamilies == null) ? 0 : mFamilies.size();
    }

    public void setContents(List<Family> family) {
        mFamilies = family;
        if (family != null) {
            this.notifyDataSetChanged();
        }
    }

    public class FamilyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.list_item_profiles_name) TextView nameInRecycleView;
        @BindView(R.id.list_item_profiles_details) TextView detailsInRecycleView;
        @BindView(R.id.list_item_profiles_image) ImageView imageInRecycleView;

        FamilyViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            mOnClickHandler.onFamiliesListItemClick(clickedPosition);
        }
    }

    public interface FamiliesListItemClickHandler {
        void onFamiliesListItemClick(int clickedItemIndex);
    }
}
