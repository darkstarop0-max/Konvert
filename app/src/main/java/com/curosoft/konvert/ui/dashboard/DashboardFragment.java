package com.curosoft.konvert.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.cardview.widget.CardView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;
import com.curosoft.konvert.ui.conversion.ConversionOptionBottomSheet;
import com.curosoft.konvert.ui.dashboard.adapters.RecentFileAdapter;
import com.curosoft.konvert.ui.dashboard.models.RecentFile;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private RecyclerView recentFilesRecyclerView;
    private LinearLayout emptyStateLayout;
    
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
    }
    
    private void setupConversionButtons() {
        btnDocuments.setOnClickListener(v -> openConversionBottomSheet("docs"));
        btnImages.setOnClickListener(v -> openConversionBottomSheet("images"));
        btnAudio.setOnClickListener(v -> openConversionBottomSheet("audio"));
        btnVideo.setOnClickListener(v -> openConversionBottomSheet("video"));
        btnFiles.setOnClickListener(v -> openConversionBottomSheet("archives"));
        btnMore.setOnClickListener(v -> openConversionBottomSheet("more"));
    }
    
    private void openConversionBottomSheet(String category) {
        ConversionOptionBottomSheet bottomSheet = ConversionOptionBottomSheet.newInstance(category);
        bottomSheet.show(getParentFragmentManager(), "ConversionOptionBottomSheet");
    }
    
    private void setupRecentFiles() {
        // Get the list of recent files
        List<RecentFile> recentFiles = getRecentFiles();
        
        // Set layout manager and adapter
        recentFilesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        RecentFileAdapter adapter = new RecentFileAdapter(recentFiles);
        
        // Set click listener for recent files
        adapter.setOnRecentFileClickListener(recentFile -> {
            // Handle recent file click - could open file or show options
            // For now, just a placeholder
        });
        
        recentFilesRecyclerView.setAdapter(adapter);
        
        // Show empty state if no recent files
        if (recentFiles.isEmpty()) {
            recentFilesRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recentFilesRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    private List<RecentFile> getRecentFiles() {
        List<RecentFile> files = new ArrayList<>();
        
        // Add some dummy recent files for demonstration
        files.add(new RecentFile(R.drawable.ic_description, "vacation.jpg", "JPG → PNG", "2 minutes ago"));
        files.add(new RecentFile(R.drawable.ic_description, "document.docx", "DOCX → PDF", "Yesterday"));
        files.add(new RecentFile(R.drawable.ic_description, "presentation.pptx", "PPTX → PDF", "Aug 28, 2025"));
        
        return files;
    }
}
