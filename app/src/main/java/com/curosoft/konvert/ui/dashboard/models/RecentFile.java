package com.curosoft.konvert.ui.dashboard.models;

import com.curosoft.konvert.data.database.entities.ConvertedFile;
import com.curosoft.konvert.utils.TimeUtils;

public class RecentFile {
    private int iconRes;
    private String fileName;
    private String convertedFrom;
    private String timeAgo;
    private String filePath;
    private int databaseId;

    public RecentFile(int iconRes, String fileName, String convertedFrom, String timeAgo) {
        this.iconRes = iconRes;
        this.fileName = fileName;
        this.convertedFrom = convertedFrom;
        this.timeAgo = timeAgo;
    }

    // Constructor to create from database entity
    public RecentFile(ConvertedFile convertedFile, int iconRes) {
        this.databaseId = convertedFile.getId();
        this.fileName = convertedFile.getFileName();
        this.filePath = convertedFile.getFilePath();
        this.convertedFrom = TimeUtils.getConversionTypeDisplayName(convertedFile.getConversionType());
        this.timeAgo = TimeUtils.getTimeAgo(convertedFile.getTimestamp());
        this.iconRes = iconRes;
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

    public String getFilePath() {
        return filePath;
    }

    public int getDatabaseId() {
        return databaseId;
    }
}
