package com.tindog.adapters;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;

import com.tindog.R;
import com.tindog.resources.Utilities;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ImagesRecycleViewAdapter extends RecyclerView.Adapter<ImagesRecycleViewAdapter.ImageViewHolder> {

    private final Context mContext;
    private List<Uri> mUris;
    final private ImageClickHandler mOnClickHandler;

    public ImagesRecycleViewAdapter(Context context, ImageClickHandler listener, List<Uri> uris) {
        this.mContext = context;
        this.mOnClickHandler = listener;
        this.mUris = uris;
    }

    @NonNull @Override public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.list_item_images, parent, false);
        view.setFocusable(true);
        return new ImageViewHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {

        if (URLUtil.isNetworkUrl(mUris.get(position).toString())) {
            holder.imageInRecycleView.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
            holder.imageInRecycleView.setBackgroundColor(Color.BLACK);
            holder.imageInRecycleView.setColorFilter(Color.WHITE);
        }
        else {
            Uri uri = mUris.get(position);
            Utilities.displayUriInImageView(mContext, uri, holder.imageInRecycleView);
        }
    }

    @Override public int getItemCount() {
        return (mUris == null) ? 0 : mUris.size();
    }

    public void setContents(List<Uri> uris) {
        mUris = uris;
        if (uris != null) {
            this.notifyDataSetChanged();
        }
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.list_item_profiles_image) ImageView imageInRecycleView;

        ImageViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            mOnClickHandler.onImageClick(clickedPosition);
        }
    }

    public interface ImageClickHandler {
        void onImageClick(int clickedItemIndex);
    }
}
