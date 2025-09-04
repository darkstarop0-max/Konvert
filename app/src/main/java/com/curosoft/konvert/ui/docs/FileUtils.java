package com.curosoft.konvert.ui.docs;

import android.content.Context;
import android.text.format.Formatter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for file operations and metadata extraction
 * Provides methods for reading files, formatting sizes, dates, and file information
 */
public class FileUtils {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private static final SimpleDateFormat TODAY_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    
    /**
     * Read file content as text with error handling
     */
    public static String readFileAsText(File file) {
        StringBuilder text = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to read file: " + e.getMessage();
        }
        return text.toString();
    }
    
    /**
     * Format file size in human-readable format (KB, MB, GB)
     */
    public static String formatFileSize(Context context, long sizeBytes) {
        if (context != null) {
            return Formatter.formatFileSize(context, sizeBytes);
        }
        
        // Fallback manual formatting
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", sizeBytes / 1024.0);
        } else if (sizeBytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Format last modified date in user-friendly format
     * Shows "Today, 2:30 PM" or "Yesterday, 10:15 AM" or "Jan 15, 2025"
     */
    public static String formatLastModified(long lastModified) {
        Date fileDate = new Date(lastModified);
        Date now = new Date();
        
        long diff = now.getTime() - fileDate.getTime();
        long daysDiff = diff / (24 * 60 * 60 * 1000);
        
        if (daysDiff == 0) {
            // Today
            return "Today, " + TODAY_FORMAT.format(fileDate);
        } else if (daysDiff == 1) {
            // Yesterday
            return "Yesterday, " + TODAY_FORMAT.format(fileDate);
        } else if (daysDiff < 7) {
            // This week
            SimpleDateFormat weekFormat = new SimpleDateFormat("EEEE, h:mm a", Locale.getDefault());
            return weekFormat.format(fileDate);
        } else {
            // Older dates
            return DATE_FORMAT.format(fileDate);
        }
    }
    
    /**
     * Get file extension from filename
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase();
    }
    
    /**
     * Get file type from extension
     */
    public static String getFileType(String fileName) {
        String extension = getFileExtension(fileName);
        switch (extension) {
            case "pdf":
                return "PDF";
            case "docx":
                return "DOCX";
            case "txt":
                return "TXT";
            case "rtf":
                return "RTF";
            case "odt":
                return "ODT";
            default:
                return "DOC";
        }
    }
    
    /**
     * Get appropriate icon resource for file type
     */
    public static int getIconForFileType(String fileName) {
        String extension = getFileExtension(fileName);
        switch (extension) {
            case "pdf":
                return com.curosoft.konvert.R.drawable.ic_file_pdf;
            case "docx":
                return com.curosoft.konvert.R.drawable.ic_file_docx;
            case "txt":
                return com.curosoft.konvert.R.drawable.ic_file_txt;
            case "rtf":
            case "odt":
            default:
                return com.curosoft.konvert.R.drawable.ic_file_general;
        }
    }
    
    /**
     * Check if file is a supported document type
     */
    public static boolean isSupportedDocument(String fileName) {
        String extension = getFileExtension(fileName);
        return extension.equals("pdf") || extension.equals("docx") || 
               extension.equals("txt") || extension.equals("rtf") || 
               extension.equals("odt");
    }
    
    /**
     * Format file path to show relative path from common directories
     */
    public static String formatFilePath(String fullPath) {
        if (fullPath == null) return "";
        
        // Common path replacements for readability
        String result = fullPath;
        
        if (result.contains("/storage/emulated/0/")) {
            result = result.replace("/storage/emulated/0/", "/");
        }
        
        if (result.contains("/Android/data/")) {
            int index = result.indexOf("/Android/data/");
            result = "..." + result.substring(index);
        }
        
        // Limit length
        if (result.length() > 50) {
            result = "..." + result.substring(result.length() - 47);
        }
        
        return result;
    }
}
