package com.curosoft.konvert.ui.docs;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Scanner for document files with optimized performance and sorting capabilities
 * Supports multiple document types and provides various sorting options
 */
public class DocsScanner {
    
    // Supported document extensions
    private static final String[] SUPPORTED_EXTENSIONS = {".pdf", ".docx", ".txt", ".rtf", ".odt"};
    
    // Common document directories to prioritize scanning
    private static final String[] PRIORITY_DIRS = {
        "Documents", "Download", "Downloads", "DCIM", "Pictures", "Music", "Movies"
    };
    
    // Sorting options
    public enum SortBy {
        NAME_ASC, NAME_DESC, SIZE_ASC, SIZE_DESC, DATE_ASC, DATE_DESC
    }
    
    /**
     * Interface for document scanning callbacks
     */
    public interface DocumentScanListener {
        void onScanStarted();
        void onDocumentsFound(List<File> documents);
        void onScanProgress(int current, int total);
        void onScanComplete();
        void onScanError(String error);
    }
    
    /**
     * Scan for documents synchronously (use with caution on main thread)
     */
    public static List<File> scanForDocuments(Context context) {
        List<File> docs = new ArrayList<>();
        
        // Scan external storage
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File externalStorage = Environment.getExternalStorageDirectory();
            if (externalStorage != null && externalStorage.exists()) {
                scanDirectory(externalStorage, docs, 0, 3); // Limit depth to prevent long scans
            }
        }
        
        // Scan internal app-specific directories
        File[] externalDirs = context.getExternalFilesDirs(null);
        if (externalDirs != null) {
            for (File dir : externalDirs) {
                if (dir != null && dir.exists()) {
                    scanDirectory(dir, docs, 0, 2);
                }
            }
        }
        
        return docs;
    }
    
    /**
     * Scan for documents asynchronously with progress updates
     */
    public static void scanForDocumentsAsync(Context context, DocumentScanListener listener) {
        new DocumentScanTask(context, listener).execute();
    }
    
    /**
     * Scan a specific directory recursively with depth limiting
     */
    private static void scanDirectory(File dir, List<File> docs, int currentDepth, int maxDepth) {
        if (dir == null || !dir.isDirectory() || !dir.canRead() || currentDepth > maxDepth) {
            return;
        }
        
        try {
            File[] files = dir.listFiles();
            if (files == null) return;
            
            // Sort files to process directories first, then files
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });
            
            for (File file : files) {
                if (file.isDirectory()) {
                    // Skip hidden directories and system directories
                    String dirName = file.getName();
                    if (!dirName.startsWith(".") && !dirName.startsWith("Android")) {
                        scanDirectory(file, docs, currentDepth + 1, maxDepth);
                    }
                } else if (file.canRead()) {
                    String fileName = file.getName().toLowerCase();
                    for (String ext : SUPPORTED_EXTENSIONS) {
                        if (fileName.endsWith(ext)) {
                            docs.add(file);
                            break;
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            // Skip directories we can't access
        }
    }
    
    /**
     * Sort documents by specified criteria
     */
    public static List<File> sortDocuments(List<File> documents, SortBy sortBy) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }
        
        List<File> sorted = new ArrayList<>(documents);
        
        switch (sortBy) {
            case NAME_ASC:
                Collections.sort(sorted, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                break;
            case NAME_DESC:
                Collections.sort(sorted, (f1, f2) -> f2.getName().compareToIgnoreCase(f1.getName()));
                break;
            case SIZE_ASC:
                Collections.sort(sorted, Comparator.comparingLong(File::length));
                break;
            case SIZE_DESC:
                Collections.sort(sorted, (f1, f2) -> Long.compare(f2.length(), f1.length()));
                break;
            case DATE_ASC:
                Collections.sort(sorted, Comparator.comparingLong(File::lastModified));
                break;
            case DATE_DESC:
                Collections.sort(sorted, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                break;
        }
        
        return sorted;
    }
    
    /**
     * Filter documents by search query
     */
    public static List<File> filterDocuments(List<File> documents, String query) {
        if (query == null || query.trim().isEmpty()) {
            return documents;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        List<File> filtered = new ArrayList<>();
        
        for (File file : documents) {
            if (file.getName().toLowerCase().contains(lowerQuery)) {
                filtered.add(file);
            }
        }
        
        return filtered;
    }
    
    /**
     * Get readable name for sort type
     */
    public static String getSortDisplayName(SortBy sortBy) {
        switch (sortBy) {
            case NAME_ASC: return "Name ↑";
            case NAME_DESC: return "Name ↓";
            case SIZE_ASC: return "Size ↑";
            case SIZE_DESC: return "Size ↓";
            case DATE_ASC: return "Date ↑";
            case DATE_DESC: return "Date ↓";
            default: return "Name";
        }
    }
    
    /**
     * AsyncTask for background document scanning
     */
    private static class DocumentScanTask extends AsyncTask<Void, Integer, List<File>> {
        private final Context context;
        private final DocumentScanListener listener;
        private String errorMessage;
        
        DocumentScanTask(Context context, DocumentScanListener listener) {
            this.context = context;
            this.listener = listener;
        }
        
        @Override
        protected void onPreExecute() {
            if (listener != null) {
                listener.onScanStarted();
            }
        }
        
        @Override
        protected List<File> doInBackground(Void... voids) {
            try {
                return scanForDocuments(context);
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return new ArrayList<>();
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (listener != null && values.length >= 2) {
                listener.onScanProgress(values[0], values[1]);
            }
        }
        
        @Override
        protected void onPostExecute(List<File> documents) {
            if (listener != null) {
                if (errorMessage != null) {
                    listener.onScanError(errorMessage);
                } else {
                    listener.onDocumentsFound(documents);
                    listener.onScanComplete();
                }
            }
        }
    }
}
