package com.curosoft.konvert.ui.dashboard.models;

public class QuickAction {
    private int iconRes;
    private String title;
    private String category;

    public QuickAction(int iconRes, String title, String category) {
        this.iconRes = iconRes;
        this.title = title;
        this.category = category;
    }

    public int getIconRes() {
        return iconRes;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }
}
