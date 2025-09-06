package com.curosoft.konvert.ui.docs;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean and minimal adapter for displaying documents in a RecyclerView
 */
public class CleanDocumentAdapter extends RecyclerView.Adapter<CleanDocumentAdapter.DocumentViewHolder> {
    
    private final Context context;
    private final List<File> documents = new ArrayList<>();
    private OnDocumentClickListener onDocumentClickListener;
    private OnDocumentLongClickListener onDocumentLongClickListener;
    
    public interface OnDocumentClickListener {
        void onDocumentClick(File document);
    }
    
    public interface OnDocumentLongClickListener {
        void onDocumentLongClick(File document, View view);
    }
    
    public CleanDocumentAdapter(Context context) {
        this.context = context;
    }
    
    public void setOnDocumentClickListener(OnDocumentClickListener listener) {
        this.onDocumentClickListener = listener;
    }
    
    public void setOnDocumentLongClickListener(OnDocumentLongClickListener listener) {
        this.onDocumentLongClickListener = listener;
    }
    
    public void updateDocuments(List<File> newDocuments) {
        documents.clear();
        documents.addAll(newDocuments);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_document_clean, parent, false);
        return new DocumentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        File document = documents.get(position);
        holder.bind(document);
    }
    
    @Override
    public int getItemCount() {
        return documents.size();
    }
    
    class DocumentViewHolder extends RecyclerView.ViewHolder {
        private final ImageView fileIcon;
        private final TextView fileName;
        private final TextView lastModified;
        private final TextView fileSize;
        
        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileName = itemView.findViewById(R.id.file_name);
            lastModified = itemView.findViewById(R.id.last_modified);
            fileSize = itemView.findViewById(R.id.file_size);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDocumentClickListener != null) {
                    onDocumentClickListener.onDocumentClick(documents.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDocumentLongClickListener != null) {
                    onDocumentLongClickListener.onDocumentLongClick(documents.get(position), v);
                    return true;
                }
                return false;
            });
        }
        
        public void bind(File document) {
            // Set file name
            fileName.setText(document.getName());
            
            // Set file icon based on extension
            String extension = getFileExtension(document.getName()).toLowerCase();
            switch (extension) {
                case "pdf":
                    fileIcon.setImageResource(R.drawable.ic_file_pdf);
                    break;
                case "docx":
                    fileIcon.setImageResource(R.drawable.ic_file_docx);
                    break;
                case "txt":
                    fileIcon.setImageResource(R.drawable.ic_file_txt);
                    break;
                default:
                    fileIcon.setImageResource(R.drawable.ic_file_general);
                    break;
            }
            
            // Set last modified date
            long lastModifiedTime = document.lastModified();
            if (lastModifiedTime > 0) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    lastModifiedTime,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                );
                lastModified.setText(relativeTime);
            } else {
                lastModified.setText("Unknown");
            }
            
            // Set file size (optional, currently hidden)
            long sizeInBytes = document.length();
            if (sizeInBytes > 0) {
                String formattedSize = formatFileSize(sizeInBytes);
                fileSize.setText(formattedSize);
                fileSize.setVisibility(View.VISIBLE);
            } else {
                fileSize.setVisibility(View.GONE);
            }
        }
        
        private String getFileExtension(String fileName) {
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
                return fileName.substring(lastDotIndex + 1);
            }
            return "";
        }
        
        private String formatFileSize(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return String.format("%.1f KB", bytes / 1024.0);
            } else if (bytes < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
            } else {
                return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }
}
