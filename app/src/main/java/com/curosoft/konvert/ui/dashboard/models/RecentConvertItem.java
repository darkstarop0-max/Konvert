package com.curosoft.konvert.ui.dashboard.models;

/**
 * Model class for a recent conversion in the dashboard
 */
public class RecentConvertItem {
    private final int iconResId;
    private final String fileName;
    private final String conversionDetails;
    private final String timestamp;

    public RecentConvertItem(int iconResId, String fileName, String conversionDetails, String timestamp) {
        this.iconResId = iconResId;
        this.fileName = fileName;
        this.conversionDetails = conversionDetails;
        this.timestamp = timestamp;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getConversionDetails() {
        return conversionDetails;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
