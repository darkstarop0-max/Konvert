package com.curosoft.konvert.ui.docs;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.curosoft.konvert.R;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern document viewer fragment with search, sort, and comprehensive file management
 * Features: Real-time search, multiple sort options, context menus, and optimized performance
 */
public class DocsViewerEditorFragment extends Fragment implements DocsAdapter.OnDocumentActionListener {
    
    // UI Components
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private LinearLayout loadingStateLayout;
    private TextView emptyStateText;
    private EditText searchEditText;
    private ImageView clearSearchButton;
    private ImageView sortButton;
    private TextView documentCountText;
    private TextView sortStatusText;
    
    // Data and State
    private DocsAdapter adapter;
    private List<File> allDocuments = new ArrayList<>();
    private List<File> filteredDocuments = new ArrayList<>();
    private DocsScanner.SortBy currentSort = DocsScanner.SortBy.NAME_ASC;
    private String currentSearchQuery = "";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_docs_viewer_editor, container, false);
        initializeViews(view);
        setupRecyclerView();
        setupSearchFunctionality();
        setupSortFunctionality();
        loadDocuments();
        return view;
    }
    
    /**
     * Initialize all view components
     */
    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.docs_recycler_view);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        loadingStateLayout = view.findViewById(R.id.loadingStateLayout);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        searchEditText = view.findViewById(R.id.searchEditText);
        clearSearchButton = view.findViewById(R.id.clearSearchButton);
        sortButton = view.findViewById(R.id.sortButton);
        documentCountText = view.findViewById(R.id.documentCountText);
        sortStatusText = view.findViewById(R.id.sortStatusText);
    }
    
    /**
     * Setup RecyclerView with optimizations
     */
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true); // Performance optimization
        
        adapter = new DocsAdapter(getContext(), filteredDocuments);
        adapter.setActionListener(this);
        recyclerView.setAdapter(adapter);
    }
    
    /**
     * Setup real-time search functionality
     */
    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                updateClearButtonVisibility();
                performSearch();
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
        
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchEditText.clearFocus();
        });
    }
    
    /**
     * Setup sort functionality with popup menu
     */
    private void setupSortFunctionality() {
        sortButton.setOnClickListener(v -> showSortMenu());
        updateSortStatus();
    }
    
    /**
     * Show sort options popup menu
     */
    private void showSortMenu() {
        PopupMenu popup = new PopupMenu(getContext(), sortButton);
        
        popup.getMenu().add(0, 0, 0, "Name A-Z").setCheckable(true).setChecked(currentSort == DocsScanner.SortBy.NAME_ASC);
        popup.getMenu().add(0, 1, 0, "Name Z-A").setCheckable(true).setChecked(currentSort == DocsScanner.SortBy.NAME_DESC);
        popup.getMenu().add(0, 2, 0, "Size (Small to Large)").setCheckable(true).setChecked(currentSort == DocsScanner.SortBy.SIZE_ASC);
        popup.getMenu().add(0, 3, 0, "Size (Large to Small)").setCheckable(true).setChecked(currentSort == DocsScanner.SortBy.SIZE_DESC);
        popup.getMenu().add(0, 4, 0, "Date (Oldest First)").setCheckable(true).setChecked(currentSort == DocsScanner.SortBy.DATE_ASC);
        popup.getMenu().add(0, 5, 0, "Date (Newest First)").setCheckable(true).setChecked(currentSort == DocsScanner.SortBy.DATE_DESC);
        
        popup.setOnMenuItemClickListener(item -> {
            DocsScanner.SortBy newSort;
            switch (item.getItemId()) {
                case 0: newSort = DocsScanner.SortBy.NAME_ASC; break;
                case 1: newSort = DocsScanner.SortBy.NAME_DESC; break;
                case 2: newSort = DocsScanner.SortBy.SIZE_ASC; break;
                case 3: newSort = DocsScanner.SortBy.SIZE_DESC; break;
                case 4: newSort = DocsScanner.SortBy.DATE_ASC; break;
                case 5: newSort = DocsScanner.SortBy.DATE_DESC; break;
                default: return false;
            }
            
            if (newSort != currentSort) {
                currentSort = newSort;
                updateSortStatus();
                applySortAndFilter();
            }
            return true;
        });
        
        popup.show();
    }
    
    /**
     * Load documents asynchronously with progress updates
     */
    private void loadDocuments() {
        showLoadingState();
        
        DocsScanner.scanForDocumentsAsync(getContext(), new DocsScanner.DocumentScanListener() {
            @Override
            public void onScanStarted() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showLoadingState());
                }
            }
            
            @Override
            public void onDocumentsFound(List<File> documents) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allDocuments = new ArrayList<>(documents);
                        applySortAndFilter();
                    });
                }
            }
            
            @Override
            public void onScanProgress(int current, int total) {
                // Could update progress here if needed
            }
            
            @Override
            public void onScanComplete() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> hideLoadingState());
                }
            }
            
            @Override
            public void onScanError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        hideLoadingState();
                        Toast.makeText(getContext(), "Error scanning documents: " + error, Toast.LENGTH_LONG).show();
                        showEmptyState();
                    });
                }
            }
        });
    }
    
    /**
     * Apply current sort and filter settings
     */
    private void applySortAndFilter() {
        // First apply search filter
        List<File> searchFiltered = DocsScanner.filterDocuments(allDocuments, currentSearchQuery);
        
        // Then apply sort
        filteredDocuments = DocsScanner.sortDocuments(searchFiltered, currentSort);
        
        // Update UI
        updateDocumentCount();
        adapter.updateDocuments(filteredDocuments);
        
        // Show appropriate state
        if (filteredDocuments.isEmpty()) {
            if (currentSearchQuery.isEmpty()) {
                showEmptyState();
            } else {
                showNoSearchResultsState();
            }
        } else {
            showDocumentsState();
        }
    }
    
    /**
     * Perform search without changing sort
     */
    private void performSearch() {
        applySortAndFilter();
    }
    
    /**
     * Update document count display
     */
    private void updateDocumentCount() {
        if (documentCountText != null) {
            int totalCount = allDocuments.size();
            int filteredCount = filteredDocuments.size();
            
            String countText;
            if (currentSearchQuery.isEmpty()) {
                countText = totalCount + " document" + (totalCount != 1 ? "s" : "");
            } else {
                countText = filteredCount + " of " + totalCount + " document" + (totalCount != 1 ? "s" : "");
            }
            
            documentCountText.setText(countText);
        }
    }
    
    /**
     * Update sort status display
     */
    private void updateSortStatus() {
        if (sortStatusText != null) {
            sortStatusText.setText("Sort: " + DocsScanner.getSortDisplayName(currentSort));
        }
    }
    
    /**
     * Update clear search button visibility
     */
    private void updateClearButtonVisibility() {
        if (clearSearchButton != null) {
            clearSearchButton.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
    
    /**
     * Show loading state
     */
    private void showLoadingState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        loadingStateLayout.setVisibility(View.VISIBLE);
    }
    
    /**
     * Hide loading state
     */
    private void hideLoadingState() {
        loadingStateLayout.setVisibility(View.GONE);
    }
    
    /**
     * Show documents list
     */
    private void showDocumentsState() {
        loadingStateLayout.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
    
    /**
     * Show empty state (no documents found)
     */
    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        loadingStateLayout.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        emptyStateText.setText("No documents found");
    }
    
    /**
     * Show no search results state
     */
    private void showNoSearchResultsState() {
        recyclerView.setVisibility(View.GONE);
        loadingStateLayout.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        emptyStateText.setText("No documents match \"" + currentSearchQuery + "\"");
    }
    
    // Document Action Listener Implementation
    
    @Override
    public void onDocumentOpen(File document) {
        try {
            Intent intent = new Intent(getContext(), DocumentViewerActivity.class);
            Uri fileUri = FileProvider.getUriForFile(
                getContext(),
                getContext().getPackageName() + ".provider",
                document
            );
            intent.setData(fileUri);
            intent.putExtra("fileName", document.getName());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDocumentShare(File document) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            Uri fileUri = FileProvider.getUriForFile(
                getContext(),
                getContext().getPackageName() + ".provider",
                document
            );
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing: " + document.getName());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share document"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to share document", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDocumentRename(File document) {
        showRenameDialog(document);
    }
    
    @Override
    public void onDocumentDelete(File document) {
        showDeleteConfirmation(document);
    }
    
    @Override
    public void onDocumentInfo(File document) {
        // Info is handled by the adapter
    }
    
    /**
     * Show rename dialog
     */
    private void showRenameDialog(File document) {
        EditText editText = new EditText(getContext());
        editText.setText(document.getName());
        editText.selectAll();
        
        new AlertDialog.Builder(getContext())
            .setTitle("Rename Document")
            .setView(editText)
            .setPositiveButton("Rename", (dialog, which) -> {
                String newName = editText.getText().toString().trim();
                if (!newName.isEmpty() && !newName.equals(document.getName())) {
                    renameDocument(document, newName);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * Rename document file
     */
    private void renameDocument(File document, String newName) {
        File newFile = new File(document.getParent(), newName);
        if (document.renameTo(newFile)) {
            Toast.makeText(getContext(), "Document renamed", Toast.LENGTH_SHORT).show();
            loadDocuments(); // Refresh list
        } else {
            Toast.makeText(getContext(), "Failed to rename document", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show delete confirmation
     */
    private void showDeleteConfirmation(File document) {
        new AlertDialog.Builder(getContext())
            .setTitle("Delete Document")
            .setMessage("Are you sure you want to delete \"" + document.getName() + "\"?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteDocument(document))
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    /**
     * Delete document file
     */
    private void deleteDocument(File document) {
        if (document.delete()) {
            Toast.makeText(getContext(), "Document deleted", Toast.LENGTH_SHORT).show();
            // Remove from lists and refresh
            allDocuments.remove(document);
            applySortAndFilter();
        } else {
            Toast.makeText(getContext(), "Failed to delete document", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Refresh document list
     */
    public void refreshDocuments() {
        loadDocuments();
    }
}
