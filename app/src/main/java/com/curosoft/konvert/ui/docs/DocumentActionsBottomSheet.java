package com.curosoft.konvert.ui.docs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.curosoft.konvert.R;
import com.curosoft.konvert.utils.CustomDialogUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;

/**
 * Clean bottom sheet for document actions with Apple-style design
 */
public class DocumentActionsBottomSheet extends BottomSheetDialogFragment {
    
    private File document;
    private OnDocumentActionListener listener;
    
    public interface OnDocumentActionListener {
        void onDocumentDeleted(File document);
        void onDocumentRenamed(File oldFile, File newFile);
        void onDocumentOpened(File document);
    }
    
    public static DocumentActionsBottomSheet newInstance(File document) {
        DocumentActionsBottomSheet fragment = new DocumentActionsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("document", document);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setOnDocumentActionListener(OnDocumentActionListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            document = (File) getArguments().getSerializable("document");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_document_actions, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupDocumentInfo(view);
        setupActions(view);
    }
    
    private void setupDocumentInfo(View view) {
        ImageView documentIcon = view.findViewById(R.id.document_icon);
        TextView documentName = view.findViewById(R.id.document_name);
        TextView documentDetails = view.findViewById(R.id.document_details);
        
        // Set document name
        documentName.setText(document.getName());
        
        // Set document icon based on extension
        String extension = getFileExtension(document.getName()).toLowerCase();
        switch (extension) {
            case "pdf":
                documentIcon.setImageResource(R.drawable.ic_file_pdf);
                break;
            case "docx":
                documentIcon.setImageResource(R.drawable.ic_file_docx);
                break;
            case "txt":
                documentIcon.setImageResource(R.drawable.ic_file_txt);
                break;
            default:
                documentIcon.setImageResource(R.drawable.ic_file_general);
                break;
        }
        
        // Set document details
        String fileSize = formatFileSize(document.length());
        CharSequence lastModified = DateUtils.getRelativeTimeSpanString(
            document.lastModified(),
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        );
        documentDetails.setText(fileSize + " • Modified " + lastModified);
    }
    
    private void setupActions(View view) {
        LinearLayout actionOpen = view.findViewById(R.id.action_open);
        LinearLayout actionRename = view.findViewById(R.id.action_rename);
        LinearLayout actionShare = view.findViewById(R.id.action_share);
        LinearLayout actionDetails = view.findViewById(R.id.action_details);
        LinearLayout actionDelete = view.findViewById(R.id.action_delete);
        
        actionOpen.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDocumentOpened(document);
            }
            dismiss();
        });
        
        actionRename.setOnClickListener(v -> {
            showRenameDialog();
        });
        
        actionShare.setOnClickListener(v -> {
            shareDocument();
        });
        
        actionDetails.setOnClickListener(v -> {
            showDocumentDetails();
        });
        
        actionDelete.setOnClickListener(v -> {
            showDeleteConfirmation();
        });
    }
    
    private void showRenameDialog() {
        CustomDialogUtils.showRenameDialog(requireContext(), document.getName(), newName -> {
            renameDocument(newName);
        });
        dismiss();
    }
    
    private void renameDocument(String newName) {
        String extension = getFileExtension(document.getName());
        String fullNewName = newName + (extension.isEmpty() ? "" : "." + extension);
        
        File newFile = new File(document.getParent(), fullNewName);
        
        if (document.renameTo(newFile)) {
            if (listener != null) {
                listener.onDocumentRenamed(document, newFile);
            }
            Toast.makeText(requireContext(), "Document renamed successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Failed to rename document", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shareDocument() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            Uri fileUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                document
            );
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share Document"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to share document", Toast.LENGTH_SHORT).show();
        }
        dismiss();
    }
    
    private void showDocumentDetails() {
        String details = "Name: " + document.getName() + "\n" +
                        "Size: " + formatFileSize(document.length()) + "\n" +
                        "Path: " + document.getAbsolutePath() + "\n" +
                        "Last Modified: " + DateUtils.formatDateTime(requireContext(), 
                            document.lastModified(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        
        CustomDialogUtils.showDetailsDialog(requireContext(), "Document Details", details);
        dismiss();
    }
    
    private void showDeleteConfirmation() {
        String message = "Are you sure you want to delete \"" + document.getName() + "\"? This action cannot be undone.";
        CustomDialogUtils.showConfirmDialog(requireContext(), "Delete Document", message, "Delete", () -> {
            deleteDocument();
        });
        dismiss();
    }
    
    private void deleteDocument() {
        if (document.delete()) {
            if (listener != null) {
                listener.onDocumentDeleted(document);
            }
            Toast.makeText(requireContext(), "Document deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Failed to delete document", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    private String getFileNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
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
