package com.curosoft.konvert.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilePickerUtils {

    private static final String TAG = "FilePickerUtils";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    // Callback interface for file selection
    public interface FileSelectionCallback {
        void onFileSelected(File file, String originalName, String mimeType, Uri uri);
        void onFileSelectionCancelled();
        void onFileSelectionError(Exception e);
    }
    
    // Check and request necessary permissions
    public static void checkAndRequestPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            String[] permissions = {
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.READ_MEDIA_VIDEO
            };
            checkPermissions(activity, permissions);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 uses scoped storage
            String[] permissions = {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            };
            checkPermissions(activity, permissions);
        } else {
            // Android 10 and below
            String[] permissions = {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            checkPermissions(activity, permissions);
        }
    }
    
    private static void checkPermissions(Activity activity, String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        }
    }
    
    // Setup file picker for a fragment
    public static ActivityResultLauncher<String> registerFilePicker(
            Fragment fragment, String category, FileSelectionCallback callback) {
        
        return fragment.registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Context context = fragment.requireContext();
                            File localFile = createTempFileFromUri(context, uri);
                            String fileName = getFileName(context, uri);
                            String mimeType = getMimeType(context, uri);
                            
                            callback.onFileSelected(localFile, fileName, mimeType, uri);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing selected file", e);
                            callback.onFileSelectionError(e);
                        }
                    } else {
                        callback.onFileSelectionCancelled();
                    }
                }
        );
    }
    
    // Get MIME type based on category
    public static String getMimeTypeForCategory(String category) {
        switch (category.toLowerCase()) {
            case "docs":
                return "application/*";
            case "images":
                return "image/*";
            case "audio":
                return "audio/*";
            case "video":
                return "video/*";
            case "archives":
                return "*/*"; // For archives, we might need to filter afterward
            default:
                return "*/*";
        }
    }
    
    // Get file name from Uri
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    
    // Get MIME type from Uri
    public static String getMimeType(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (extension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            }
        }
        return mimeType != null ? mimeType : "application/octet-stream";
    }
    
    // Create a temporary file from Uri
    private static File createTempFileFromUri(Context context, Uri uri) throws IOException {
        String fileName = getFileName(context, uri);
        String fileExtension = "";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            fileExtension = fileName.substring(dotIndex);
        }
        
        File tempFile = File.createTempFile("konvert_", fileExtension, context.getCacheDir());
        tempFile.deleteOnExit();
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {
            
            if (inputStream == null) {
                throw new IOException("Failed to open input stream");
            }
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
        
        return tempFile;
    }
}
