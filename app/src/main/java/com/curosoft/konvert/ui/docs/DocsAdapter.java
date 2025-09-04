package com.curosoft.konvert.ui.docs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.curosoft.konvert.R;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern adapter for document RecyclerView with DiffUtil optimization
 * Supports context menus, file operations, and efficient updates
 */
public class DocsAdapter extends RecyclerView.Adapter<DocsAdapter.DocViewHolder> {
    
    private List<File> documents;
    private final Context context;
    private OnDocumentActionListener actionListener;
    
    /**
     * Interface for document action callbacks
     */
    public interface OnDocumentActionListener {
        void onDocumentOpen(File document);
        void onDocumentShare(File document);
        void onDocumentRename(File document);
        void onDocumentDelete(File document);
        void onDocumentInfo(File document);
    }
    
    public DocsAdapter(Context context, List<File> documents) {
        this.context = context;
        this.documents = documents != null ? new ArrayList<>(documents) : new ArrayList<>();
    }
    
    public void setActionListener(OnDocumentActionListener listener) {
        this.actionListener = listener;
    }
    
    /**
     * Update documents list using DiffUtil for efficient animations
     */
    public void updateDocuments(List<File> newDocuments) {
        List<File> oldDocuments = this.documents;
        this.documents = newDocuments != null ? new ArrayList<>(newDocuments) : new ArrayList<>();
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DocumentDiffCallback(oldDocuments, this.documents));
        diffResult.dispatchUpdatesTo(this);
    }
    
    /**
     * Legacy method for compatibility
     */
    public void setFiles(List<File> files) {
        updateDocuments(files);
    }
    
    @NonNull
    @Override
    public DocViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doc, parent, false);
        return new DocViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DocViewHolder holder, int position) {
        File document = documents.get(position);
        holder.bind(document);
    }
    
    @Override
    public int getItemCount() {
        return documents.size();
    }
    
    /**
     * ViewHolder for document items with modern UI and interactions
     */
    class DocViewHolder extends RecyclerView.ViewHolder {
        private final ImageView fileIcon;
        private final TextView fileName;
        private final TextView fileSize;
        private final TextView fileDate;
        private final TextView filePath;
        private final TextView fileTypeBadge;
        private final ImageView actionArrow;
        
        DocViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileName = itemView.findViewById(R.id.file_name);
            fileSize = itemView.findViewById(R.id.file_size);
            fileDate = itemView.findViewById(R.id.file_date);
            filePath = itemView.findViewById(R.id.file_path);
            fileTypeBadge = itemView.findViewById(R.id.file_type_badge);
            actionArrow = itemView.findViewById(R.id.action_arrow);
        }
        
        void bind(File document) {
            // Set file icon based on type
            int iconRes = FileUtils.getIconForFileType(document.getName());
            fileIcon.setImageResource(iconRes);
            
            // Set file name
            fileName.setText(document.getName());
            
            // Set file size
            String formattedSize = FileUtils.formatFileSize(context, document.length());
            fileSize.setText(formattedSize);
            
            // Set last modified date
            String formattedDate = FileUtils.formatLastModified(document.lastModified());
            fileDate.setText(formattedDate);
            
            // Set file path
            String formattedPath = FileUtils.formatFilePath(document.getAbsolutePath());
            filePath.setText(formattedPath);
            
            // Set file type badge (optional)
            String fileType = FileUtils.getFileType(document.getName());
            if (fileTypeBadge != null) {
                fileTypeBadge.setText(fileType);
                fileTypeBadge.setVisibility(View.GONE); // Hide for cleaner look
            }
            
            // Set click listeners
            itemView.setOnClickListener(v -> openDocument(document));
            itemView.setOnLongClickListener(v -> {
                showContextMenu(v, document);
                return true;
            });
            
            actionArrow.setOnClickListener(v -> showContextMenu(v, document));
        }
        
        /**
         * Open document in DocumentViewerActivity
         */
        private void openDocument(File document) {
            try {
                if (actionListener != null) {
                    actionListener.onDocumentOpen(document);
                } else {
                    // Fallback direct opening
                    Intent intent = new Intent(context, DocumentViewerActivity.class);
                    Uri fileUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".provider",
                        document
                    );
                    intent.setData(fileUri);
                    intent.putExtra("fileName", document.getName());
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                }
            } catch (Exception e) {
                Toast.makeText(context, "Unable to open document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        
        /**
         * Show context menu for document actions
         */
        private void showContextMenu(View anchor, File document) {
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.getMenuInflater().inflate(R.menu.document_context_menu, popup.getMenu());
            
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_open) {
                    openDocument(document);
                    return true;
                } else if (itemId == R.id.action_share) {
                    shareDocument(document);
                    return true;
                } else if (itemId == R.id.action_rename) {
                    if (actionListener != null) {
                        actionListener.onDocumentRename(document);
                    }
                    return true;
                } else if (itemId == R.id.action_delete) {
                    showDeleteConfirmation(document);
                    return true;
                } else if (itemId == R.id.action_info) {
                    showDocumentInfo(document);
                    return true;
                }
                return false;
            });
            
            popup.show();
        }
        
        /**
         * Share document with other apps
         */
        private void shareDocument(File document) {
            try {
                if (actionListener != null) {
                    actionListener.onDocumentShare(document);
                } else {
                    // Fallback direct sharing
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    Uri fileUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".provider",
                        document
                    );
                    shareIntent.setType("*/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing: " + document.getName());
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    
                    context.startActivity(Intent.createChooser(shareIntent, "Share document"));
                }
            } catch (Exception e) {
                Toast.makeText(context, "Unable to share document", Toast.LENGTH_SHORT).show();
            }
        }
        
        /**
         * Show delete confirmation dialog
         */
        private void showDeleteConfirmation(File document) {
            new AlertDialog.Builder(context)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete \"" + document.getName() + "\"?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (actionListener != null) {
                        actionListener.onDocumentDelete(document);
                    } else {
                        // Fallback direct deletion
                        if (document.delete()) {
                            Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show();
                            // Remove from list
                            int position = getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) {
                                documents.remove(position);
                                notifyItemRemoved(position);
                            }
                        } else {
                            Toast.makeText(context, "Failed to delete document", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        }
        
        /**
         * Show document information dialog
         */
        private void showDocumentInfo(File document) {
            String info = "Name: " + document.getName() + "\n" +
                         "Size: " + FileUtils.formatFileSize(context, document.length()) + "\n" +
                         "Type: " + FileUtils.getFileType(document.getName()) + "\n" +
                         "Modified: " + FileUtils.formatLastModified(document.lastModified()) + "\n" +
                         "Path: " + document.getAbsolutePath();
            
            new AlertDialog.Builder(context)
                .setTitle("Document Information")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .setIcon(FileUtils.getIconForFileType(document.getName()))
                .show();
        }
    }
    
    /**
     * DiffUtil.Callback for efficient RecyclerView updates
     */
    private static class DocumentDiffCallback extends DiffUtil.Callback {
        private final List<File> oldList;
        private final List<File> newList;
        
        DocumentDiffCallback(List<File> oldList, List<File> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }
        
        @Override
        public int getOldListSize() {
            return oldList.size();
        }
        
        @Override
        public int getNewListSize() {
            return newList.size();
        }
        
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getAbsolutePath()
                    .equals(newList.get(newItemPosition).getAbsolutePath());
        }
        
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            File oldFile = oldList.get(oldItemPosition);
            File newFile = newList.get(newItemPosition);
            
            return oldFile.getName().equals(newFile.getName()) &&
                   oldFile.length() == newFile.length() &&
                   oldFile.lastModified() == newFile.lastModified();
        }
    }
}
