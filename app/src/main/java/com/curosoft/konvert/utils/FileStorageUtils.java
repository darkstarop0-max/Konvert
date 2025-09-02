package com.curosoft.konvert.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Utility class for managing file storage locations across the app
 */
public class FileStorageUtils {
    private static final String TAG = "FileStorageUtils";
    
    // Constants for file paths
    private static final String APP_FOLDER_NAME = "Konvert";
    private static final String CONVERTED_FOLDER_NAME = "Converted";
    private static final String PATH_SEPARATOR = "/";
    
    /**
     * Get the appropriate output directory for saving converted files.
     * This method tries to use the public top-level directory first,
     * and falls back to app-specific storage if needed.
     *
     * @param context Application context
     * @return The output directory File object
     */
    public static File getOutputDirectory(Context context) {
        File outputDir;
        
        // Try to use the top-level public directory first (preferred location)
        if (isExternalStorageWritable()) {
            outputDir = new File(Environment.getExternalStorageDirectory(), 
                                APP_FOLDER_NAME + PATH_SEPARATOR + CONVERTED_FOLDER_NAME);
            
            // Try to create the directory if it doesn't exist
            if (!outputDir.exists()) {
                boolean dirCreated = outputDir.mkdirs();
                Log.d(TAG, "Created top-level output directory: " + dirCreated + " at " + outputDir.getAbsolutePath());
                
                // If directory creation fails or we can't write to it, fall back to app-specific storage
                if (!dirCreated || !outputDir.canWrite()) {
                    Log.w(TAG, "Cannot use top-level directory, falling back to app-specific storage");
                    outputDir = getAppSpecificOutputDirectory(context);
                }
            } else if (!outputDir.canWrite()) {
                // Directory exists but we can't write to it (likely due to Scoped Storage restrictions)
                Log.w(TAG, "Cannot write to top-level directory, falling back to app-specific storage");
                outputDir = getAppSpecificOutputDirectory(context);
            }
        } else {
            // External storage is not writable, use app-specific storage
            Log.w(TAG, "External storage not writable, using app-specific storage");
            outputDir = getAppSpecificOutputDirectory(context);
        }
        
        return outputDir;
    }
    
    /**
     * Get the app-specific output directory (fallback)
     *
     * @param context Application context
     * @return The app-specific output directory File object
     */
    private static File getAppSpecificOutputDirectory(Context context) {
        File outputDir = new File(context.getExternalFilesDir(null), 
                               APP_FOLDER_NAME + PATH_SEPARATOR + CONVERTED_FOLDER_NAME);
        
        if (!outputDir.exists()) {
            boolean dirCreated = outputDir.mkdirs();
            Log.d(TAG, "Created app-specific output directory: " + dirCreated + " at " + outputDir.getAbsolutePath());
        }
        
        return outputDir;
    }
    
    /**
     * Checks if external storage is available for read and write
     */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    
    /**
     * Checks if we're running on Android 10+ with Scoped Storage
     */
    public static boolean isScopedStorage() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
}
