package com.curosoft.konvert.utils;

import android.content.Context;

/**
 * Helper class to integrate conversion tracking with the dashboard
 * Use this in your conversion activities to automatically track conversions
 */
public class DashboardIntegrationHelper {
    
    private Context context;
    
    public DashboardIntegrationHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Call this method when a conversion is successfully completed
     * The dashboard will automatically update via LiveData
     * 
     * Example usage in a conversion activity:
     * 
     * // After successful conversion
     * DashboardIntegrationHelper helper = new DashboardIntegrationHelper(this);
     * helper.onConversionCompleted("/path/to/converted/file.pdf", "DOCX", "PDF");
     */
    public void onConversionCompleted(String outputFilePath, String fromFormat, String toFormat) {
        ConversionUtils.trackConversion(context, outputFilePath, fromFormat, toFormat);
    }
    
    /**
     * Call this method with a custom file name
     */
    public void onConversionCompleted(String outputFilePath, String fileName, String fromFormat, String toFormat) {
        ConversionUtils.trackConversion(context, outputFilePath, fileName, fromFormat, toFormat);
    }
}
