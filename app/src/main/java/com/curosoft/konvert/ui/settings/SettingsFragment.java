package com.curosoft.konvert.ui.settings;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.curosoft.konvert.R;
import com.curosoft.konvert.utils.CustomDialogUtils;
import com.curosoft.konvert.utils.SettingsManager;

public class SettingsFragment extends Fragment {

    private SettingsManager settingsManager;
    
    // UI Components
    private SwitchCompat darkModeSwitch;
    private LinearLayout saveLocationSetting;
    private LinearLayout permissionsSetting;
    private LinearLayout rateAppSetting;
    private LinearLayout shareAppSetting;
    private LinearLayout privacyPolicySetting;
    
    private TextView appVersionText;
    private TextView currentLocationText;

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
    }

    private void initializeViews(View view) {
        // Find all UI components
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        saveLocationSetting = view.findViewById(R.id.saveLocationSetting);
        permissionsSetting = view.findViewById(R.id.permissionsSetting);
        rateAppSetting = view.findViewById(R.id.rateAppSetting);
        shareAppSetting = view.findViewById(R.id.shareAppSetting);
        privacyPolicySetting = view.findViewById(R.id.privacyPolicySetting);
        
        appVersionText = view.findViewById(R.id.appVersionText);
        currentLocationText = view.findViewById(R.id.currentLocationText);
    }

    private void setupClickListeners() {
        // App Preferences
        saveLocationSetting.setOnClickListener(v -> showSaveLocationDialog());
        permissionsSetting.setOnClickListener(v -> openAppPermissions());
        
        // App Info & Utilities
        rateAppSetting.setOnClickListener(v -> rateApp());
        shareAppSetting.setOnClickListener(v -> shareApp());
        privacyPolicySetting.setOnClickListener(v -> openPrivacyPolicy());
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
        
        // Update dark mode switch
        updateDarkModeSwitch();
        
        // Update save location display
        updateSaveLocationDisplay();
    }

    private void updateDarkModeSwitch() {
        int currentTheme = settingsManager.getThemeMode();
        // Temporarily disable listener to prevent feedback loop
        darkModeSwitch.setOnCheckedChangeListener(null);
        // Set switch to checked if dark mode is enabled
        darkModeSwitch.setChecked(currentTheme == SettingsManager.THEME_DARK);
        // Re-enable listener
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the dark mode preference
            settingsManager.setThemeMode(isChecked ? SettingsManager.THEME_DARK : SettingsManager.THEME_LIGHT);
            applyTheme(isChecked);
            
            String message = isChecked ? "Dark mode enabled" : "Light mode enabled";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSaveLocationDisplay() {
        String location = settingsManager.getSaveLocation();
        // Show just the folder name for cleaner display
        String displayLocation = location.substring(location.lastIndexOf('/') + 1);
        if (displayLocation.isEmpty()) {
            displayLocation = "Documents/Konvert";
        }
        currentLocationText.setText(displayLocation);
    }

    private void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void showSaveLocationDialog() {
        String currentLocation = settingsManager.getSaveLocation();
        String message = "Current location:\n" + currentLocation + 
                        "\n\nChoose where to save converted files:";
        
        String[] options = {
            "Documents/Konvert", 
            "Downloads", 
            "Pictures", 
            "Custom Location"
        };
        
        CustomDialogUtils.showSingleChoiceDialog(
            requireContext(),
            "Save Location",
            message,
            options,
            0, // Default selection
            (dialog, selectedIndex) -> {
                String newLocation;
                switch (selectedIndex) {
                    case 0:
                        newLocation = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/Konvert";
                        break;
                    case 1:
                        newLocation = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                        break;
                    case 2:
                        newLocation = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Konvert";
                        break;
                    case 3:
                        // Show custom location picker dialog
                        showCustomLocationPicker();
                        return;
                    default:
                        return;
                }
                
                settingsManager.setSaveLocation(newLocation);
                updateSaveLocationDisplay();
                Toast.makeText(getContext(), "Save location updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        );
    }

    private void showCustomLocationPicker() {
        String message = "Custom folder picker will allow you to choose any folder on your device.\n\n" +
                        "This feature is coming in the next update!";
        
        CustomDialogUtils.showInfoDialog(
            requireContext(),
            "Custom Location",
            message,
            "Got it",
            null
        );
    }

    private void openAppPermissions() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void openPrivacyPolicy() {
        String message = "🔒 Your Privacy Matters\n\n" +
                        "Konvert is designed with privacy in mind:\n\n" +
                        "• All document processing happens locally on your device\n" +
                        "• No personal data is collected or transmitted\n" +
                        "• Storage access is only used for saving converted files\n" +
                        "• No ads, no tracking, no data mining\n\n" +
                        "Your documents stay private and secure.";
        
        CustomDialogUtils.showInfoDialog(
            requireContext(),
            "Privacy Policy",
            message,
            "Learn More",
            () -> {
                // In a real app, this would open a web page or PDF
                Toast.makeText(getContext(), "Would open full privacy policy", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void rateApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + requireContext().getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to web version
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + 
                                       requireContext().getPackageName()));
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(getContext(), "Unable to open Play Store", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shareApp() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Check out Konvert!");
        intent.putExtra(Intent.EXTRA_TEXT, 
            "Transform your documents with ease! 📄✨\n\n" +
            "Konvert makes document conversion simple and fast. " +
            "Convert images to PDFs, merge documents, and more!\n\n" +
            "Download: https://play.google.com/store/apps/details?id=" + 
            requireContext().getPackageName());
        
        startActivity(Intent.createChooser(intent, "Share Konvert"));
    }
}
