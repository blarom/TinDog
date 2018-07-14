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
import com.tindog.data.Foundation;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FoundationsListRecycleViewAdapter extends RecyclerView.Adapter<FoundationsListRecycleViewAdapter.FoundationViewHolder> {

    private final Context mContext;
    private List<Foundation> mFoundations;
    final private FoundationsListItemClickHandler mOnClickHandler;

    public FoundationsListRecycleViewAdapter(Context context, FoundationsListItemClickHandler listener, List<Foundation> foundations) {
        this.mContext = context;
        this.mOnClickHandler = listener;
        this.mFoundations = foundations;
    }

    @NonNull @Override public FoundationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.list_item_profiles, parent, false);
        view.setFocusable(true);
        return new FoundationViewHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull FoundationViewHolder holder, int position) {
        updateItemDescription(holder, position);
        updateItemImage(holder, position);
    }
    private void updateItemDescription(FoundationViewHolder holder, int position) {
        Foundation foundation = mFoundations.get(position);
        TextView nameTV = holder.nameInRecycleView;
        nameTV.setText(foundation.getNm());
        TextView detailsTV = holder.detailsInRecycleView;
        String details = foundation.getCt();
        detailsTV.setText(details);
    }
    private void updateItemImage(final FoundationViewHolder holder, int position) {

        Picasso.with(mContext)
                .load(DatabaseUtilities.getImageUri(mContext, mFoundations.get(position), "mainImage"))
                .error(R.drawable.ic_image_not_available)
                .into(holder.imageInRecycleView);
    }

    @Override public int getItemCount() {
        return (mFoundations == null) ? 0 : mFoundations.size();
    }

    public void setContents(List<Foundation> foundation) {
        mFoundations = foundation;
        if (foundation != null) {
            this.notifyDataSetChanged();
        }
    }

    public class FoundationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.list_item_profiles_name) TextView nameInRecycleView;
        @BindView(R.id.list_item_profiles_details) TextView detailsInRecycleView;
        @BindView(R.id.list_item_profiles_image) ImageView imageInRecycleView;

        FoundationViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            mOnClickHandler.onFoundationsListItemClick(clickedPosition);
        }
    }

    public interface FoundationsListItemClickHandler {
        void onFoundationsListItemClick(int clickedItemIndex);
    }
}
