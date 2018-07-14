package com.tindog.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tindog.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SimpleTextRecycleViewAdapter extends RecyclerView.Adapter<SimpleTextRecycleViewAdapter.TextViewHolder> {

    private final Context mContext;
    private List<String> texts;
    final private TextClickHandler mOnClickHandler;

    public SimpleTextRecycleViewAdapter(Context context, TextClickHandler listener, List<String> texts) {
        this.mContext = context;
        this.mOnClickHandler = listener;
        this.texts = texts;
    }

    @NonNull @Override public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.list_item_texts, parent, false);
        view.setFocusable(true);
        return new TextViewHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {

        holder.textViewInRecyclerView.setText(texts.get(position));
    }

    @Override public int getItemCount() {
        return (texts == null) ? 0 : texts.size();
    }

    public void setContents(List<String> texts) {
        this.texts = texts;
        if (texts != null) {
            this.notifyDataSetChanged();
        }
    }

    public class TextViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.list_item_simple_text) TextView textViewInRecyclerView;

        TextViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            mOnClickHandler.onTextClick(clickedPosition);
        }
    }

    public interface TextClickHandler {
        void onTextClick(int clickedItemIndex);
    }
}
