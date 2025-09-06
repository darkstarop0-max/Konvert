package com.curosoft.konvert.ui.docs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;
import com.curosoft.konvert.utils.DocumentFileScanner;
import com.curosoft.konvert.utils.DocumentSorter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean and minimal document viewer fragment
 * Features: Real-time file scanning, minimal UI, document opening, context actions
 */
public class DocsViewerEditorFragment extends Fragment implements 
        DocumentFileScanner.FileChangeListener, 
        CleanDocumentAdapter.OnDocumentClickListener,
        CleanDocumentAdapter.OnDocumentLongClickListener,
        DocumentActionsBottomSheet.OnDocumentActionListener {
    
    // UI Components
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private LinearLayout loadingStateLayout;
    
    // Data and Utilities
    private CleanDocumentAdapter adapter;
    private DocumentFileScanner fileScanner;
    private List<File> allDocuments = new ArrayList<>();
    private DocumentSorter.SortBy currentSortBy = DocumentSorter.SortBy.NAME_ASC;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_docs_viewer_editor, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerView();
        setupFileScanner();
        
        // Start scanning for documents
        showLoadingState();
        fileScanner.startScanning();
    }
    
    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.docs_recycler_view);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        loadingStateLayout = view.findViewById(R.id.loadingStateLayout);
    }
    
    private void setupRecyclerView() {
        adapter = new CleanDocumentAdapter(requireContext());
        adapter.setOnDocumentClickListener(this);
        adapter.setOnDocumentLongClickListener(this);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }
    
    private void setupFileScanner() {
        fileScanner = new DocumentFileScanner(requireContext());
        fileScanner.addFileChangeListener(this);
    }
    
    private void showLoadingState() {
        loadingStateLayout.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }
    
    private void showDocuments(List<File> documents) {
        loadingStateLayout.setVisibility(View.GONE);
        
        if (documents.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.updateDocuments(documents);
        }
    }
    
    // DocumentFileScanner.FileChangeListener implementation
    @Override
    public void onFilesChanged(List<File> documents) {
        if (getActivity() != null) {
            allDocuments = new ArrayList<>(documents);
            applySorting();
        }
    }
    
    private void applySorting() {
        List<File> sortedDocuments = DocumentSorter.sortDocuments(allDocuments, currentSortBy);
        showDocuments(sortedDocuments);
    }
    
    @Override
    public void onFileAdded(File file) {
        // File scanner will trigger onFilesChanged which updates the list
    }
    
    @Override
    public void onFileRemoved(File file) {
        // File scanner will trigger onFilesChanged which updates the list
    }
    
    // CleanDocumentAdapter.OnDocumentClickListener implementation
    @Override
    public void onDocumentClick(File document) {
        openDocument(document);
    }
    
    // CleanDocumentAdapter.OnDocumentLongClickListener implementation
    @Override
    public void onDocumentLongClick(File document, View view) {
        showDocumentActions(document);
    }
    
    private void showDocumentActions(File document) {
        DocumentActionsBottomSheet bottomSheet = DocumentActionsBottomSheet.newInstance(document);
        bottomSheet.setOnDocumentActionListener(this);
        bottomSheet.show(getParentFragmentManager(), "document_actions");
    }
    
    // DocumentActionsBottomSheet.OnDocumentActionListener implementation
    @Override
    public void onDocumentDeleted(File document) {
        // Refresh the document list
        if (fileScanner != null) {
            fileScanner.scanForDocuments();
        }
    }
    
    @Override
    public void onDocumentRenamed(File oldFile, File newFile) {
        // Refresh the document list
        if (fileScanner != null) {
            fileScanner.scanForDocuments();
        }
    }
    
    @Override
    public void onDocumentOpened(File document) {
        openDocument(document);
    }
    
    /**
     * Show sort menu - called from MainActivity
     */
    public void showSortMenu() {
        if (getActivity() != null && getActivity().findViewById(R.id.toolbar) != null) {
            View anchor = getActivity().findViewById(R.id.toolbar);
            
            PopupMenu popup = new PopupMenu(requireContext(), anchor);
            
            // Add menu items
            DocumentSorter.SortBy[] sortOptions = DocumentSorter.SortBy.values();
            for (int i = 0; i < sortOptions.length; i++) {
                popup.getMenu().add(0, i, i, sortOptions[i].getDisplayName())
                        .setCheckable(true)
                        .setChecked(sortOptions[i] == currentSortBy);
            }
            
            popup.setOnMenuItemClickListener(item -> {
                DocumentSorter.SortBy newSort = sortOptions[item.getItemId()];
                if (newSort != currentSortBy) {
                    currentSortBy = newSort;
                    applySorting();
                }
                return true;
            });
            
            popup.show();
        }
    }
    
    private void openDocument(File document) {
        Intent intent = new Intent(requireContext(), DocumentViewerActivity.class);
        intent.putExtra("document_path", document.getAbsolutePath());
        intent.putExtra("document_name", document.getName());
        startActivity(intent);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh documents when returning to the fragment
        if (fileScanner != null) {
            fileScanner.scanForDocuments();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fileScanner != null) {
            fileScanner.removeFileChangeListener(this);
            fileScanner.destroy();
        }
    }
}
