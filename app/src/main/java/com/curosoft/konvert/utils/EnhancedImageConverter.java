package com.curosoft.konvert.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Enhanced image converter that preserves resolution, transparency, and EXIF metadata
 * Optimized for Android without external libraries
 */
public class EnhancedImageConverter {
    private static final String TAG = "EnhancedImageConverter";
    private static final int MAX_MEMORY_USAGE = 50 * 1024 * 1024; // 50MB max memory usage
    
    public static class ConversionResult {
        public final boolean success;
        public final String outputPath;
        public final String errorMessage;
        
        public ConversionResult(boolean success, String outputPath, String errorMessage) {
            this.success = success;
            this.outputPath = outputPath;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * Convert image while preserving original quality, transparency, and EXIF data
     */
    public static ConversionResult convertImage(Context context, Uri inputUri, String targetFormat) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        File outputFile = null;
        
        try {
            // Extract EXIF data before conversion
            ExifInterface originalExif = null;
            try {
                inputStream = context.getContentResolver().openInputStream(inputUri);
                if (inputStream != null) {
                    originalExif = new ExifInterface(inputStream);
                    inputStream.close();
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not read EXIF data", e);
            }
            
            // Analyze image dimensions for memory optimization
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            inputStream = context.getContentResolver().openInputStream(inputUri);
            BitmapFactory.decodeStream(inputStream, null, boundsOptions);
            if (inputStream != null) inputStream.close();
            
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return new ConversionResult(false, null, "Invalid image dimensions");
            }
            
            // Calculate optimal sample size for memory efficiency
            int sampleSize = calculateOptimalSampleSize(boundsOptions);
            
            // Decode with optimal settings
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;
            decodeOptions.inPreferredConfig = getOptimalBitmapConfig(targetFormat);
            decodeOptions.inPremultiplied = true; // Better for transparency
            
            inputStream = context.getContentResolver().openInputStream(inputUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions);
            if (inputStream != null) inputStream.close();
            
            if (bitmap == null) {
                return new ConversionResult(false, null, "Failed to decode image");
            }
            
            // Prepare output file
            String baseName = getFileName(context, inputUri);
            String outputFileName = generateOutputFileName(baseName, targetFormat);
            outputFile = createOutputFile(outputFileName);
            
            if (outputFile == null) {
                bitmap.recycle();
                return new ConversionResult(false, null, "Failed to create output file");
            }
            
            // Convert with format-specific optimizations
            outputStream = new FileOutputStream(outputFile);
            boolean compressionSuccess = compressWithOptimalSettings(bitmap, targetFormat, outputStream);
            bitmap.recycle(); // Free memory immediately
            
            if (!compressionSuccess) {
                if (outputFile.exists()) outputFile.delete();
                return new ConversionResult(false, null, "Compression failed");
            }
            
            // Preserve EXIF data where possible
            if (originalExif != null && supportsExif(targetFormat)) {
                try {
                    copyExifData(originalExif, outputFile.getAbsolutePath());
                } catch (Exception e) {
                    Log.w(TAG, "Could not preserve EXIF data", e);
                }
            }
            
            // Add to media store
            addToMediaStore(context, outputFile, targetFormat);
            
            // Update recent files
            ConversionUtils.trackConversion(context, outputFile.getAbsolutePath(), 
                getImageFormat(context, inputUri), targetFormat.toUpperCase());
            
            return new ConversionResult(true, outputFile.getAbsolutePath(), null);
            
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory during image conversion", e);
            if (outputFile != null && outputFile.exists()) outputFile.delete();
            return new ConversionResult(false, null, "Image too large for device memory");
        } catch (Exception e) {
            Log.e(TAG, "Image conversion failed", e);
            if (outputFile != null && outputFile.exists()) outputFile.delete();
            return new ConversionResult(false, null, "Conversion error: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }
    
    /**
     * Calculate optimal sample size to prevent OutOfMemoryError
     */
    private static int calculateOptimalSampleSize(BitmapFactory.Options options) {
        int width = options.outWidth;
        int height = options.outHeight;
        
        // Calculate memory requirement
        long memoryRequired = (long) width * height * 4; // 4 bytes per pixel for ARGB_8888
        
        int sampleSize = 1;
        while (memoryRequired > MAX_MEMORY_USAGE && sampleSize < 32) {
            sampleSize *= 2;
            memoryRequired /= 4; // Each doubling of sample size reduces memory by 4x
        }
        
        return sampleSize;
    }
    
    /**
     * Get optimal bitmap config for target format
     */
    private static Bitmap.Config getOptimalBitmapConfig(String targetFormat) {
        switch (targetFormat.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return Bitmap.Config.RGB_565; // No alpha channel needed for JPEG
            case "png":
            case "webp":
            default:
                return Bitmap.Config.ARGB_8888; // Preserve transparency
        }
    }
    
    /**
     * Compress with format-specific optimal settings
     */
    private static boolean compressWithOptimalSettings(Bitmap bitmap, String targetFormat, 
                                                     OutputStream outputStream) {
        switch (targetFormat.toLowerCase()) {
            case "png":
                return bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            case "jpg":
            case "jpeg":
                return bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            case "webp":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Use lossless WEBP for better quality on newer devices
                    return bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, outputStream);
                } else {
                    return bitmap.compress(Bitmap.CompressFormat.WEBP, 95, outputStream);
                }
            default:
                return false;
        }
    }
    
    /**
     * Check if format supports EXIF metadata
     */
    private static boolean supportsExif(String format) {
        return format.toLowerCase().equals("jpg") || format.toLowerCase().equals("jpeg");
    }
    
    /**
     * Copy EXIF data from original to converted image
     */
    private static void copyExifData(ExifInterface originalExif, String outputPath) 
            throws IOException {
        ExifInterface newExif = new ExifInterface(outputPath);
        
        // Copy important EXIF tags (excluding API 28+ tags for compatibility)
        String[] tags = {
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_COPYRIGHT
            // Note: TAG_CAMERA_OWNER_NAME requires API 28+, omitted for compatibility
        };
        
        for (String tag : tags) {
            String value = originalExif.getAttribute(tag);
            if (value != null) {
                newExif.setAttribute(tag, value);
            }
        }
        
        newExif.saveAttributes();
    }
    
    /**
     * Generate output file name with proper naming convention
     */
    private static String generateOutputFileName(String baseName, String targetFormat) {
        String nameWithoutExt = baseName.replaceAll("\\.[^.]*$", "");
        return nameWithoutExt + "_converted." + targetFormat.toLowerCase();
    }
    
    /**
     * Create output file in appropriate directory
     */
    private static File createOutputFile(String fileName) {
        try {
            File outputDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "Konvert/Converted/Images");
            
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                Log.e(TAG, "Failed to create output directory");
                return null;
            }
            
            return new File(outputDir, fileName);
        } catch (Exception e) {
            Log.e(TAG, "Error creating output file", e);
            return null;
        }
    }
    
    /**
     * Add converted file to Android Media Store
     */
    private static void addToMediaStore(Context context, File file, String format) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + format.toLowerCase());
            values.put(MediaStore.Images.Media.RELATIVE_PATH, 
                Environment.DIRECTORY_DOCUMENTS + "/Konvert/Converted/Images");
            
            ContentResolver resolver = context.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            
            if (uri != null) {
                Log.d(TAG, "Added to media store: " + file.getName());
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not add to media store", e);
        }
    }
    
    /**
     * Get original image format from URI
     */
    private static String getImageFormat(Context context, Uri uri) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                if (mimeType.contains("jpeg") || mimeType.contains("jpg")) return "JPG";
                if (mimeType.contains("png")) return "PNG";
                if (mimeType.contains("webp")) return "WEBP";
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not determine image format", e);
        }
        return "IMAGE";
    }
    
    /**
     * Extract file name from URI
     */
    private static String getFileName(Context context, Uri uri) {
        try {
            String fileName = UriUtils.getFileName(context, uri);
            return fileName != null ? fileName : "image";
        } catch (Exception e) {
            return "image";
        }
    }
}
