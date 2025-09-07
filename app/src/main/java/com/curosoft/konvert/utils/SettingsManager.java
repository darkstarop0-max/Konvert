package com.curosoft.konvert.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;
import java.io.File;
import java.text.DecimalFormat;

/**
 * Manager class for handling all app settings and preferences
 * Provides a centralized interface for storing and retrieving user preferences
 */
public class SettingsManager {
    
    private static final String PREFS_NAME = "konvert_settings";
    private static final String PREF_SAVE_LOCATION = "save_location";
    private static final String PREF_CUSTOM_LOCATION_DISPLAY = "custom_location_display";
    private static final String PREF_VIEWER_MODE = "viewer_mode";
    private static final String PREF_FONT_SIZE = "font_size";
    private static final String PREF_DARK_MODE = "dark_mode";
    private static final String PREF_THEME_MODE = "theme_mode";
    private static final String PREF_FIRST_LAUNCH = "first_launch";
    private static final String PREF_PRIVACY_ACCEPTED = "privacy_accepted";
    
    // Default values
    private static final boolean DEFAULT_CONTINUOUS_VIEW = false;
    private static final int DEFAULT_FONT_SIZE = 14;
    private static final boolean DEFAULT_DARK_MODE = false;
    private static final int DEFAULT_THEME_MODE = 2; // System default
    
    // Theme mode constants
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;
    
    private final SharedPreferences prefs;
    private final Context context;
    
    public SettingsManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // Storage & File Management Settings
    
    /**
     * Get the current save location for documents
     */
    public String getSaveLocation() {
        String defaultLocation = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/Konvert";
        return prefs.getString(PREF_SAVE_LOCATION, defaultLocation);
    }
    
    /**
     * Set the save location for documents
     */
    public void setSaveLocation(String location) {
        prefs.edit().putString(PREF_SAVE_LOCATION, location).apply();
    }
    
    /**
     * Set the display name for custom save location
     */
    public void setCustomSaveLocationDisplay(String displayName) {
        prefs.edit().putString(PREF_CUSTOM_LOCATION_DISPLAY, displayName).apply();
    }
    
    /**
     * Get the display name for custom save location
     */
    public String getCustomSaveLocationDisplay() {
        return prefs.getString(PREF_CUSTOM_LOCATION_DISPLAY, "Custom Location");
    }
    
    /**
     * Check if current save location is a custom URI
     */
    public boolean isCustomSaveLocation() {
        String location = getSaveLocation();
        return location.startsWith("content://");
    }
    
    /**
     * Get display-friendly save location
     */
    public String getSaveLocationDisplay() {
        if (isCustomSaveLocation()) {
            return getCustomSaveLocationDisplay();
        }
        
        String location = getSaveLocation();
        if (location.contains("Documents")) {
            return "Documents/Konvert";
        } else if (location.contains("Downloads")) {
            return "Downloads";
        } else if (location.contains("Pictures")) {
            return "Pictures/Konvert";
        }
        return location;
    }
    
    /**
     * Get formatted cache size information
     */
    public String getCacheSizeFormatted() {
        File cacheDir = context.getCacheDir();
        long size = calculateFolderSize(cacheDir);
        return formatFileSize(size);
    }
    
    /**
     * Clear application cache
     */
    public void clearCache() {
        File cacheDir = context.getCacheDir();
        deleteRecursive(cacheDir);
    }
    
    /**
     * Get formatted storage information
     */
    public String getStorageInfoFormatted() {
        try {
            File externalDir = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(externalDir.getPath());
            
            long totalBytes = stat.getTotalBytes();
            long freeBytes = stat.getAvailableBytes();
            long usedBytes = totalBytes - freeBytes;
            
            return "Used: " + formatFileSize(usedBytes) + " / " + formatFileSize(totalBytes) + 
                   " (" + formatFileSize(freeBytes) + " free)";
        } catch (Exception e) {
            return "Storage info unavailable";
        }
    }
    
    // Document Viewer Preferences
    
    /**
     * Check if continuous view mode is enabled
     */
    public boolean isContinuousViewMode() {
        return prefs.getBoolean(PREF_VIEWER_MODE, DEFAULT_CONTINUOUS_VIEW);
    }
    
    /**
     * Set continuous view mode
     */
    public void setContinuousViewMode(boolean enabled) {
        prefs.edit().putBoolean(PREF_VIEWER_MODE, enabled).apply();
    }
    
    /**
     * Get the current font size
     */
    public int getFontSize() {
        return prefs.getInt(PREF_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
    
    /**
     * Set the font size
     */
    public void setFontSize(int fontSize) {
        prefs.edit().putInt(PREF_FONT_SIZE, fontSize).apply();
    }
    
    /**
     * Check if dark mode is enabled (based on theme mode)
     */
    public boolean isDarkModeEnabled() {
        int themeMode = getThemeMode();
        return themeMode == THEME_DARK;
    }
    
    /**
     * Set dark mode preference (updates theme mode)
     */
    public void setDarkModeEnabled(boolean enabled) {
        setThemeMode(enabled ? THEME_DARK : THEME_LIGHT);
    }
    
    /**
     * Get current theme mode (0=Light, 1=Dark, 2=System)
     */
    public int getThemeMode() {
        return prefs.getInt(PREF_THEME_MODE, DEFAULT_THEME_MODE);
    }
    
    /**
     * Set theme mode (0=Light, 1=Dark, 2=System)
     */
    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(PREF_THEME_MODE, themeMode).apply();
    }
    
    // Privacy & Permissions
    
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
     * Reset all settings to their default values
     */
    public void resetToDefaults() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
    
    /**
     * Export current settings as a formatted string
     */
    public String exportSettings() {
        StringBuilder sb = new StringBuilder();
        sb.append("Konvert Settings Export\n");
        sb.append("======================\n\n");
        sb.append("Save Location: ").append(getSaveLocation()).append("\n");
        sb.append("Viewer Mode: ").append(isContinuousViewMode() ? "Continuous" : "Page").append("\n");
        sb.append("Font Size: ").append(getFontSize()).append("sp\n");
        sb.append("Dark Mode: ").append(isDarkModeEnabled() ? "Enabled" : "Disabled").append("\n");
        sb.append("Privacy Accepted: ").append(isPrivacyAccepted() ? "Yes" : "No").append("\n");
        
        return sb.toString();
    }
    
    // Private helper methods
    
    private long calculateFolderSize(File folder) {
        long size = 0;
        if (folder != null && folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += calculateFolderSize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
    
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory != null && fileOrDirectory.exists()) {
            if (fileOrDirectory.isDirectory()) {
                File[] files = fileOrDirectory.listFiles();
                if (files != null) {
                    for (File child : files) {
                        deleteRecursive(child);
                    }
                }
            }
            // Don't delete the cache directory itself, just its contents
            if (!fileOrDirectory.equals(context.getCacheDir())) {
                fileOrDirectory.delete();
            }
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        return decimalFormat.format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
