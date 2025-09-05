package com.curosoft.konvert.utils;

import android.content.Context;

import com.curosoft.konvert.data.database.entities.ConvertedFile;
import com.curosoft.konvert.data.repository.ConvertedFileRepository;

import java.io.File;

/**
 * Simple utility to track conversions from anywhere in your app.
 * Call ConversionUtils.trackConversion() after a successful conversion.
 */
public class ConversionUtils {
    
    /**
     * Track a successful conversion - call this after conversion completes
     * 
     * @param context Application context
     * @param outputFilePath Path to the converted output file
     * @param fromFormat Source format (e.g., "DOCX", "JPG")
     * @param toFormat Target format (e.g., "PDF", "PNG")
     */
    public static void trackConversion(Context context, String outputFilePath, String fromFormat, String toFormat) {
        if (outputFilePath == null || outputFilePath.isEmpty()) {
            return;
        }
        
        File file = new File(outputFilePath);
        if (!file.exists()) {
            return;
        }
        
        String fileName = file.getName();
        String conversionType = fromFormat.toUpperCase() + "_TO_" + toFormat.toUpperCase();
        long timestamp = System.currentTimeMillis();
        
        ConvertedFile convertedFile = new ConvertedFile(
            fileName,
            outputFilePath,
            conversionType,
            timestamp
        );
        
        ConvertedFileRepository repository = new ConvertedFileRepository(context);
        repository.insertConvertedFile(convertedFile);
    }
    
    /**
     * Track a conversion with custom file name
     */
    public static void trackConversion(Context context, String outputFilePath, String fileName, String fromFormat, String toFormat) {
        if (outputFilePath == null || outputFilePath.isEmpty()) {
            return;
        }
        
        String conversionType = fromFormat.toUpperCase() + "_TO_" + toFormat.toUpperCase();
        long timestamp = System.currentTimeMillis();
        
        ConvertedFile convertedFile = new ConvertedFile(
            fileName,
            outputFilePath,
            conversionType,
            timestamp
        );
        
        ConvertedFileRepository repository = new ConvertedFileRepository(context);
        repository.insertConvertedFile(convertedFile);
    }
}
