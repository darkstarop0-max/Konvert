package com.curosoft.konvert;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.curosoft.konvert.ui.onboarding.OnboardingActivity;
import com.curosoft.konvert.utils.PreferenceManager;
import com.curosoft.konvert.utils.SettingsManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNavigationView;
    private CardView bottomNavigationContainer;
    private Toolbar toolbar;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply dark mode setting before setting content view
        SettingsManager settingsManager = new SettingsManager(this);
        if (settingsManager.isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        // Check if onboarding is completed
        preferenceManager = PreferenceManager.getInstance(this);
        if (preferenceManager.isFirstTimeLaunch()) {
            // First time launch - show onboarding
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        
        // User has seen onboarding, proceed normally
        setContentView(R.layout.activity_main);
        
        // Set up toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Setup Navigation - Get NavController from NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            // Find the bottom navigation view and its container
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationContainer = findViewById(R.id.bottom_navigation_container);
            
            // Define top-level destinations
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
            R.id.dashboardFragment, R.id.docsViewerEditorFragment, R.id.settingsFragment)
            .build();
                    
            // Connect the navController with the toolbar and BottomNavigationView
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
            
            // Handle destination changes to show/hide bottom navigation and update toolbar title
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                if (destinationId == R.id.onboardingFragment) {
                    bottomNavigationContainer.setVisibility(android.view.View.GONE);
                    toolbar.setVisibility(android.view.View.GONE);
                } else {
                    bottomNavigationContainer.setVisibility(android.view.View.VISIBLE);
                    toolbar.setVisibility(android.view.View.VISIBLE);
                    toolbar.setTitle(destination.getLabel());
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }
}