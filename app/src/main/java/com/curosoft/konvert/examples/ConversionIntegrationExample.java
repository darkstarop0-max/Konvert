package com.curosoft.konvert.examples;

import android.content.Context;
import android.widget.Toast;

import com.curosoft.konvert.utils.ConversionUtils;

/**
 * Example showing how to integrate conversion tracking
 * Add the tracking call wherever your conversion succeeds
 */
public class ConversionIntegrationExample {

    /**
     * Example: Document conversion activity
     */
    public void handleDocumentConversion(Context context, String inputPath, String outputPath) {
        try {
            // Your existing conversion logic here...
            // performDocumentConversion(inputPath, outputPath);
            
            // ✅ ADD THIS SINGLE LINE after successful conversion:
            ConversionUtils.trackConversion(context, outputPath, "DOCX", "PDF");
            
            // Your existing success handling...
            Toast.makeText(context, "Conversion successful!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            // Your existing error handling...
            Toast.makeText(context, "Conversion failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Example: Image conversion activity  
     */
    public void handleImageConversion(Context context, String inputPath, String outputPath) {
        try {
            // Your existing conversion logic here...
            // performImageConversion(inputPath, outputPath);
            
            // ✅ ADD THIS SINGLE LINE after successful conversion:
            ConversionUtils.trackConversion(context, outputPath, "JPG", "PNG");
            
            // Your existing success handling...
            Toast.makeText(context, "Image converted!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            // Your existing error handling...
            Toast.makeText(context, "Conversion failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Example: Async conversion with callback
     */
    public void handleAsyncConversion(Context context) {
        // Your existing async conversion...
        performAsyncConversion(new ConversionCallback() {
            @Override
            public void onSuccess(String outputPath, String fromFormat, String toFormat) {
                // ✅ ADD THIS SINGLE LINE in your success callback:
                ConversionUtils.trackConversion(context, outputPath, fromFormat, toFormat);
                
                // Your existing success handling...
                Toast.makeText(context, "Conversion complete!", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(String error) {
                // Your existing error handling...
                Toast.makeText(context, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Example: Batch conversion
     */
    public void handleBatchConversion(Context context, String[] inputPaths, String[] outputPaths) {
        for (int i = 0; i < inputPaths.length; i++) {
            try {
                // Your existing conversion logic for each file...
                // performSingleConversion(inputPaths[i], outputPaths[i]);
                
                // ✅ ADD THIS SINGLE LINE for each successful conversion:
                ConversionUtils.trackConversion(context, outputPaths[i], "DOCX", "PDF");
                
            } catch (Exception e) {
                // Handle individual file errors...
            }
        }
        
        // Your existing batch completion handling...
        Toast.makeText(context, "Batch conversion complete!", Toast.LENGTH_SHORT).show();
    }

    // Dummy methods for example
    private void performAsyncConversion(ConversionCallback callback) {
        // Simulate async conversion
    }
    
    private interface ConversionCallback {
        void onSuccess(String outputPath, String fromFormat, String toFormat);
        void onError(String error);
    }
}
