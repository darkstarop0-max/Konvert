package com.curosoft.konvert.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Real-time file scanner that monitors document directories for changes
 * and notifies listeners when files are added, removed, or modified.
 * Enhanced to scan entire device storage with proper permissions.
 */
public class DocumentFileScanner {
    private static final String TAG = "DocumentFileScanner";
    
    // Document file extensions to monitor - only supported formats
    private static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList(
        ".pdf", ".docx", ".txt"
    );
    
    // Common directories to scan with high priority
    private static final List<String> PRIORITY_DIRECTORIES = Arrays.asList(
        "Documents", "Download", "Downloads", "DCIM"
    );
    
    private final Context context;
    private final List<FileChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final List<DirectoryObserver> observers = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();
    
    // Cache for scanned documents
    private final Set<String> documentPaths = new HashSet<>();
    private volatile boolean isScanning = false;
    
    public interface FileChangeListener {
        void onFilesChanged(List<File> documents);
        void onFileAdded(File file);
        void onFileRemoved(File file);
        void onScanStarted();
        void onScanProgress(int current, int total);
        void onScanCompleted();
    }
    
    public DocumentFileScanner(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Start monitoring document directories for changes
     */
    public void startScanning() {
        if (isScanning) {
            return;
        }
        
        stopScanning(); // Stop any existing observers
        isScanning = true;
        
        // Notify listeners that scanning started
        mainHandler.post(() -> {
            for (FileChangeListener listener : listeners) {
                listener.onScanStarted();
            }
        });
        
        // Start comprehensive scan in background
        scanExecutor.execute(this::performComprehensiveScan);
    }
    
    /**
     * Perform comprehensive document scan using multiple approaches
     */
    private void performComprehensiveScan() {
        try {
            List<File> allDocuments = new ArrayList<>();
            documentPaths.clear();
            
            // 1. Scan using MediaStore (fastest for supported formats)
            if (hasStoragePermissions()) {
                List<File> mediaStoreFiles = scanUsingMediaStore();
                addUniqueFiles(allDocuments, mediaStoreFiles);
                Log.d(TAG, "MediaStore scan found: " + mediaStoreFiles.size() + " documents");
            }
            
            // 2. Scan common directories with file system
            List<File> fileSystemFiles = scanFileSystemDirectories();
            addUniqueFiles(allDocuments, fileSystemFiles);
            Log.d(TAG, "File system scan found: " + fileSystemFiles.size() + " documents");
            
            // 3. Setup file observers for real-time monitoring
            setupFileObservers();
            
            // Notify completion
            isScanning = false;
            mainHandler.post(() -> {
                for (FileChangeListener listener : listeners) {
                    listener.onFilesChanged(allDocuments);
                    listener.onScanCompleted();
                }
            });
            
            Log.d(TAG, "Total documents found: " + allDocuments.size());
            
        } catch (Exception e) {
            Log.e(TAG, "Error during comprehensive scan", e);
            isScanning = false;
            mainHandler.post(() -> {
                for (FileChangeListener listener : listeners) {
                    listener.onScanCompleted();
                }
            });
        }
    }
    
    /**
     * Scan documents using MediaStore API (most efficient)
     */
    private List<File> scanUsingMediaStore() {
        List<File> documents = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore.Files for documents
            String[] projection = {
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            };
            
            // Create selection for supported document types
            StringBuilder selection = new StringBuilder();
            String[] selectionArgs = new String[DOCUMENT_EXTENSIONS.size()];
            
            for (int i = 0; i < DOCUMENT_EXTENSIONS.size(); i++) {
                if (i > 0) selection.append(" OR ");
                selection.append(MediaStore.Files.FileColumns.DATA + " LIKE ?");
                selectionArgs[i] = "%" + DOCUMENT_EXTENSIONS.get(i);
            }
            
            try (Cursor cursor = context.getContentResolver().query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selection.toString(),
                    selectionArgs,
                    MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
            )) {
                if (cursor != null) {
                    int dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    
                    while (cursor.moveToNext()) {
                        if (dataIndex >= 0) {
                            String filePath = cursor.getString(dataIndex);
                            if (filePath != null) {
                                File file = new File(filePath);
                                if (file.exists() && isDocumentFile(file)) {
                                    documents.add(file);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "MediaStore scan failed", e);
            }
        }
        
        return documents;
    }
    
    /**
     * Scan file system directories
     */
    private List<File> scanFileSystemDirectories() {
        List<File> documents = new ArrayList<>();
        List<File> directoriesToScan = getDirectoriesToScan();
        
        int totalDirectories = directoriesToScan.size();
        int processedDirectories = 0;
        
        for (File directory : directoriesToScan) {
            if (directory.exists() && directory.canRead()) {
                try {
                    scanDirectoryRecursively(directory, documents, 0, 3); // Reduced depth to prevent ANR
                } catch (Exception e) {
                    Log.w(TAG, "Error scanning directory: " + directory.getPath(), e);
                }
            }
            
            processedDirectories++;
            final int currentProgress = processedDirectories;
            final int total = totalDirectories;
            mainHandler.post(() -> {
                for (FileChangeListener listener : listeners) {
                    listener.onScanProgress(currentProgress, total);
                }
            });
        }
        
        return documents;
    }
    
    /**
     * Get comprehensive list of directories to scan
     */
    private List<File> getDirectoriesToScan() {
        List<File> directories = new ArrayList<>();
        
        // External storage directories
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File externalStorage = Environment.getExternalStorageDirectory();
            if (externalStorage != null && externalStorage.exists()) {
                // Add priority directories
                for (String dirName : PRIORITY_DIRECTORIES) {
                    File dir = new File(externalStorage, dirName);
                    if (dir.exists() && dir.canRead()) {
                        directories.add(dir);
                    }
                }
                
                // Add standard Android directories
                directories.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
                directories.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            }
        }
        
        // App-specific external directories
        File[] externalDirs = context.getExternalFilesDirs(null);
        if (externalDirs != null) {
            for (File dir : externalDirs) {
                if (dir != null && dir.exists()) {
                    directories.add(dir);
                }
            }
        }
        
        // Internal app directories
        File internalDir = context.getFilesDir();
        if (internalDir != null && internalDir.exists()) {
            directories.add(internalDir);
        }
        
        // Remove duplicates
        Set<String> addedPaths = new HashSet<>();
        List<File> uniqueDirectories = new ArrayList<>();
        for (File dir : directories) {
            if (dir != null && addedPaths.add(dir.getAbsolutePath())) {
                uniqueDirectories.add(dir);
            }
        }
        
        return uniqueDirectories;
    }
    
    /**
     * Setup file observers for real-time monitoring
     */
    private void setupFileObservers() {
        List<File> directoriesToMonitor = getDirectoriesToScan();
        
        for (File directory : directoriesToMonitor) {
            if (directory.exists() && directory.canRead()) {
                try {
                    DirectoryObserver observer = new DirectoryObserver(directory.getAbsolutePath());
                    observer.startWatching();
                    observers.add(observer);
                    Log.d(TAG, "Started monitoring: " + directory.getAbsolutePath());
                } catch (Exception e) {
                    Log.w(TAG, "Could not monitor directory: " + directory.getPath(), e);
                }
            }
        }
    }
    
    /**
     * Add unique files to list (avoid duplicates)
     */
    private void addUniqueFiles(List<File> targetList, List<File> sourceList) {
        for (File file : sourceList) {
            String path = file.getAbsolutePath();
            if (documentPaths.add(path)) {
                targetList.add(file);
            }
        }
    }
    
    /**
     * Check if app has necessary storage permissions
     */
    private boolean hasStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            return ContextCompat.checkSelfPermission(context, "android.permission.READ_MEDIA_DOCUMENTS") 
                   == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) 
                   == PackageManager.PERMISSION_GRANTED;
        }
        return true; // No runtime permissions needed below Android 6
    }
    
    /**
     * Stop monitoring directories
     */
    public void stopScanning() {
        isScanning = false;
        for (DirectoryObserver observer : observers) {
            try {
                observer.stopWatching();
            } catch (Exception e) {
                Log.w(TAG, "Error stopping observer", e);
            }
        }
        observers.clear();
    }
    
    /**
     * Add a listener for file changes
     */
    public void addFileChangeListener(FileChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a file change listener
     */
    public void removeFileChangeListener(FileChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Manually trigger a scan for documents
     */
    public void scanForDocuments() {
        if (!isScanning) {
            startScanning();
        }
    }
    
    /**
     * Recursively scan directory for document files with depth limit
     */
    private void scanDirectoryRecursively(File directory, List<File> documents, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth) {
            return;
        }
        
        // Add small delay to prevent overwhelming the system
        if (currentDepth > 0) {
            try {
                Thread.sleep(5); // Small pause to reduce CPU load
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        
        try {
            File[] files = directory.listFiles();
            if (files != null) {
                // Process files first (faster)
                for (File file : files) {
                    try {
                        if (file.isFile() && isDocumentFile(file)) {
                            documents.add(file);
                        }
                    } catch (SecurityException e) {
                        Log.d(TAG, "Access denied: " + file.getPath());
                    }
                }
                
                // Then process directories with system exclusions
                for (File file : files) {
                    try {
                        if (file.isDirectory() && !file.getName().startsWith(".")) {
                            String dirName = file.getName().toLowerCase();
                            // Skip system and cache directories that won't have user documents
                            if (!dirName.equals("android") && 
                                !dirName.equals("data") && 
                                !dirName.equals("cache") && 
                                !dirName.equals("temp") &&
                                !dirName.equals("proc") &&
                                !dirName.equals("sys") &&
                                !dirName.contains("cache")) {
                                scanDirectoryRecursively(file, documents, currentDepth + 1, maxDepth);
                            }
                        }
                    } catch (SecurityException e) {
                        Log.d(TAG, "Access denied: " + file.getPath());
                    }
                }
            }
        } catch (SecurityException e) {
            Log.d(TAG, "Cannot list directory: " + directory.getPath());
        }
    }
    
    /**
     * Check if a file is a document based on its extension
     */
    private boolean isDocumentFile(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        for (String extension : DOCUMENT_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Custom FileObserver to monitor directory changes
     */
    private class DirectoryObserver extends FileObserver {
        private final String path;
        
        public DirectoryObserver(String path) {
            super(path, FileObserver.CREATE | FileObserver.DELETE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO);
            this.path = path;
        }
        
        @Override
        public void onEvent(int event, @Nullable String path) {
            if (path == null) return;
            
            File file = new File(this.path, path);
            
            // Only handle document files
            if (!isDocumentFile(file)) return;
            
            switch (event & FileObserver.ALL_EVENTS) {
                case FileObserver.CREATE:
                case FileObserver.MOVED_TO:
                    mainHandler.post(() -> {
                        for (FileChangeListener listener : listeners) {
                            listener.onFileAdded(file);
                        }
                        // Trigger full scan to update the list
                        scanForDocuments();
                    });
                    break;
                    
                case FileObserver.DELETE:
                case FileObserver.MOVED_FROM:
                    mainHandler.post(() -> {
                        for (FileChangeListener listener : listeners) {
                            listener.onFileRemoved(file);
                        }
                        // Trigger full scan to update the list
                        scanForDocuments();
                    });
                    break;
            }
        }
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        stopScanning();
        listeners.clear();
        if (!scanExecutor.isShutdown()) {
            scanExecutor.shutdown();
        }
    }
}
