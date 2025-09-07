package com.curosoft.konvert.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

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
    
    public interface OnSingleChoiceListener {
        void onChoice(DialogInterface dialog, int selectedIndex);
    }
    
    public interface OnSaveLocationListener {
        void onLocationSelected(String newLocation);
        void onCustomLocationRequested();
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
    
    /**
     * Show single choice dialog
     */
    public static void showSingleChoiceDialog(Context context, String title, String message, 
                                            String[] options, int selectedIndex, OnSingleChoiceListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_single_choice, null);
        dialog.setContentView(view);
        
        // Make dialog background transparent so our custom background shows
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView titleView = view.findViewById(R.id.dialog_title);
        TextView messageView = view.findViewById(R.id.dialog_message);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group_options);
        TextView cancelButton = view.findViewById(R.id.dialog_button_cancel);
        TextView positiveButton = view.findViewById(R.id.dialog_button_positive);
        
        titleView.setText(title);
        
        if (message != null && !message.isEmpty()) {
            messageView.setText(message);
            messageView.setVisibility(View.VISIBLE);
        }
        
        // Add radio buttons for each option
        for (int i = 0; i < options.length; i++) {
            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(options[i]);
            radioButton.setTextSize(16);
            radioButton.setTextColor(context.getResources().getColor(R.color.text_primary, null));
            android.content.res.ColorStateList colorStateList = context.getResources().getColorStateList(R.color.clean_accent, null);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                radioButton.setButtonTintList(colorStateList);
            }
            radioButton.setPadding(0, 16, 0, 16);
            radioButton.setId(i);
            
            if (i == selectedIndex) {
                radioButton.setChecked(true);
            }
            
            radioGroup.addView(radioButton);
        }
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        positiveButton.setOnClickListener(v -> {
            int checkedId = radioGroup.getCheckedRadioButtonId();
            if (checkedId != -1 && listener != null) {
                listener.onChoice(null, checkedId);
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    /**
     * Show info dialog
     */
    public static void showInfoDialog(Context context, String title, String message, 
                                    String buttonText, Runnable onButtonClick) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_info, null);
        dialog.setContentView(view);
        
        // Make dialog background transparent so our custom background shows
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView titleView = view.findViewById(R.id.dialog_title);
        TextView messageView = view.findViewById(R.id.dialog_message);
        TextView positiveButton = view.findViewById(R.id.dialog_button_positive);
        
        titleView.setText(title);
        messageView.setText(message);
        positiveButton.setText(buttonText);
        
        positiveButton.setOnClickListener(v -> {
            if (onButtonClick != null) {
                onButtonClick.run();
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    /**
     * Show custom save location dialog
     */
    public static void showSaveLocationDialog(Context context, String currentLocation, OnSaveLocationListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_save_location, null);
        dialog.setContentView(view);
        
        // Make dialog background transparent so our custom background shows
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        TextView titleView = view.findViewById(R.id.dialog_title);
        TextView currentLocationText = view.findViewById(R.id.dialog_current_location);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group_locations);
        RadioButton radioDocuments = view.findViewById(R.id.radio_documents);
        RadioButton radioDownloads = view.findViewById(R.id.radio_downloads);
        RadioButton radioPictures = view.findViewById(R.id.radio_pictures);
        RadioButton radioCustom = view.findViewById(R.id.radio_custom);
        TextView cancelButton = view.findViewById(R.id.dialog_button_cancel);
        TextView positiveButton = view.findViewById(R.id.dialog_button_positive);
        
        currentLocationText.setText("Current: " + currentLocation);
        
        // Set default selection based on current location
        if (currentLocation.contains("Documents")) {
            radioDocuments.setChecked(true);
        } else if (currentLocation.contains("Downloads")) {
            radioDownloads.setChecked(true);
        } else if (currentLocation.contains("Pictures")) {
            radioPictures.setChecked(true);
        } else {
            radioCustom.setChecked(true);
        }
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        positiveButton.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            String newLocation;
            
            if (selectedId == R.id.radio_documents) {
                newLocation = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/Konvert";
            } else if (selectedId == R.id.radio_downloads) {
                newLocation = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            } else if (selectedId == R.id.radio_pictures) {
                newLocation = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Konvert";
            } else {
                // Custom location - call the custom location picker
                listener.onCustomLocationRequested();
                dialog.dismiss();
                return;
            }
            
            listener.onLocationSelected(newLocation);
            dialog.dismiss();
        });
        
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
