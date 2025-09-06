package com.curosoft.konvert.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import java.io.File;

/**
 * Manager class for handling all app settings and preferences
 * Provides a centralized interface for storing and retrieving user preferences
 */
public class SettingsManager {
    
    private static final String PREFS_NAME = "konvert_settings";
    private static final String PREF_SAVE_PATH = "save_path";
    private static final String PREF_VIEWER_MODE = "viewer_mode";
    private static final String PREF_FONT_SIZE = "font_size";
    private static final String PREF_DARK_MODE = "dark_mode";
    private static final String PREF_FIRST_LAUNCH = "first_launch";
    private static final String PREF_PRIVACY_ACCEPTED = "privacy_accepted";
    
    // Default values
    private static final String DEFAULT_SAVE_PATH = "Documents/Konvert";
    private static final String DEFAULT_VIEWER_MODE = "page"; // "page" or "scroll"
    private static final int DEFAULT_FONT_SIZE = 14;
    private static final boolean DEFAULT_DARK_MODE = false;
    
    private final SharedPreferences prefs;
    private final Context context;
    
    public SettingsManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // Storage & File Management Settings
    
    /**
     * Get the current save path for documents
     */
    public String getSavePath() {
        return prefs.getString(PREF_SAVE_PATH, getDefaultSavePath());
    }
    
    /**
     * Set the save path for documents
     */
    public void setSavePath(String path) {
        prefs.edit().putString(PREF_SAVE_PATH, path).apply();
    }
    
    /**
     * Get default save path
     */
    public String getDefaultSavePath() {
        File documentsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Konvert");
        return documentsDir.getAbsolutePath();
    }
    
    /**
     * Get cache directory size in bytes
     */
    public long getCacheSize() {
        return calculateDirectorySize(context.getCacheDir());
    }
    
    /**
     * Clear app cache
     */
    public boolean clearCache() {
        return deleteDirectory(context.getCacheDir());
    }
    
    /**
     * Get storage summary information
     */
    public StorageInfo getStorageInfo() {
        File externalStorage = Environment.getExternalStorageDirectory();
        long totalSpace = externalStorage.getTotalSpace();
        long freeSpace = externalStorage.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        
        return new StorageInfo(totalSpace, freeSpace, usedSpace, getCacheSize());
    }
    
    // Document Viewer Preferences
    
    /**
     * Get viewer mode (page or scroll)
     */
    public String getViewerMode() {
        return prefs.getString(PREF_VIEWER_MODE, DEFAULT_VIEWER_MODE);
    }
    
    /**
     * Set viewer mode
     */
    public void setViewerMode(String mode) {
        prefs.edit().putString(PREF_VIEWER_MODE, mode).apply();
    }
    
    /**
     * Check if viewer mode is page-based
     */
    public boolean isPageViewerMode() {
        return "page".equals(getViewerMode());
    }
    
    /**
     * Get font size for document viewer
     */
    public int getFontSize() {
        return prefs.getInt(PREF_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
    
    /**
     * Set font size for document viewer
     */
    public void setFontSize(int size) {
        prefs.edit().putInt(PREF_FONT_SIZE, size).apply();
    }
    
    /**
     * Check if dark mode is enabled
     */
    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(PREF_DARK_MODE, DEFAULT_DARK_MODE);
    }
    
    /**
     * Set dark mode preference
     */
    public void setDarkModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_DARK_MODE, enabled).apply();
    }
    
    // Privacy & App State
    
    /**
     * Check if this is the first app launch
     */
    public boolean isFirstLaunch() {
        return prefs.getBoolean(PREF_FIRST_LAUNCH, true);
    }
    
    /**
     * Mark first launch as completed
     */
    public void setFirstLaunchCompleted() {
        prefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply();
    }
    
    /**
     * Check if privacy policy has been accepted
     */
    public boolean isPrivacyAccepted() {
        return prefs.getBoolean(PREF_PRIVACY_ACCEPTED, false);
    }
    
    /**
     * Set privacy policy acceptance
     */
    public void setPrivacyAccepted(boolean accepted) {
        prefs.edit().putBoolean(PREF_PRIVACY_ACCEPTED, accepted).apply();
    }
    
    // Utility Methods
    
    /**
     * Reset all settings to defaults
     */
    public void resetToDefaults() {
        prefs.edit().clear().apply();
    }
    
    /**
     * Export settings as string (for backup/sharing)
     */
    public String exportSettings() {
        StringBuilder sb = new StringBuilder();
        sb.append("Konvert Settings Export\n");
        sb.append("Save Path: ").append(getSavePath()).append("\n");
        sb.append("Viewer Mode: ").append(getViewerMode()).append("\n");
        sb.append("Font Size: ").append(getFontSize()).append("\n");
        sb.append("Dark Mode: ").append(isDarkModeEnabled()).append("\n");
        return sb.toString();
    }
    
    // Helper Methods
    
    private long calculateDirectorySize(File directory) {
        long size = 0;
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += calculateDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
    
    private boolean deleteDirectory(File directory) {
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Storage information container class
     */
    public static class StorageInfo {
        public final long totalSpace;
        public final long freeSpace;
        public final long usedSpace;
        public final long cacheSize;
        
        StorageInfo(long totalSpace, long freeSpace, long usedSpace, long cacheSize) {
            this.totalSpace = totalSpace;
            this.freeSpace = freeSpace;
            this.usedSpace = usedSpace;
            this.cacheSize = cacheSize;
        }
        
        public double getUsedPercentage() {
            return totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;
        }
    }
}
