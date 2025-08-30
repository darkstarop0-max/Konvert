package com.curosoft.konvert.ui.dashboard.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;
import com.curosoft.konvert.ui.dashboard.models.RecentConvertItem;

import java.util.List;

public class RecentConvertAdapter extends RecyclerView.Adapter<RecentConvertAdapter.RecentConvertViewHolder> {

    private static final String TAG = "RecentConvertAdapter";
    private final List<RecentConvertItem> recentItems;

    public RecentConvertAdapter(List<RecentConvertItem> recentItems) {
        this.recentItems = recentItems;
    }

    @NonNull
    @Override
    public RecentConvertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_convert, parent, false);
        return new RecentConvertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentConvertViewHolder holder, int position) {
        RecentConvertItem item = recentItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return recentItems.size();
    }

    static class RecentConvertViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView fileNameView;
        private final TextView conversionDetailsView;
        private final TextView timestampView;

        public RecentConvertViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.file_icon);
            fileNameView = itemView.findViewById(R.id.file_name);
            conversionDetailsView = itemView.findViewById(R.id.conversion_details);
            timestampView = itemView.findViewById(R.id.timestamp);

            // Set click listener
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // Show a toast for now
                    Toast.makeText(
                            itemView.getContext(),
                            "Selected: " + fileNameView.getText(),
                            Toast.LENGTH_SHORT
                    ).show();
                    
                    // Log the click for debugging
                    Log.d(TAG, "Clicked on recent convert: " + fileNameView.getText());
                }
            });
        }

        public void bind(RecentConvertItem item) {
            iconView.setImageResource(item.getIconResId());
            fileNameView.setText(item.getFileName());
            conversionDetailsView.setText(item.getConversionDetails());
            timestampView.setText(item.getTimestamp());
        }
    }
}
