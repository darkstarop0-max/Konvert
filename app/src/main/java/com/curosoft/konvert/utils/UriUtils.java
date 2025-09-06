package com.curosoft.konvert.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

/**
 * Utility class for URI operations and file name extraction
 */
public class UriUtils {
    private static final String TAG = "UriUtils";
    
    /**
     * Get file name from URI using various methods
     */
    public static String getFileName(Context context, Uri uri) {
        String fileName = null;
        
        // Method 1: Try OpenableColumns (works for most content URIs)
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get filename from OpenableColumns", e);
        }
        
        // Method 2: Try DocumentsContract (for document provider URIs)
        if (fileName == null) {
            try {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    String documentId = DocumentsContract.getDocumentId(uri);
                    if (documentId != null && documentId.contains(":")) {
                        fileName = documentId.substring(documentId.lastIndexOf(":") + 1);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not get filename from DocumentsContract", e);
            }
        }
        
        // Method 3: Try MediaStore (for media URIs)
        if (fileName == null) {
            try {
                String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
                Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not get filename from MediaStore", e);
            }
        }
        
        // Method 4: Extract from URI path as last resort
        if (fileName == null) {
            try {
                String path = uri.getPath();
                if (path != null) {
                    int lastSlash = path.lastIndexOf('/');
                    if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                        fileName = path.substring(lastSlash + 1);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not extract filename from path", e);
            }
        }
        
        // Fallback to generic name
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "file";
        }
        
        return fileName;
    }
    
    /**
     * Get file extension from URI
     */
    public static String getFileExtension(Context context, Uri uri) {
        String fileName = getFileName(context, uri);
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        
        // Try to get from MIME type as fallback
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                if (mimeType.contains("pdf")) return "pdf";
                if (mimeType.contains("wordprocessingml") || mimeType.contains("docx")) return "docx";
                if (mimeType.contains("text")) return "txt";
                if (mimeType.contains("jpeg") || mimeType.contains("jpg")) return "jpg";
                if (mimeType.contains("png")) return "png";
                if (mimeType.contains("webp")) return "webp";
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not determine extension from MIME type", e);
        }
        
        return "";
    }
    
    /**
     * Get MIME type from URI
     */
    public static String getMimeType(Context context, Uri uri) {
        try {
            return context.getContentResolver().getType(uri);
        } catch (Exception e) {
            Log.w(TAG, "Could not get MIME type", e);
            return null;
        }
    }
    
    /**
     * Check if URI points to a valid file
     */
    public static boolean isValidFile(Context context, Uri uri) {
        try {
            Cursor cursor = context.getContentResolver().query(uri, 
                new String[]{OpenableColumns.SIZE}, null, null, null);
            if (cursor != null) {
                boolean hasSize = cursor.moveToFirst() && 
                    cursor.getColumnIndex(OpenableColumns.SIZE) >= 0;
                cursor.close();
                return hasSize;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not validate file", e);
        }
        return false;
    }
}
