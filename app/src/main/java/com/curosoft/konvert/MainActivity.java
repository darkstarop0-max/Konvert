package com.curosoft.konvert;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.curosoft.konvert.utils.PreferenceManager;
import com.curosoft.konvert.utils.SettingsManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "KonvertPrefs";
    private static final String KEY_SEEN_WELCOME = "seen_welcome";
    
    private NavController navController;
    private BottomNavigationView bottomNavigationView;
    private CardView bottomNavigationContainer;
    private Toolbar toolbar;
    private PreferenceManager preferenceManager;
    private boolean isDocumentsScreenActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if welcome screen should be shown
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_SEEN_WELCOME, false)) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        // Apply theme setting before setting content view
        SettingsManager settingsManager = new SettingsManager(this);
        int themeMode = settingsManager.getThemeMode();
        switch (themeMode) {
            case SettingsManager.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SettingsManager.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SettingsManager.THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        
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
            
            // Connect the nav controller to the toolbar
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            
            // Connect the nav controller to the bottom navigation
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
            
            // Add smooth animations to bottom navigation items
            setupBottomNavigationAnimations();
            
            // Handle destination changes to show/hide bottom navigation and update toolbar title
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                // Bottom navigation and toolbar are always visible in main app
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
                // Invalidate options menu to show/hide documents menu
                invalidateOptionsMenu();
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
            }
            
            // Navigate to the selected destination
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
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_open_document) {
            // Handle open document action - delegate to the fragment
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (currentFragment instanceof NavHostFragment) {
                Fragment childFragment = ((NavHostFragment) currentFragment).getChildFragmentManager().getFragments().get(0);
                if (childFragment instanceof DocsViewerEditorFragment) {
                    ((DocsViewerEditorFragment) childFragment).openDocumentFromMenu();
                }
            }
            return true;
        } else if (itemId == R.id.action_sort) {
            // Handle sort action for documents screen
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (currentFragment instanceof NavHostFragment) {
                Fragment childFragment = ((NavHostFragment) currentFragment).getChildFragmentManager().getFragments().get(0);
                if (childFragment instanceof DocsViewerEditorFragment) {
                    ((DocsViewerEditorFragment) childFragment).showSortDialog();
                }
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
