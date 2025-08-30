package com.curosoft.konvert.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.curosoft.konvert.MainActivity;
import com.curosoft.konvert.R;
import com.curosoft.konvert.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private Button nextButton;
    private Button skipButton;
    private Button getStartedButton;
    private OnboardingAdapter onboardingAdapter;
    private PreferenceManager preferenceManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make the activity full screen with edge-to-edge content
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        
        setContentView(R.layout.activity_onboarding);
        
        // Initialize preference manager
        preferenceManager = PreferenceManager.getInstance(this);
        
        // Skip onboarding if already shown
        if (!preferenceManager.isFirstTimeLaunch()) {
            navigateToMainActivity();
            return;
        }
        
        // Initialize views
        viewPager = findViewById(R.id.onboarding_viewpager);
        dotsLayout = findViewById(R.id.layoutDots);
        nextButton = findViewById(R.id.nextButton);
        skipButton = findViewById(R.id.skipButton);
        getStartedButton = findViewById(R.id.buttonGetStarted);
        
        // Setup the onboarding content
        setupOnboardingItems();
        
        // Add dot indicators
        setupIndicators();
        
        // ViewPager page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                updateButtons(position);
            }
        });
        
        // Setup click listeners
        setupClickListeners();
        
        // Set custom animations for ViewPager2
        setPageTransformer();
    }
    
    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();
        
        // Page 1: Welcome
        onboardingItems.add(new OnboardingItem(
                R.drawable.illustration_onboarding_1,
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_subtitle_1)
        ));
        
        // Page 2: Fast & Reliable
        onboardingItems.add(new OnboardingItem(
                R.drawable.illustration_onboarding_2,
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_subtitle_2)
        ));
        
        // Page 3: All Formats
        onboardingItems.add(new OnboardingItem(
                R.drawable.illustration_onboarding_3,
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_subtitle_3)
        ));
        
        // Page 4: Privacy
        onboardingItems.add(new OnboardingItem(
                R.drawable.illustration_onboarding_4,
                getString(R.string.onboarding_title_4),
                getString(R.string.onboarding_subtitle_4)
        ));
        
        // Set up adapter
        onboardingAdapter = new OnboardingAdapter(onboardingItems);
        viewPager.setAdapter(onboardingAdapter);
    }
    
    private void setupIndicators() {
        // Create initial indicators
        updateIndicators(0);
    }
    
    private void updateIndicators(int position) {
        // Clear existing dots
        dotsLayout.removeAllViews();
        
        // Create new dots
        ImageView[] dots = new ImageView[onboardingAdapter.getItemCount()];
        
        // Create and add dots to layout
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(this);
            
            // Set layout parameters with margins between dots
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dots[i].setLayoutParams(params);
            
            // Set active or inactive dot image
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, 
                    i == position ? R.drawable.dot_active : R.drawable.dot_inactive));
            
            // Add dot to layout
            dotsLayout.addView(dots[i]);
        }
    }
    
    private void updateButtons(int position) {
        if (position == onboardingAdapter.getItemCount() - 1) {
            // Last page - show Get Started button, hide Next/Skip buttons
            findViewById(R.id.layoutControls).findViewById(R.id.nextButton).setVisibility(View.GONE);
            getStartedButton.setVisibility(View.VISIBLE);
            skipButton.setVisibility(View.INVISIBLE);
        } else {
            // Not last page - show Next/Skip buttons, hide Get Started button
            findViewById(R.id.layoutControls).findViewById(R.id.nextButton).setVisibility(View.VISIBLE);
            getStartedButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.VISIBLE);
        }
    }
    
    private void setupClickListeners() {
        // Skip button - go directly to main activity
        skipButton.setOnClickListener(v -> navigateToMainActivity());
        
        // Next button - go to next page
        nextButton.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            if (currentPosition < onboardingAdapter.getItemCount() - 1) {
                viewPager.setCurrentItem(currentPosition + 1);
            }
        });
        
        // Get Started button - go to main activity
        getStartedButton.setOnClickListener(v -> navigateToMainActivity());
    }
    
    private void setPageTransformer() {
        // Add a cool animation effect when swiping between pages
        viewPager.setPageTransformer((page, position) -> {
            float absPosition = Math.abs(position);
            
            // Fade effect
            page.setAlpha(1.0f - absPosition * 0.5f);
            
            // Scale effect
            float scale = 1.0f - (absPosition * 0.1f);
            page.setScaleX(scale);
            page.setScaleY(scale);
            
            // Slide effect
            page.setTranslationX(-position * page.getWidth() / 4);
        });
    }
    
    private void navigateToMainActivity() {
        // Mark onboarding as completed
        preferenceManager.setFirstTimeLaunch(false);
        
        // Navigate to MainActivity
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        
        // Apply transition animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        
        // Close this activity
        finish();
    }
}
