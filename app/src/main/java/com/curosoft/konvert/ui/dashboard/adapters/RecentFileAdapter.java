package com.curosoft.konvert.ui.dashboard.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;
import com.curosoft.konvert.ui.dashboard.models.RecentFile;

import java.util.List;

public class RecentFileAdapter extends RecyclerView.Adapter<RecentFileAdapter.ViewHolder> {
    
    private List<RecentFile> recentFiles;
    private OnRecentFileClickListener listener;

    public interface OnRecentFileClickListener {
        void onRecentFileClick(RecentFile recentFile);
    }

    public RecentFileAdapter(List<RecentFile> recentFiles) {
        this.recentFiles = recentFiles;
    }

    public void setOnRecentFileClickListener(OnRecentFileClickListener listener) {
        this.listener = listener;
    }

    public void updateFiles(List<RecentFile> newFiles) {
        this.recentFiles.clear();
        this.recentFiles.addAll(newFiles);
        notifyDataSetChanged();
    }

    public void submitList(List<RecentFile> newFiles) {
        this.recentFiles.clear();
        this.recentFiles.addAll(newFiles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_file_clean, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentFile recentFile = recentFiles.get(position);
        holder.bind(recentFile);
    }

    @Override
    public int getItemCount() {
        return recentFiles.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView fileNameTextView;
        private TextView fileTimestampTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.file_name);
            fileTimestampTextView = itemView.findViewById(R.id.file_timestamp);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onRecentFileClick(recentFiles.get(getAdapterPosition()));
                }
            });
        }

        public void bind(RecentFile recentFile) {
            fileNameTextView.setText(recentFile.getFileName());
            fileTimestampTextView.setText(recentFile.getTimeAgo());
        }
    }
}
