package com.curosoft.konvert.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "KonvertPrefs";
    private static final String KEY_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Context context;
    private static PreferenceManager instance;

    private PreferenceManager(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context);
        }
        return instance;
    }

    /**
     * Set the first time launch to false after onboarding is completed
     */
    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(KEY_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    /**
     * Check if it's the first time launch - default is true
     */
    public boolean isFirstTimeLaunch() {
        return preferences.getBoolean(KEY_FIRST_TIME_LAUNCH, true);
    }
}
