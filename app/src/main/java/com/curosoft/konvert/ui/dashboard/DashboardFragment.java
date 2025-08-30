package com.curosoft.konvert.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;
import com.curosoft.konvert.ui.dashboard.adapters.ConvertAdapter;
import com.curosoft.konvert.ui.dashboard.adapters.RecentConvertAdapter;
import com.curosoft.konvert.ui.dashboard.models.ConvertItem;
import com.curosoft.konvert.ui.dashboard.models.RecentConvertItem;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private RecyclerView recentConvertsRecyclerView;
    private RecyclerView convertGridRecyclerView;
    private TextView noConvertsTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recentConvertsRecyclerView = view.findViewById(R.id.recent_converts_recycler);
        convertGridRecyclerView = view.findViewById(R.id.convert_grid);
        noConvertsTextView = view.findViewById(R.id.no_converts_text);
        
        // Set up the Recent Converts RecyclerView
        setupRecentConverts();
        
        // Set up the Convert Grid RecyclerView
        setupConvertGrid();
    }
    
    private void setupRecentConverts() {
        // For now, we'll use dummy data
        List<RecentConvertItem> recentItems = getDummyRecentConvertItems();
        
        // Set layout manager and adapter
        recentConvertsRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recentConvertsRecyclerView.setAdapter(new RecentConvertAdapter(recentItems));
        
        // Show "No conversions yet" text if the list is empty
        if (recentItems.isEmpty()) {
            recentConvertsRecyclerView.setVisibility(View.GONE);
            noConvertsTextView.setVisibility(View.VISIBLE);
        } else {
            recentConvertsRecyclerView.setVisibility(View.VISIBLE);
            noConvertsTextView.setVisibility(View.GONE);
        }
    }
    
    private void setupConvertGrid() {
        // Get the list of conversion categories
        List<ConvertItem> convertItems = getConvertItems();
        
        // Set layout manager and adapter
        convertGridRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        convertGridRecyclerView.setAdapter(new ConvertAdapter(convertItems));
    }
    
    private List<RecentConvertItem> getDummyRecentConvertItems() {
        List<RecentConvertItem> items = new ArrayList<>();
        
        // For demonstration, add a couple of dummy items
        // In a real app, these would come from a database or file history
        items.add(new RecentConvertItem(
                R.drawable.ic_file_image,
                "vacation.jpg",
                "JPG → PNG",
                "2 minutes ago"
        ));
        
        items.add(new RecentConvertItem(
                R.drawable.ic_file_doc,
                "document.docx",
                "DOCX → PDF",
                "Yesterday"
        ));
        
        items.add(new RecentConvertItem(
                R.drawable.ic_file_doc,
                "presentation.pptx",
                "PPTX → PDF",
                "Aug 28, 2025"
        ));
        
        return items;
    }
    
    private List<ConvertItem> getConvertItems() {
        List<ConvertItem> items = new ArrayList<>();
        
        // Add the 5 conversion categories
        items.add(new ConvertItem(
                R.drawable.ic_docs,
                "Docs",
                "docs"
        ));
        
        items.add(new ConvertItem(
                R.drawable.ic_images,
                "Images",
                "images"
        ));
        
        items.add(new ConvertItem(
                R.drawable.ic_audio,
                "Audio",
                "audio"
        ));
        
        items.add(new ConvertItem(
                R.drawable.ic_video,
                "Video",
                "video"
        ));
        
        items.add(new ConvertItem(
                R.drawable.ic_archives,
                "Archives",
                "archives"
        ));
        
        return items;
    }
}
