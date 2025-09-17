package com.curosoft.konvert.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Professional document access manager that handles permissions and SAF gracefully
 * Like Adobe Acrobat Reader and WPS Office
 */
public class DocumentAccessManager {
    private static final String TAG = "DocumentAccessManager";
    
    private final Context context;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<Intent> documentPickerLauncher;
    private ActivityResultLauncher<Intent> folderPickerLauncher;
    private DocumentAccessListener listener;
    
    public interface DocumentAccessListener {
        void onPermissionGranted();
        void onPermissionDenied();
        void onDocumentPicked(Uri uri);
        void onFolderPicked(Uri uri);
        void onAccessError(String error);
    }
    
    public DocumentAccessManager(Fragment fragment) {
        this.context = fragment.requireContext();
        setupActivityResultLaunchers(fragment);
    }
    
    public DocumentAccessManager(Activity activity) {
        this.context = activity;
        setupActivityResultLaunchers(activity);
    }
    
    public void setListener(DocumentAccessListener listener) {
        this.listener = listener;
    }
    
    /**
     * Setup activity result launchers for Fragment
     */
    private void setupActivityResultLaunchers(Fragment fragment) {
        // Permission launcher
        permissionLauncher = fragment.registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    notifyListener(l -> l.onPermissionGranted());
                } else {
                    notifyListener(l -> l.onPermissionDenied());
                }
            }
        );
        
        // Document picker launcher
        documentPickerLauncher = fragment.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Grant persistent permissions
                        try {
                            context.getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception e) {
                            Log.w(TAG, "Could not take persistable permission", e);
                        }
                        notifyListener(l -> l.onDocumentPicked(uri));
                    }
                }
            }
        );
        
        // Folder picker launcher
        folderPickerLauncher = fragment.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Grant persistent permissions
                        try {
                            context.getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception e) {
                            Log.w(TAG, "Could not take persistable permission", e);
                        }
                        notifyListener(l -> l.onFolderPicked(uri));
                    }
                }
            }
        );
    }
    
    /**
     * Setup activity result launchers for Activity (if needed)
     */
    private void setupActivityResultLaunchers(Activity activity) {
        // For Activity-based implementation, you would need to implement this
        // differently using startActivityForResult or ActivityResultContracts
        // This is primarily designed for Fragment usage
    }
    
    /**
     * Check and request necessary permissions based on Android version
     */
    public void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - Check granular media permissions
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_AUDIO);
            }
            
            // Note: READ_MEDIA_DOCUMENTS doesn't exist, we use SAF for documents on Android 13+
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12 - Check READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
        } else {
            notifyListener(l -> l.onPermissionGranted());
        }
    }
    
    /**
     * Check if we have necessary permissions
     */
    public boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+, we primarily use SAF, so return true
            // Documents are accessed via SAF, media files via MediaStore
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) 
                   == PackageManager.PERMISSION_GRANTED;
        }
        return true; // No runtime permissions needed below Android 6
    }
    
    /**
     * Launch document picker (SAF) for single file selection
     */
    public void launchDocumentPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            
            // Set supported MIME types
            String[] mimeTypes = MediaStoreDocumentScanner.getSupportedMimeTypes();
            if (mimeTypes.length == 1) {
                intent.setType(mimeTypes[0]);
            } else {
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
            
            // Allow multiple selections if needed
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            
            documentPickerLauncher.launch(intent);
            Log.d(TAG, "Launched document picker");
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching document picker", e);
            notifyListener(l -> l.onAccessError("Unable to open document picker: " + e.getMessage()));
        }
    }
    
    /**
     * Launch folder picker (SAF) for directory access
     */
    public void launchFolderPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | 
                           Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            
            folderPickerLauncher.launch(intent);
            Log.d(TAG, "Launched folder picker");
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching folder picker", e);
            notifyListener(l -> l.onAccessError("Unable to open folder picker: " + e.getMessage()));
        }
    }
    
    /**
     * Get documents from a folder URI using SAF
     */
    public void getDocumentsFromFolder(Uri folderUri, DocumentFolderCallback callback) {
        // This would be implemented to scan documents in a SAF folder
        // For now, it's a placeholder for future enhancement
        Log.d(TAG, "Getting documents from folder: " + folderUri);
    }
    
    /**
     * Show permission explanation dialog
     */
    public void showPermissionExplanation() {
        // This should show a user-friendly explanation of why permissions are needed
        // and offer alternative access methods via SAF
        Log.d(TAG, "Should show permission explanation");
    }
    
    /**
     * Check if device supports Storage Access Framework
     */
    public boolean isSAFSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }
    
    /**
     * Helper method to safely notify listener
     */
    private void notifyListener(ListenerAction action) {
        if (listener != null) {
            try {
                action.perform(listener);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener", e);
            }
        }
    }
    
    @FunctionalInterface
    private interface ListenerAction {
        void perform(DocumentAccessListener listener);
    }
    
    @FunctionalInterface
    public interface DocumentFolderCallback {
        void onDocumentsFound(List<Uri> documents);
    }
}
