package com.curosoft.konvert.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.List;

/**
 * Professional external file opener utility that handles all file types
 * Opens files in appropriate external apps with proper MIME type detection
 * Compatible with Android Scoped Storage and modern Android versions
 */
public class ExternalFileOpener {
    private static final String TAG = "ExternalFileOpener";
    
    // File types that the app can handle internally
    private static final String[] INTERNAL_SUPPORTED_TYPES = {
        "application/pdf",
        "text/plain",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
        "application/msword" // DOC
    };
    
    // Common MIME types for better detection
    private static final String[] IMAGE_TYPES = {
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", 
        "image/webp", "image/tiff", "image/svg+xml"
    };
    
    private static final String[] VIDEO_TYPES = {
        "video/mp4", "video/avi", "video/mkv", "video/mov", "video/wmv", 
        "video/flv", "video/webm", "video/3gp"
    };
    
    private static final String[] AUDIO_TYPES = {
        "audio/mp3", "audio/wav", "audio/flac", "audio/aac", "audio/ogg", 
        "audio/m4a", "audio/wma"
    };
    
    private static final String[] ARCHIVE_TYPES = {
        "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
        "application/x-tar", "application/gzip"
    };

    /**
     * Open file in external app if not supported internally, otherwise return false
     * @param context The context
     * @param filePath Path to the file
     * @param fileName Name of the file (optional, for display)
     * @return true if file was opened externally, false if should be handled internally
     */
    public static boolean openFileIfNotSupported(Context context, String filePath, String fileName) {
        if (filePath == null) {
            showError(context, "File path is null");
            return true; // Handled (with error)
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            showError(context, "File not found: " + (fileName != null ? fileName : file.getName()));
            return true; // Handled (with error)
        }
        
        String mimeType = getMimeType(file);
        Log.d(TAG, "File: " + fileName + ", MIME type: " + mimeType);
        
        // Check if our app can handle this file type internally
        if (isInternallySupportedType(mimeType)) {
            Log.d(TAG, "File type supported internally: " + mimeType);
            return false; // Let the app handle it internally
        }
        
        // File is not supported internally, open with external app
        openWithExternalApp(context, file, mimeType, fileName);
        return true; // Handled externally
    }
    
    /**
     * Force open file with external app (even if supported internally)
     * @param context The context
     * @param filePath Path to the file
     * @param fileName Name of the file (optional)
     */
    public static void openWithExternalApp(Context context, String filePath, String fileName) {
        if (filePath == null) {
            showError(context, "File path is null");
            return;
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            showError(context, "File not found: " + (fileName != null ? fileName : file.getName()));
            return;
        }
        
        String mimeType = getMimeType(file);
        openWithExternalApp(context, file, mimeType, fileName);
    }
    
    /**
     * Open file with external app using proper content:// URI
     */
    private static void openWithExternalApp(Context context, File file, String mimeType, String fileName) {
        try {
            // Create content:// URI using FileProvider for Android 7+ compatibility
            Uri fileUri;
            try {
                fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    file
                );
                Log.d(TAG, "FileProvider URI created: " + fileUri);
                Log.d(TAG, "File path: " + file.getAbsolutePath());
                Log.d(TAG, "File exists: " + file.exists());
                Log.d(TAG, "File size: " + file.length() + " bytes");
            } catch (Exception e) {
                Log.w(TAG, "FileProvider failed, falling back to file:// URI", e);
                fileUri = Uri.fromFile(file);
            }
            
            // Create intent to open file
            Intent intent = createOptimizedIntent(fileUri, mimeType);
            
            // For images, ensure we get image viewers specifically
            if (isImageType(mimeType)) {
                Log.d(TAG, "Opening image file: " + file.getName() + " with MIME type: " + mimeType);
            }
            
            // Check if any app can handle this file type
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            
            // Log available apps for debugging
            Log.d(TAG, "Found " + activities.size() + " apps for MIME type: " + mimeType);
            for (ResolveInfo info : activities) {
                Log.d(TAG, "  Available app: " + info.activityInfo.packageName + " - " + info.loadLabel(packageManager));
            }
            
            if (activities.isEmpty()) {
                // Try without MATCH_DEFAULT_ONLY for broader search
                activities = packageManager.queryIntentActivities(intent, 0);
                Log.d(TAG, "Broader search found " + activities.size() + " apps");
                
                if (activities.isEmpty()) {
                    // No app found, try generic intent
                    intent.setType("*/*");
                    activities = packageManager.queryIntentActivities(intent, 0);
                    Log.d(TAG, "Generic search found " + activities.size() + " apps");
                    
                    if (activities.isEmpty()) {
                        showError(context, "No app found to open this file type: " + getFileTypeDescription(mimeType));
                        return;
                    }
                }
            }
            
            // If multiple apps available, show chooser
            if (activities.size() > 1) {
                String chooserTitle = isImageType(mimeType) ? "Open image with" : 
                                    isVideoType(mimeType) ? "Open video with" : 
                                    isAudioType(mimeType) ? "Open audio with" : "Open with";
                Intent chooser = Intent.createChooser(intent, chooserTitle);
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
                Log.d(TAG, "Opened file chooser for: " + (fileName != null ? fileName : file.getName()));
            } else {
                // Only one app available, open directly
                context.startActivity(intent);
                Log.d(TAG, "Opened file directly with: " + activities.get(0).activityInfo.packageName + " for " + mimeType);
            }
            
            // Show success message
            String fileDisplayName = fileName != null ? fileName : file.getName();
            String fileType = getFileTypeDescription(mimeType);
            Toast.makeText(context, "Opening " + fileType.toLowerCase() + ": " + fileDisplayName + "...", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening file with external app", e);
            showError(context, "Unable to open file: " + e.getMessage());
        }
    }
    
    /**
     * Create optimized intent for specific file types
     */
    private static Intent createOptimizedIntent(Uri fileUri, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Add specific categories for different file types
        if (isImageType(mimeType)) {
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            // Ensure we target image viewing apps specifically
            intent.setAction(Intent.ACTION_VIEW);
        } else if (isVideoType(mimeType)) {
            intent.addCategory(Intent.CATEGORY_DEFAULT);
        } else if (isAudioType(mimeType)) {
            intent.addCategory(Intent.CATEGORY_DEFAULT);
        }
        
        return intent;
    }

    /**
     * Get MIME type for file using multiple detection methods
     */
    private static String getMimeType(File file) {
        String mimeType = null;
        String fileName = file.getName();
        
        // Method 1: Try MimeTypeMap
        String extension = getFileExtension(fileName);
        if (extension != null) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        
        // Method 2: Manual detection for common types
        if (mimeType == null && fileName != null) {
            String lowerName = fileName.toLowerCase();
            
            // Images
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) mimeType = "image/jpeg";
            else if (lowerName.endsWith(".png")) mimeType = "image/png";
            else if (lowerName.endsWith(".gif")) mimeType = "image/gif";
            else if (lowerName.endsWith(".webp")) mimeType = "image/webp";
            else if (lowerName.endsWith(".bmp")) mimeType = "image/bmp";
            else if (lowerName.endsWith(".tiff") || lowerName.endsWith(".tif")) mimeType = "image/tiff";
            else if (lowerName.endsWith(".svg")) mimeType = "image/svg+xml";
            
            // Videos
            else if (lowerName.endsWith(".mp4")) mimeType = "video/mp4";
            else if (lowerName.endsWith(".avi")) mimeType = "video/x-msvideo";
            else if (lowerName.endsWith(".mkv")) mimeType = "video/x-matroska";
            else if (lowerName.endsWith(".mov")) mimeType = "video/quicktime";
            else if (lowerName.endsWith(".wmv")) mimeType = "video/x-ms-wmv";
            else if (lowerName.endsWith(".flv")) mimeType = "video/x-flv";
            else if (lowerName.endsWith(".webm")) mimeType = "video/webm";
            else if (lowerName.endsWith(".3gp")) mimeType = "video/3gpp";
            
            // Audio
            else if (lowerName.endsWith(".mp3")) mimeType = "audio/mpeg";
            else if (lowerName.endsWith(".wav")) mimeType = "audio/wav";
            else if (lowerName.endsWith(".flac")) mimeType = "audio/flac";
            else if (lowerName.endsWith(".aac")) mimeType = "audio/aac";
            else if (lowerName.endsWith(".ogg")) mimeType = "audio/ogg";
            else if (lowerName.endsWith(".m4a")) mimeType = "audio/mp4";
            else if (lowerName.endsWith(".wma")) mimeType = "audio/x-ms-wma";
            
            // Documents
            else if (lowerName.endsWith(".pdf")) mimeType = "application/pdf";
            else if (lowerName.endsWith(".txt")) mimeType = "text/plain";
            else if (lowerName.endsWith(".docx")) mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            else if (lowerName.endsWith(".doc")) mimeType = "application/msword";
            else if (lowerName.endsWith(".xlsx")) mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            else if (lowerName.endsWith(".xls")) mimeType = "application/vnd.ms-excel";
            else if (lowerName.endsWith(".pptx")) mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            else if (lowerName.endsWith(".ppt")) mimeType = "application/vnd.ms-powerpoint";
            
            // Archives
            else if (lowerName.endsWith(".zip")) mimeType = "application/zip";
            else if (lowerName.endsWith(".rar")) mimeType = "application/x-rar-compressed";
            else if (lowerName.endsWith(".7z")) mimeType = "application/x-7z-compressed";
            else if (lowerName.endsWith(".tar")) mimeType = "application/x-tar";
            else if (lowerName.endsWith(".gz")) mimeType = "application/gzip";
        }
        
        // Fallback to generic binary type
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        
        return mimeType;
    }
    
    /**
     * Get file extension from filename
     */
    private static String getFileExtension(String fileName) {
        if (fileName == null) return null;
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return null;
    }
    
    /**
     * Check if file type is supported internally by the app
     */
    private static boolean isInternallySupportedType(String mimeType) {
        if (mimeType == null) return false;
        
        for (String supportedType : INTERNAL_SUPPORTED_TYPES) {
            if (supportedType.equals(mimeType)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get user-friendly description of file type
     */
    private static String getFileTypeDescription(String mimeType) {
        if (mimeType == null) return "Unknown";
        
        if (isImageType(mimeType)) return "Image";
        if (isVideoType(mimeType)) return "Video";
        if (isAudioType(mimeType)) return "Audio";
        if (isArchiveType(mimeType)) return "Archive";
        if (mimeType.startsWith("text/")) return "Text Document";
        if (mimeType.startsWith("application/")) return "Document";
        
        return mimeType;
    }
    
    private static boolean isImageType(String mimeType) {
        if (mimeType == null) return false;
        for (String type : IMAGE_TYPES) {
            if (type.equals(mimeType)) return true;
        }
        return mimeType.startsWith("image/");
    }
    
    private static boolean isVideoType(String mimeType) {
        if (mimeType == null) return false;
        for (String type : VIDEO_TYPES) {
            if (type.equals(mimeType)) return true;
        }
        return mimeType.startsWith("video/");
    }
    
    private static boolean isAudioType(String mimeType) {
        if (mimeType == null) return false;
        for (String type : AUDIO_TYPES) {
            if (type.equals(mimeType)) return true;
        }
        return mimeType.startsWith("audio/");
    }
    
    private static boolean isArchiveType(String mimeType) {
        if (mimeType == null) return false;
        for (String type : ARCHIVE_TYPES) {
            if (type.equals(mimeType)) return true;
        }
        return false;
    }
    
    /**
     * Show error message to user
     */
    private static void showError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
    }
}
