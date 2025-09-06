package com.curosoft.konvert.utils;

import android.content.Context;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Real-time file scanner that monitors document directories for changes
 * and notifies listeners when files are added, removed, or modified.
 */
public class DocumentFileScanner {
    private static final String TAG = "DocumentFileScanner";
    
    // Document file extensions to monitor - only supported formats
    private static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList(
        ".pdf", ".docx", ".txt"
    );
    
    private final Context context;
    private final List<FileChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final List<DirectoryObserver> observers = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public interface FileChangeListener {
        void onFilesChanged(List<File> documents);
        void onFileAdded(File file);
        void onFileRemoved(File file);
    }
    
    public DocumentFileScanner(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Start monitoring document directories for changes
     */
    public void startScanning() {
        stopScanning(); // Stop any existing observers
        
        List<File> directoriesToMonitor = getDocumentDirectories();
        
        for (File directory : directoriesToMonitor) {
            if (directory.exists() && directory.canRead()) {
                DirectoryObserver observer = new DirectoryObserver(directory.getAbsolutePath());
                observer.startWatching();
                observers.add(observer);
                Log.d(TAG, "Started monitoring: " + directory.getAbsolutePath());
            }
        }
        
        // Initial scan
        scanForDocuments();
    }
    
    /**
     * Stop monitoring directories
     */
    public void stopScanning() {
        for (DirectoryObserver observer : observers) {
            observer.stopWatching();
        }
        observers.clear();
    }
    
    /**
     * Add a listener for file changes
     */
    public void addFileChangeListener(FileChangeListener listener) {
        if (!listeners.contains(listener)) {
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
        new Thread(() -> {
            List<File> documents = findAllDocuments();
            mainHandler.post(() -> {
                for (FileChangeListener listener : listeners) {
                    listener.onFilesChanged(documents);
                }
            });
        }).start();
    }
    
    /**
     * Get list of directories to monitor for documents
     */
    private List<File> getDocumentDirectories() {
        List<File> directories = new ArrayList<>();
        
        // Add common document directories
        File externalStorage = context.getExternalFilesDir(null);
        if (externalStorage != null) {
            directories.add(externalStorage);
            
            // Add Downloads folder if accessible
            File downloadsDir = new File(externalStorage.getParentFile().getParentFile(), "Download");
            if (downloadsDir.exists()) {
                directories.add(downloadsDir);
            }
            
            // Add Documents folder if accessible
            File documentsDir = new File(externalStorage.getParentFile().getParentFile(), "Documents");
            if (documentsDir.exists()) {
                directories.add(documentsDir);
            }
        }
        
        // Add app-specific converted files directory
        File convertedFilesDir = new File(context.getFilesDir(), "converted");
        if (!convertedFilesDir.exists()) {
            convertedFilesDir.mkdirs();
        }
        directories.add(convertedFilesDir);
        
        return directories;
    }
    
    /**
     * Find all documents in monitored directories
     */
    private List<File> findAllDocuments() {
        List<File> documents = new ArrayList<>();
        List<File> directories = getDocumentDirectories();
        
        for (File directory : directories) {
            if (directory.exists() && directory.canRead()) {
                scanDirectoryRecursively(directory, documents);
            }
        }
        
        return documents;
    }
    
    /**
     * Recursively scan directory for document files
     */
    private void scanDirectoryRecursively(File directory, List<File> documents) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recursively scan subdirectories (limit depth to avoid infinite loops)
                    scanDirectoryRecursively(file, documents);
                } else if (isDocumentFile(file)) {
                    documents.add(file);
                }
            }
        }
    }
    
    /**
     * Check if a file is a document based on its extension
     */
    private boolean isDocumentFile(File file) {
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
    }
}
