package com.curosoft.konvert.utils;

import android.app.Activity;
import android.content.ContentResolver;
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

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnhancedFilePickerUtils {

    private static final String TAG = "EnhancedFilePickerUtils";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    // Define supported file types
    public static final class SupportedFileTypes {
        // Document formats
        public static final String[] DOCS = {
            "docx", "pdf", "txt"
        };
        
        // Image formats
        public static final String[] IMAGES = {
            "jpg", "jpeg", "png", "webp", "heic", "bmp", "gif"
        };
        
        // Audio formats
        public static final String[] AUDIO = {
            "mp3", "wav", "aac", "ogg", "m4a"
        };
        
        // Video formats
        public static final String[] VIDEO = {
            "mp4", "mov", "mkv", "avi", "webm"
        };
        
        // Archive formats
        public static final String[] ARCHIVES = {
            "zip", "rar", "7z", "tar.gz"
        };
        
        // Maps extensions to MIME types
        private static final Map<String, String> MIME_TYPES = new HashMap<>();
        
        static {
            // Document MIME types
            MIME_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            MIME_TYPES.put("pdf", "application/pdf");
            MIME_TYPES.put("txt", "text/plain");
            
            // Image MIME types
            MIME_TYPES.put("jpg", "image/jpeg");
            MIME_TYPES.put("jpeg", "image/jpeg");
            MIME_TYPES.put("png", "image/png");
            MIME_TYPES.put("webp", "image/webp");
            MIME_TYPES.put("heic", "image/heic");
            MIME_TYPES.put("bmp", "image/bmp");
            MIME_TYPES.put("gif", "image/gif");
            
            // Audio MIME types
            MIME_TYPES.put("mp3", "audio/mpeg");
            MIME_TYPES.put("wav", "audio/wav");
            MIME_TYPES.put("aac", "audio/aac");
            MIME_TYPES.put("ogg", "audio/ogg");
            MIME_TYPES.put("m4a", "audio/m4a");
            
            // Video MIME types
            MIME_TYPES.put("mp4", "video/mp4");
            MIME_TYPES.put("mov", "video/quicktime");
            MIME_TYPES.put("mkv", "video/x-matroska");
            MIME_TYPES.put("avi", "video/x-msvideo");
            MIME_TYPES.put("webm", "video/webm");
            
            // Archive MIME types
            MIME_TYPES.put("zip", "application/zip");
            MIME_TYPES.put("rar", "application/x-rar-compressed");
            MIME_TYPES.put("7z", "application/x-7z-compressed");
            MIME_TYPES.put("tar.gz", "application/gzip");
        }
        
        public static String getMimeTypeForExtension(String extension) {
            return MIME_TYPES.getOrDefault(extension.toLowerCase(), "application/octet-stream");
        }
        
        public static String[] getMimeTypesForCategory(String category) {
            String[] extensions;
            
            switch (category.toLowerCase()) {
                case "docs":
                    extensions = DOCS;
                    break;
                case "images":
                    extensions = IMAGES;
                    break;
                case "audio":
                    extensions = AUDIO;
                    break;
                case "video":
                    extensions = VIDEO;
                    break;
                case "archives":
                    extensions = ARCHIVES;
                    break;
                default:
                    return new String[] {"*/*"};
            }
            
            List<String> mimeTypes = new ArrayList<>();
            for (String ext : extensions) {
                String mimeType = getMimeTypeForExtension(ext);
                if (mimeType != null) {
                    mimeTypes.add(mimeType);
                }
            }
            
            return mimeTypes.toArray(new String[0]);
        }
    }
    
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
    
    // Custom contract for opening documents with specific MIME types
    public static class OpenDocumentWithMultipleTypes extends ActivityResultContract<String, Uri> {
        private final String category;
        
        public OpenDocumentWithMultipleTypes(String category) {
            this.category = category;
        }
        
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, String input) {
            // Create intent with ACTION_OPEN_DOCUMENT
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            
            // Get MIME types for the selected category
            String[] mimeTypes = SupportedFileTypes.getMimeTypesForCategory(category);
            
            // Set type to */* and extra MIME types for filtering
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            
            // Allow multiple selections if needed
            // intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            
            return intent;
        }
        
        @Override
        public Uri parseResult(int resultCode, @Nullable Intent intent) {
            return (intent == null || resultCode != Activity.RESULT_OK) ? null : intent.getData();
        }
    }
    
    // Setup file picker for a fragment using the OpenDocument contract
    public static ActivityResultLauncher<String[]> registerFilePicker(
            Fragment fragment, String category, FileSelectionCallback callback) {
        
        return fragment.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            Context context = fragment.requireContext();
                            
                            // Skip taking persistent URI permission as it's not available in all API levels
                            
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
        
        // If still null, try to determine from file extension
        if (mimeType == null) {
            String fileName = getFileName(context, uri);
            if (fileName != null) {
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot >= 0 && lastDot < fileName.length() - 1) {
                    String extension = fileName.substring(lastDot + 1).toLowerCase();
                    mimeType = SupportedFileTypes.getMimeTypeForExtension(extension);
                }
            }
        }
        
        return mimeType != null ? mimeType : "application/octet-stream";
    }
    
    // Check if a file extension is supported for a category
    public static boolean isExtensionSupportedForCategory(String extension, String category) {
        if (extension == null || category == null) {
            return false;
        }
        
        String[] supportedExtensions;
        switch (category.toLowerCase()) {
            case "docs":
                supportedExtensions = SupportedFileTypes.DOCS;
                break;
            case "images":
                supportedExtensions = SupportedFileTypes.IMAGES;
                break;
            case "audio":
                supportedExtensions = SupportedFileTypes.AUDIO;
                break;
            case "video":
                supportedExtensions = SupportedFileTypes.VIDEO;
                break;
            case "archives":
                supportedExtensions = SupportedFileTypes.ARCHIVES;
                break;
            default:
                return true;
        }
        
        for (String ext : supportedExtensions) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        
        return false;
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
