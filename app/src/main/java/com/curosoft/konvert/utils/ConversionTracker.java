package com.curosoft.konvert.utils;

import android.content.Context;

import com.curosoft.konvert.data.database.entities.ConvertedFile;
import com.curosoft.konvert.data.repository.ConvertedFileRepository;

import java.io.File;

public class ConversionTracker {
    
    private ConvertedFileRepository repository;
    
    public ConversionTracker(Context context) {
        repository = new ConvertedFileRepository(context);
    }
    
    /**
     * Track a successful conversion
     * @param outputFilePath Path to the converted file
     * @param fromFormat Source format (e.g., "DOCX")
     * @param toFormat Target format (e.g., "PDF")
     */
    public void trackConversion(String outputFilePath, String fromFormat, String toFormat) {
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
        
        repository.insertConvertedFile(convertedFile);
    }
    
    /**
     * Track a conversion with a custom file name
     */
    public void trackConversion(String outputFilePath, String fileName, String fromFormat, String toFormat) {
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
        
        repository.insertConvertedFile(convertedFile);
    }
}
