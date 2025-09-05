package com.curosoft.konvert.ui.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
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
        
        // Adjust for status bar height
        adjustForStatusBar();
        
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
        
        // ViewPager page change listener with enhanced animations
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                updateButtons(position);
                animatePageTransition(position);
            }
            
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                // Add parallax effect to dots
                animateIndicatorScroll(position, positionOffset);
            }
        });
        
        // Setup click listeners
        setupClickListeners();
        
        // Set custom animations for ViewPager2
        setPageTransformer();
        
        // Add entrance animations
        animateEntranceElements();
    }
    
    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();
        
        // Page 1: Transform Anything
        onboardingItems.add(new OnboardingItem(
                R.drawable.illustration_onboarding_1,
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_subtitle_1)
        ));
        
        // Page 2: Lightning Fast
        onboardingItems.add(new OnboardingItem(
                R.drawable.illustration_onboarding_2,
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_subtitle_2)
        ));
        
        // Page 3: Privacy First
        onboardingItems.add(new OnboardingItem(
                R.drawable.illustration_onboarding_3,
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_subtitle_3)
        ));
        
        // Page 4: Ready to Launch
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
                // Animate button before proceeding
                animateButtonPress(nextButton, () -> {
                    viewPager.setCurrentItem(currentPosition + 1);
                });
            }
        });
        
        // Get Started button - go to main activity with animation
        getStartedButton.setOnClickListener(v -> {
            animateButtonPress(getStartedButton, this::navigateToMainActivity);
        });
    }
    
    private void setPageTransformer() {
        // Add Apple-inspired page transition effects
        viewPager.setPageTransformer((page, position) -> {
            float absPosition = Math.abs(position);
            
            if (absPosition >= 1.0f) {
                page.setAlpha(0f);
            } else {
                // Enhanced fade effect
                page.setAlpha(1.0f - absPosition * 0.3f);
                
                // Smooth scale effect
                float scale = Math.max(0.85f, 1.0f - absPosition * 0.15f);
                page.setScaleX(scale);
                page.setScaleY(scale);
                
                // Parallax translation
                page.setTranslationX(-position * page.getWidth() * 0.25f);
                
                // Rotate effect for cards
                page.setRotationY(position * -30);
            }
        });
    }
    
    private void animatePageTransition(int position) {
        // Animate dots and buttons when page changes
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(dotsLayout, "alpha", 1f, 0.7f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(dotsLayout, "alpha", 0.7f, 1f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(fadeOut, fadeIn);
        animatorSet.setDuration(150);
        animatorSet.start();
    }
    
    private void animateIndicatorScroll(int position, float offset) {
        // Smooth dot animation during scroll
        if (position < dotsLayout.getChildCount()) {
            View currentDot = dotsLayout.getChildAt(position);
            if (position + 1 < dotsLayout.getChildCount()) {
                View nextDot = dotsLayout.getChildAt(position + 1);
                
                // Interpolate scale between dots
                float currentScale = 1.0f - offset * 0.3f;
                float nextScale = 0.7f + offset * 0.3f;
                
                currentDot.setScaleX(currentScale);
                currentDot.setScaleY(currentScale);
                nextDot.setScaleX(nextScale);
                nextDot.setScaleY(nextScale);
            }
        }
    }
    
    private void animateButtonPress(View button, Runnable action) {
        // Create satisfying button press animation
        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(button,
                PropertyValuesHolder.ofFloat("scaleX", 0.95f),
                PropertyValuesHolder.ofFloat("scaleY", 0.95f));
        scaleDown.setDuration(100);
        
        ObjectAnimator scaleUp = ObjectAnimator.ofPropertyValuesHolder(button,
                PropertyValuesHolder.ofFloat("scaleX", 1.0f),
                PropertyValuesHolder.ofFloat("scaleY", 1.0f));
        scaleUp.setDuration(100);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(scaleDown, scaleUp);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (action != null) {
                    action.run();
                }
            }
        });
        animatorSet.start();
    }
    
    private void animateEntranceElements() {
        // Animate entrance of UI elements
        skipButton.setAlpha(0f);
        dotsLayout.setAlpha(0f);
        nextButton.setAlpha(0f);
        
        // Staggered entrance animation
        skipButton.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        
        dotsLayout.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        
        nextButton.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(700)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }
    
    private void navigateToMainActivity() {
        // Mark onboarding as completed
        preferenceManager.setFirstTimeLaunch(false);
        
        // Navigate to MainActivity with enhanced animation
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        
        // Add smooth transition animation
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(findViewById(android.R.id.content), "alpha", 1f, 0f);
        fadeOut.setDuration(300);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }
        });
        fadeOut.start();
    }
    
    private void adjustForStatusBar() {
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        if (statusBarSpacer != null) {
            // Get status bar height
            int statusBarHeight = getStatusBarHeight();
            ViewGroup.LayoutParams params = statusBarSpacer.getLayoutParams();
            params.height = statusBarHeight;
            statusBarSpacer.setLayoutParams(params);
        }
    }
    
    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        // Fallback to 24dp converted to pixels
        return (int) (24 * getResources().getDisplayMetrics().density);
    }
}
