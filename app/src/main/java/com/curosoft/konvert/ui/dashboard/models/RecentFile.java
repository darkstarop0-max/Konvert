package com.curosoft.konvert.ui.dashboard.models;

public class RecentFile {
    private int iconRes;
    private String fileName;
    private String convertedFrom;
    private String timeAgo;

    public RecentFile(int iconRes, String fileName, String convertedFrom, String timeAgo) {
        this.iconRes = iconRes;
        this.fileName = fileName;
        this.convertedFrom = convertedFrom;
        this.timeAgo = timeAgo;
    }

    public int getIconRes() {
        return iconRes;
    }

    public String getFileName() {
        return fileName;
    }

    public String getConvertedFrom() {
        return convertedFrom;
    }

    public String getTimeAgo() {
        return timeAgo;
    }
}
