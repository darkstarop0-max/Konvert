package com.curosoft.konvert.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {
    
    public static String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        // Less than a minute
        if (diff < 60 * 1000) {
            return "Just now";
        }
        
        // Less than an hour
        if (diff < 60 * 60 * 1000) {
            long minutes = diff / (60 * 1000);
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }
        
        // Less than a day
        if (diff < 24 * 60 * 60 * 1000) {
            long hours = diff / (60 * 60 * 1000);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }
        
        // Less than a week
        if (diff < 7 * 24 * 60 * 60 * 1000) {
            long days = diff / (24 * 60 * 60 * 1000);
            return days + (days == 1 ? " day ago" : " days ago");
        }
        
        // More than a week - show date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public static String getConversionTypeDisplayName(String conversionType) {
        if (conversionType == null) return "Unknown";
        
        // Convert from "PDF_TO_DOCX" to "PDF → DOCX"
        String[] parts = conversionType.split("_TO_");
        if (parts.length == 2) {
            return parts[0] + " → " + parts[1];
        }
        
        return conversionType;
    }
}
