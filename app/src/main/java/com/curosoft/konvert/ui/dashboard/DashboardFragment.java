package com.curosoft.konvert.ui.dashboard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;
import com.curosoft.konvert.ui.conversion.ConversionOptionBottomSheet;
import com.curosoft.konvert.ui.dashboard.adapters.RecentFileAdapter;
import com.curosoft.konvert.ui.dashboard.models.RecentFile;
import com.curosoft.konvert.ui.docs.DocumentViewerActivity;

import java.io.File;
import java.util.ArrayList;

public class DashboardFragment extends Fragment {

    private RecyclerView recentFilesRecyclerView;
    private LinearLayout emptyStateLayout;
    private RecentFileAdapter recentFileAdapter;
    private DashboardViewModel viewModel;
    
    // Premium conversion buttons
    private CardView btnDocuments, btnImages, btnAudio, btnVideo, btnFiles, btnMore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        
        // Initialize views
        recentFilesRecyclerView = view.findViewById(R.id.recent_files_recycler);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        
        // Initialize premium conversion buttons
        btnDocuments = view.findViewById(R.id.btn_documents);
        btnImages = view.findViewById(R.id.btn_images);
        btnAudio = view.findViewById(R.id.btn_audio);
        btnVideo = view.findViewById(R.id.btn_video);
        btnFiles = view.findViewById(R.id.btn_files);
        btnMore = view.findViewById(R.id.btn_more);
        
        // Set up button click listeners
        setupConversionButtons();
        
        // Set up the Recent Files RecyclerView
        setupRecentFiles();
        
        // Observe recent files LiveData
        observeRecentFiles();
    }
    
    private void setupConversionButtons() {
        // Active conversion buttons
        btnDocuments.setOnClickListener(v -> openConversionBottomSheet("docs"));
        btnImages.setOnClickListener(v -> openConversionBottomSheet("images"));
        
        // Coming Soon buttons with feedback
        btnAudio.setOnClickListener(v -> showComingSoonMessage("Audio"));
        btnVideo.setOnClickListener(v -> showComingSoonMessage("Video"));
        btnFiles.setOnClickListener(v -> showComingSoonMessage("Files"));
        btnMore.setOnClickListener(v -> showComingSoonMessage("More"));
        
        // Apply visual styling to coming soon buttons
        setupComingSoonStyling();
    }
    
    private void setupComingSoonStyling() {
        // Re-enable clicks for animation feedback but don't navigate
        btnAudio.setClickable(true);
        btnVideo.setClickable(true);
        btnFiles.setClickable(true);
        btnMore.setClickable(true);
        
        // Add subtle scale animation on tap
        setupButtonAnimation(btnAudio);
        setupButtonAnimation(btnVideo);
        setupButtonAnimation(btnFiles);
        setupButtonAnimation(btnMore);
    }
    
    private void setupButtonAnimation(android.view.View button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false; // Let the click listener handle the actual click
        });
    }
    
    private void showComingSoonMessage(String featureName) {
        android.widget.Toast.makeText(getContext(), 
            featureName + " conversion is coming soon!", 
            android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void openConversionBottomSheet(String category) {
        ConversionOptionBottomSheet bottomSheet = ConversionOptionBottomSheet.newInstance(category);
        bottomSheet.show(getParentFragmentManager(), "ConversionOptionBottomSheet");
    }
    
    private void setupRecentFiles() {
        // Set layout manager
        recentFilesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize adapter with empty list
        recentFileAdapter = new RecentFileAdapter(new ArrayList<>());
        recentFilesRecyclerView.setAdapter(recentFileAdapter);
        
        // Set click listener for recent files
        recentFileAdapter.setOnRecentFileClickListener(this::openRecentFile);
    }
    
    private void observeRecentFiles() {
        viewModel.getRecentFiles().observe(getViewLifecycleOwner(), recentFiles -> {
            // Update adapter
            recentFileAdapter.submitList(recentFiles);
            
            // Show/hide empty state
            if (recentFiles == null || recentFiles.isEmpty()) {
                recentFilesRecyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
            } else {
                recentFilesRecyclerView.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
            }
        });
    }
    
    private void openRecentFile(RecentFile recentFile) {
        // Open the file in DocumentViewerActivity
        if (recentFile.getFilePath() != null) {
            Intent intent = new Intent(getContext(), DocumentViewerActivity.class);
            try {
                // Convert file path to URI using FileProvider
                File file = new File(recentFile.getFilePath());
                Uri fileUri = FileProvider.getUriForFile(
                    getContext(),
                    getContext().getPackageName() + ".provider",
                    file
                );
                intent.setData(fileUri);
                intent.putExtra("fileName", recentFile.getFileName());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                // Fallback to regular file URI if FileProvider fails
                File file = new File(recentFile.getFilePath());
                Uri fileUri = Uri.fromFile(file);
                intent.setData(fileUri);
                intent.putExtra("fileName", recentFile.getFileName());
                startActivity(intent);
            }
        }
    }
    
    // Public method to add a converted file (call from conversion activities)
    public void addConvertedFile(String fileName, String filePath, String fromFormat, String toFormat) {
        if (viewModel != null) {
            viewModel.addConvertedFile(fileName, filePath, fromFormat, toFormat);
        }
    }
}
