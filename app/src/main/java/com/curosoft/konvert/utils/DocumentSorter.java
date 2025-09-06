package com.curosoft.konvert.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class for sorting documents by various criteria
 */
public class DocumentSorter {
    
    public enum SortBy {
        NAME_ASC("Name A-Z"),
        NAME_DESC("Name Z-A"),
        SIZE_ASC("Size (Small to Large)"),
        SIZE_DESC("Size (Large to Small)"),
        TYPE_ASC("Type A-Z"),
        TYPE_DESC("Type Z-A"),
        DATE_ASC("Date (Oldest First)"),
        DATE_DESC("Date (Newest First)");
        
        private final String displayName;
        
        SortBy(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Sort documents by the specified criteria
     */
    public static List<File> sortDocuments(List<File> documents, SortBy sortBy) {
        List<File> sortedList = new ArrayList<>(documents);
        
        Comparator<File> comparator;
        switch (sortBy) {
            case NAME_ASC:
                comparator = (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName());
                break;
            case NAME_DESC:
                comparator = (f1, f2) -> f2.getName().compareToIgnoreCase(f1.getName());
                break;
            case SIZE_ASC:
                comparator = Comparator.comparingLong(File::length);
                break;
            case SIZE_DESC:
                comparator = (f1, f2) -> Long.compare(f2.length(), f1.length());
                break;
            case TYPE_ASC:
                comparator = (f1, f2) -> getFileExtension(f1).compareToIgnoreCase(getFileExtension(f2));
                break;
            case TYPE_DESC:
                comparator = (f1, f2) -> getFileExtension(f2).compareToIgnoreCase(getFileExtension(f1));
                break;
            case DATE_ASC:
                comparator = Comparator.comparingLong(File::lastModified);
                break;
            case DATE_DESC:
                comparator = (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified());
                break;
            default:
                comparator = (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName());
                break;
        }
        
        Collections.sort(sortedList, comparator);
        return sortedList;
    }
    
    /**
     * Get file extension without the dot
     */
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
}
