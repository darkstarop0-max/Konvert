package com.curosoft.konvert.ui.dashboard.models;

/**
 * Model class for a conversion category in the dashboard
 */
public class ConvertItem {
    private final int iconResId;
    private final String title;
    private final String type;

    public ConvertItem(int iconResId, String title, String type) {
        this.iconResId = iconResId;
        this.title = title;
        this.type = type;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }
}
