package com.curosoft.konvert;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.curosoft.konvert.ui.docs.DocsViewerEditorFragment;
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
    private boolean isDocumentsScreenActive = false;

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
            
            // Add smooth animations to bottom navigation items
            setupBottomNavigationAnimations();
            
            // Handle destination changes to show/hide bottom navigation and update toolbar title
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                if (destinationId == R.id.onboardingFragment) {
                    bottomNavigationContainer.setVisibility(android.view.View.GONE);
                    toolbar.setVisibility(android.view.View.GONE);
                } else {
                    bottomNavigationContainer.setVisibility(android.view.View.VISIBLE);
                    toolbar.setVisibility(android.view.View.VISIBLE);
                    
                    // Set custom title styling for Documents fragment
                    if (destinationId == R.id.docsViewerEditorFragment) {
                        toolbar.setTitle("Documents");
                        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary, null));
                        isDocumentsScreenActive = true;
                    } else {
                        toolbar.setTitle(destination.getLabel());
                        toolbar.setTitleTextColor(getResources().getColor(R.color.clean_text_primary, null));
                        isDocumentsScreenActive = false;
                    }
                    // Invalidate options menu to show/hide sort button
                    invalidateOptionsMenu();
                }
            });
        }
    }
    
    private void setupBottomNavigationAnimations() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Animate the selected item
            View selectedView = bottomNavigationView.findViewById(item.getItemId());
            if (selectedView != null) {
                AnimatorSet scaleUp = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.nav_item_scale_up);
                scaleUp.setTarget(selectedView);
                scaleUp.start();
                
                // Scale back down after a short delay
                selectedView.postDelayed(() -> {
                    AnimatorSet scaleDown = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.nav_item_scale_down);
                    scaleDown.setTarget(selectedView);
                    scaleDown.start();
                }, 100);
            }
            
            // Let NavigationUI handle the actual navigation
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isDocumentsScreenActive) {
            getMenuInflater().inflate(R.menu.documents_menu, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sort) {
            // Get the current fragment and call sort method
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
                if (currentFragment instanceof DocsViewerEditorFragment) {
                    ((DocsViewerEditorFragment) currentFragment).showSortMenu();
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}