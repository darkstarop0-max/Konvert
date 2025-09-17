package com.curosoft.konvert.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Professional MediaStore-based document scanner similar to Adobe Acrobat Reader and WPS Office
 * Uses Android's MediaStore APIs for efficient, Play Store compliant document discovery
 */
public class MediaStoreDocumentScanner {
    private static final String TAG = "MediaStoreDocumentScanner";
    
    // Supported document MIME types for MediaStore queries
    private static final String[] DOCUMENT_MIME_TYPES = {
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
        "application/msword", // DOC
        "text/plain" // TXT
    };
    
    // File extensions for fallback filtering
    private static final String[] DOCUMENT_EXTENSIONS = {
        ".pdf", ".docx", ".doc", ".txt"
    };
    
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Map<String, DocumentInfo> documentCache;
    private ContentObserver mediaStoreObserver;
    private DocumentScanListener listener;
    private boolean isScanning = false;
    
    public interface DocumentScanListener {
        void onScanStarted();
        void onDocumentsFound(List<DocumentInfo> documents);
        void onScanCompleted(int totalFound);
        void onScanError(String error);
        void onPermissionRequired();
    }
    
    public static class DocumentInfo {
        public final long id;
        public final String name;
        public final String path;
        public final String mimeType;
        public final long size;
        public final long dateModified;
        public final Uri uri;
        
        public DocumentInfo(long id, String name, String path, String mimeType, 
                          long size, long dateModified, Uri uri) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.mimeType = mimeType;
            this.size = size;
            this.dateModified = dateModified;
            this.uri = uri;
        }
        
        public File toFile() {
            return new File(path);
        }
    }
    
    public MediaStoreDocumentScanner(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.documentCache = new ConcurrentHashMap<>();
        setupMediaStoreObserver();
    }
    
    public void setListener(DocumentScanListener listener) {
        this.listener = listener;
    }
    
    /**
     * Start scanning for documents using MediaStore
     */
    public void startScan() {
        if (isScanning) {
            Log.d(TAG, "Scan already in progress");
            return;
        }
        
        isScanning = true;
        notifyListener(l -> l.onScanStarted());
        
        executorService.execute(() -> {
            try {
                List<DocumentInfo> documents = scanDocumentsFromMediaStore();
                
                // Cache the results
                documentCache.clear();
                for (DocumentInfo doc : documents) {
                    documentCache.put(doc.path, doc);
                }
                
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onDocumentsFound(documents);
                        listener.onScanCompleted(documents.size());
                    }
                    isScanning = false;
                });
                
                Log.d(TAG, "MediaStore scan completed. Found " + documents.size() + " documents");
                
            } catch (SecurityException e) {
                Log.w(TAG, "Permission denied for MediaStore access", e);
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onPermissionRequired();
                    }
                    isScanning = false;
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during MediaStore scan", e);
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onScanError("Error scanning documents: " + e.getMessage());
                    }
                    isScanning = false;
                });
            }
        });
    }
    
    /**
     * Get cached documents without re-scanning
     */
    public List<DocumentInfo> getCachedDocuments() {
        return new ArrayList<>(documentCache.values());
    }
    
    /**
     * Check if cache is empty
     */
    public boolean isCacheEmpty() {
        return documentCache.isEmpty();
    }
    
    /**
     * Clear the document cache
     */
    public void clearCache() {
        documentCache.clear();
    }
    
    /**
     * Scan documents using MediaStore API - the professional way
     */
    private List<DocumentInfo> scanDocumentsFromMediaStore() {
        List<DocumentInfo> documents = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        
        // For Android 10+ (API 29+), use MediaStore.Files to query all file types
        Uri collection = MediaStore.Files.getContentUri("external");
        
        // Projection - columns we want to retrieve
        String[] projection = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA, // Full path
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        };
        
        // Selection - filter for document types
        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();
        
        // Add MIME type filters
        for (int i = 0; i < DOCUMENT_MIME_TYPES.length; i++) {
            if (i > 0) selection.append(" OR ");
            selection.append(MediaStore.Files.FileColumns.MIME_TYPE).append(" = ?");
            selectionArgs.add(DOCUMENT_MIME_TYPES[i]);
        }
        
        // Add file extension filters as fallback (for files without proper MIME types)
        for (String extension : DOCUMENT_EXTENSIONS) {
            selection.append(" OR ").append(MediaStore.Files.FileColumns.DATA).append(" LIKE ?");
            selectionArgs.add("%" + extension);
        }
        
        // Sort by date modified (newest first)
        String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";
        
        try (Cursor cursor = resolver.query(
                collection,
                projection,
                selection.toString(),
                selectionArgs.toArray(new String[0]),
                sortOrder
        )) {
            
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                int mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
                
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String path = cursor.getString(pathColumn);
                    String mimeType = cursor.getString(mimeColumn);
                    long size = cursor.getLong(sizeColumn);
                    long dateModified = cursor.getLong(dateColumn);
                    
                    // Validate the file exists and is accessible
                    if (path != null && name != null) {
                        File file = new File(path);
                        if (file.exists() && file.canRead() && isDocumentFile(file)) {
                            Uri uri = ContentUris.withAppendedId(collection, id);
                            DocumentInfo doc = new DocumentInfo(id, name, path, mimeType, 
                                                              size, dateModified, uri);
                            documents.add(doc);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore", e);
            throw e;
        }
        
        return documents;
    }
    
    /**
     * Check if file is a supported document type
     */
    private boolean isDocumentFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String name = file.getName().toLowerCase();
        for (String extension : DOCUMENT_EXTENSIONS) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Setup MediaStore content observer for automatic updates
     */
    private void setupMediaStoreObserver() {
        mediaStoreObserver = new ContentObserver(mainHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                Log.d(TAG, "MediaStore changed, refreshing documents");
                
                // Refresh after a short delay to avoid rapid successive updates
                mainHandler.removeCallbacks(refreshRunnable);
                mainHandler.postDelayed(refreshRunnable, 1000);
            }
        };
        
        // Register observer for file changes
        context.getContentResolver().registerContentObserver(
                MediaStore.Files.getContentUri("external"),
                true,
                mediaStoreObserver
        );
    }
    
    private final Runnable refreshRunnable = () -> {
        if (!isScanning) {
            startScan();
        }
    };
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        if (mediaStoreObserver != null) {
            context.getContentResolver().unregisterContentObserver(mediaStoreObserver);
        }
        
        mainHandler.removeCallbacks(refreshRunnable);
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        isScanning = false;
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
        void perform(DocumentScanListener listener);
    }
    
    /**
     * Get supported MIME types for document picker
     */
    public static String[] getSupportedMimeTypes() {
        return DOCUMENT_MIME_TYPES.clone();
    }
    
    /**
     * Check if a URI represents a document file
     */
    public static boolean isDocumentUri(Context context, Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            String mimeType = resolver.getType(uri);
            
            if (mimeType != null) {
                for (String supportedType : DOCUMENT_MIME_TYPES) {
                    if (mimeType.equals(supportedType)) {
                        return true;
                    }
                }
            }
            
            // Fallback to file extension check
            String path = uri.getPath();
            if (path != null) {
                String lowercasePath = path.toLowerCase();
                for (String extension : DOCUMENT_EXTENSIONS) {
                    if (lowercasePath.endsWith(extension)) {
                        return true;
                    }
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking document URI", e);
        }
        
        return false;
    }
}
