package com.curosoft.konvert.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "converted_files")
public class ConvertedFile {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String fileName;
    private String filePath;
    private String conversionType;
    private long timestamp;

    public ConvertedFile(String fileName, String filePath, String conversionType, long timestamp) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.conversionType = conversionType;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getConversionType() {
        return conversionType;
    }

    public void setConversionType(String conversionType) {
        this.conversionType = conversionType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
