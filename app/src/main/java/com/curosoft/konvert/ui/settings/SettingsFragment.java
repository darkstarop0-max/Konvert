package com.curosoft.konvert.ui.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.curosoft.konvert.R;
import com.curosoft.konvert.utils.SettingsManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private SettingsManager settingsManager;
    
    // UI Components
    private TextView appVersionText;
    private TextView saveLocationPath;
    private TextView cacheSize;
    private TextView storageInfo;
    private TextView fontSizeStatus;
    private Switch darkModeSwitch;
    
    // Settings Containers
    private LinearLayout saveLocationSetting;
    private LinearLayout clearCacheSetting;
    private LinearLayout storageSummarySetting;
    private LinearLayout fontSizeSetting;
    private LinearLayout darkModeSetting;
    private LinearLayout managePermissionsSetting;
    private LinearLayout privacyPolicySetting;
    private LinearLayout rateAppSetting;
    private LinearLayout shareAppSetting;
    private TextView resetSettingsButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManager = new SettingsManager(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupClickListeners();
        loadCurrentSettings();
        updateDynamicInfo();
    }

    private void initializeViews(View view) {
        // Text views
        appVersionText = view.findViewById(R.id.appVersionText);
        saveLocationPath = view.findViewById(R.id.saveLocationPath);
        cacheSize = view.findViewById(R.id.cacheSize);
        storageInfo = view.findViewById(R.id.storageInfo);
        fontSizeStatus = view.findViewById(R.id.fontSizeStatus);
        
        // Switches
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        
        // Settings containers
        saveLocationSetting = view.findViewById(R.id.saveLocationSetting);
        clearCacheSetting = view.findViewById(R.id.clearCacheSetting);
        storageSummarySetting = view.findViewById(R.id.storageSummarySetting);
        fontSizeSetting = view.findViewById(R.id.fontSizeSetting);
        darkModeSetting = view.findViewById(R.id.darkModeSetting);
        managePermissionsSetting = view.findViewById(R.id.managePermissionsSetting);
        privacyPolicySetting = view.findViewById(R.id.privacyPolicySetting);
        rateAppSetting = view.findViewById(R.id.rateAppSetting);
        shareAppSetting = view.findViewById(R.id.shareAppSetting);
        resetSettingsButton = view.findViewById(R.id.resetSettingsButton);
    }

    private void setupClickListeners() {
        // Storage & File Management
        saveLocationSetting.setOnClickListener(v -> showSaveLocationDialog());
        clearCacheSetting.setOnClickListener(v -> showClearCacheDialog());
        storageSummarySetting.setOnClickListener(v -> showStorageDetailsDialog());
        
        // Document Viewer Preferences
        fontSizeSetting.setOnClickListener(v -> showFontSizeDialog());
        
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setDarkModeEnabled(isChecked);
            applyDarkMode(isChecked);
            Toast.makeText(getContext(), "Dark mode " + (isChecked ? "enabled" : "disabled") + 
                          ". Restart app to see full effect.", Toast.LENGTH_LONG).show();
        });
        
        // Privacy & Permissions
        managePermissionsSetting.setOnClickListener(v -> openAppSettings());
        privacyPolicySetting.setOnClickListener(v -> openPrivacyPolicy());
        
        // About & Support
        rateAppSetting.setOnClickListener(v -> rateApp());
        shareAppSetting.setOnClickListener(v -> shareApp());
        
        // Reset Settings
        resetSettingsButton.setOnClickListener(v -> showResetSettingsDialog());
    }

    private void loadCurrentSettings() {
        // Load app version
        try {
            PackageInfo pInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            appVersionText.setText("Version " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            appVersionText.setText("Version Unknown");
        }
        
        // Load save location
        saveLocationPath.setText(settingsManager.getSaveLocation());
        
        // Load viewer preferences
        updateFontSizeStatus();
        
        boolean isDarkModeEnabled = settingsManager.isDarkModeEnabled();
        darkModeSwitch.setChecked(isDarkModeEnabled);
        
        // Apply current dark mode setting
        applyDarkMode(isDarkModeEnabled);
    }

    private void updateDynamicInfo() {
        // Update cache size
        updateCacheSize();
        
        // Update storage info
        updateStorageInfo();
    }

    private void updateCacheSize() {
        String cacheInfo = settingsManager.getCacheSizeFormatted();
        cacheSize.setText(cacheInfo);
    }

    private void updateStorageInfo() {
        String storage = settingsManager.getStorageInfoFormatted();
        storageInfo.setText(storage);
    }

    private void applyDarkMode(boolean enabled) {
        if (enabled) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void updateFontSizeStatus() {
        int fontSize = settingsManager.getFontSize();
        String sizeLabel;
        if (fontSize <= 12) {
            sizeLabel = "Small";
        } else if (fontSize <= 16) {
            sizeLabel = "Medium";
        } else if (fontSize <= 20) {
            sizeLabel = "Large";
        } else {
            sizeLabel = "Extra Large";
        }
        fontSizeStatus.setText(fontSize + "sp (" + sizeLabel + ")");
    }

    private void showSaveLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Save Location");
        builder.setMessage("Current location: " + settingsManager.getSaveLocation() + 
                          "\n\nWould you like to use the default Documents folder or select a custom location?");
        
        builder.setPositiveButton("Default", (dialog, which) -> {
            String defaultLocation = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/Konvert";
            settingsManager.setSaveLocation(defaultLocation);
            saveLocationPath.setText(defaultLocation);
            Toast.makeText(getContext(), "Save location updated", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Custom", (dialog, which) -> {
            Toast.makeText(getContext(), "File picker not implemented in this demo", 
                          Toast.LENGTH_LONG).show();
        });
        
        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void showClearCacheDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Clear Cache");
        builder.setMessage("This will delete all cached files and temporary data. " +
                          "Current cache size: " + settingsManager.getCacheSizeFormatted() + 
                          "\n\nAre you sure you want to continue?");
        
        builder.setPositiveButton("Clear", (dialog, which) -> {
            settingsManager.clearCache();
            updateCacheSize();
            Toast.makeText(getContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showStorageDetailsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Storage Details");
        
        String message = "Save Location: " + settingsManager.getSaveLocation() + 
                        "\n\nStorage Information:\n" + settingsManager.getStorageInfoFormatted() +
                        "\n\nCache Size: " + settingsManager.getCacheSizeFormatted() +
                        "\n\nLast Updated: " + new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(new Date());
        
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showFontSizeDialog() {
        String[] fontSizes = {"10sp (Very Small)", "12sp (Small)", "14sp (Medium)", 
                             "16sp (Large)", "18sp (Very Large)", "20sp (Extra Large)"};
        int[] fontValues = {10, 12, 14, 16, 18, 20};
        
        int currentFontSize = settingsManager.getFontSize();
        int selectedIndex = 2; // Default to medium
        for (int i = 0; i < fontValues.length; i++) {
            if (fontValues[i] == currentFontSize) {
                selectedIndex = i;
                break;
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Font Size");
        builder.setSingleChoiceItems(fontSizes, selectedIndex, (dialog, which) -> {
            settingsManager.setFontSize(fontValues[which]);
            updateFontSizeStatus();
            dialog.dismiss();
            Toast.makeText(getContext(), "Font size updated", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void openPrivacyPolicy() {
        // In a real app, you would open your privacy policy URL
        Toast.makeText(getContext(), "Privacy Policy - Would open URL in browser", 
                      Toast.LENGTH_LONG).show();
    }

    private void rateApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + requireContext().getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Google Play Store not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareApp() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Check out Konvert!");
        intent.putExtra(Intent.EXTRA_TEXT, "Hey! I found this amazing document conversion app called Konvert. " +
                       "You should check it out: https://play.google.com/store/apps/details?id=" + 
                       requireContext().getPackageName());
        startActivity(Intent.createChooser(intent, "Share Konvert"));
    }

    private void showResetSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Reset Settings");
        builder.setMessage("This will reset all settings to their default values. " +
                          "Your documents and cache will not be affected.\n\nAre you sure?");
        
        builder.setPositiveButton("Reset", (dialog, which) -> {
            settingsManager.resetToDefaults();
            loadCurrentSettings();
            updateDynamicInfo();
            Toast.makeText(getContext(), "Settings reset to defaults", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh dynamic information when returning to the fragment
        updateDynamicInfo();
    }
}
