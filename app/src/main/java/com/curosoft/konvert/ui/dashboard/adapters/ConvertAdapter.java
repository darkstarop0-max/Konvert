package com.curosoft.konvert.ui.dashboard.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;
import com.curosoft.konvert.ui.dashboard.models.ConvertItem;

import java.util.List;

public class ConvertAdapter extends RecyclerView.Adapter<ConvertAdapter.ConvertViewHolder> {

    private static final String TAG = "ConvertAdapter";
    private final List<ConvertItem> convertItems;
    private OnConvertItemClickListener listener;
    
    public interface OnConvertItemClickListener {
        void onConvertItemClick(ConvertItem item);
    }
    
    public ConvertAdapter(List<ConvertItem> convertItems) {
        this.convertItems = convertItems;
    }
    
    public void setOnConvertItemClickListener(OnConvertItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConvertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_convert_tile, parent, false);
        return new ConvertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConvertViewHolder holder, int position) {
        ConvertItem item = convertItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return convertItems.size();
    }

    class ConvertViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView titleView;

        public ConvertViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.convert_icon);
            titleView = itemView.findViewById(R.id.convert_title);

            // Set click listener
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onConvertItemClick(convertItems.get(position));
                    
                    // Log the click for debugging
                    Log.d(TAG, "Clicked on convert tile: " + titleView.getText());
                }
            });
        }

        public void bind(ConvertItem item) {
            iconView.setImageResource(item.getIconResId());
            titleView.setText(item.getTitle());
        }
    }
}
