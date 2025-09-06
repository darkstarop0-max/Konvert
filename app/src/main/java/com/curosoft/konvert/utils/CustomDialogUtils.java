package com.curosoft.konvert.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import com.curosoft.konvert.R;

/**
 * Utility class for creating custom themed dialogs
 */
public class CustomDialogUtils {
    
    public interface OnConfirmListener {
        void onConfirm();
    }
    
    public interface OnRenameListener {
        void onRename(String newName);
    }
    
    /**
     * Show custom rename dialog
     */
    public static void showRenameDialog(Context context, String currentName, OnRenameListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_rename_document, null);
        dialog.setContentView(view);
        
        // Make dialog background transparent so our custom background shows
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView title = view.findViewById(R.id.dialog_title);
        EditText editText = view.findViewById(R.id.dialog_edit_text);
        TextView cancelButton = view.findViewById(R.id.dialog_button_cancel);
        TextView positiveButton = view.findViewById(R.id.dialog_button_positive);
        
        title.setText("Rename Document");
        editText.setText(getFileNameWithoutExtension(currentName));
        editText.setSelectAllOnFocus(true);
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        positiveButton.setOnClickListener(v -> {
            String newName = editText.getText().toString().trim();
            if (!newName.isEmpty()) {
                listener.onRename(newName);
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    /**
     * Show custom confirmation dialog
     */
    public static void showConfirmDialog(Context context, String title, String message, 
                                       String positiveText, OnConfirmListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_confirm_action, null);
        dialog.setContentView(view);
        
        // Make dialog background transparent so our custom background shows
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView titleView = view.findViewById(R.id.dialog_title);
        TextView messageView = view.findViewById(R.id.dialog_message);
        TextView cancelButton = view.findViewById(R.id.dialog_button_cancel);
        TextView positiveButton = view.findViewById(R.id.dialog_button_positive);
        
        titleView.setText(title);
        messageView.setText(message);
        positiveButton.setText(positiveText);
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        positiveButton.setOnClickListener(v -> {
            listener.onConfirm();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    /**
     * Show custom details dialog
     */
    public static void showDetailsDialog(Context context, String title, String details) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_document_details, null);
        dialog.setContentView(view);
        
        // Make dialog background transparent so our custom background shows
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView titleView = view.findViewById(R.id.dialog_title);
        TextView messageView = view.findViewById(R.id.dialog_message);
        TextView okButton = view.findViewById(R.id.dialog_button_ok);
        
        titleView.setText(title);
        messageView.setText(details);
        
        okButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private static String getFileNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
}
