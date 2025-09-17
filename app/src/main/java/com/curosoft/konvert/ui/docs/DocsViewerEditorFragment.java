package com.curosoft.konvert.ui.docs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;
import com.curosoft.konvert.utils.DocumentAccessManager;
import com.curosoft.konvert.utils.MediaStoreDocumentScanner;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Professional document viewer fragment using MediaStore APIs
 * Similar to Adobe Acrobat Reader and WPS Office approach
 */
public class DocsViewerEditorFragment extends Fragment implements 
        MediaStoreDocumentScanner.DocumentScanListener,
        DocumentAccessManager.DocumentAccessListener,
        CleanDocumentAdapter.OnDocumentClickListener,
        CleanDocumentAdapter.OnDocumentLongClickListener,
        DocumentActionsBottomSheet.OnDocumentActionListener {
    
    private static final String TAG = "DocsViewerFragment";
    
    // Professional document scanning components
    private MediaStoreDocumentScanner mediaStoreScanner;
    private DocumentAccessManager accessManager;
    private CleanDocumentAdapter adapter;
    
    // UI Components
    private RecyclerView recyclerView;
    private LinearProgressIndicator progressIndicator;
    private LinearLayout emptyStateLayout;
    private LinearLayout loadingStateLayout;
    private MaterialButton addDocumentButton;
    private MaterialButton chooseFolderButton;
    
    // State management
    private boolean isInitialLoad = true;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize professional document scanner
        mediaStoreScanner = new MediaStoreDocumentScanner(requireContext());
        mediaStoreScanner.setListener(this);
        
        // Initialize document access manager
        accessManager = new DocumentAccessManager(this);
        accessManager.setListener(this);
        
        Log.d(TAG, "DocsViewerFragment created with professional scanning");
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_docs_viewer_editor, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupEmptyState();
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Start professional document scanning
        startDocumentScanning();
    }
    
    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.docs_recycler_view);
        progressIndicator = view.findViewById(R.id.progressIndicator);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        loadingStateLayout = view.findViewById(R.id.loadingStateLayout);
        
        // Look for SAF buttons in empty state (if they exist in layout)
        if (emptyStateLayout != null) {
            addDocumentButton = emptyStateLayout.findViewById(R.id.addDocumentButton);
            chooseFolderButton = emptyStateLayout.findViewById(R.id.chooseFolderButton);
        }
    }
    
    private void setupRecyclerView() {
        adapter = new CleanDocumentAdapter(requireContext());
        adapter.setOnDocumentClickListener(this);
        adapter.setOnDocumentLongClickListener(this);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupEmptyState() {
        if (addDocumentButton != null) {
            addDocumentButton.setOnClickListener(v -> {
                Log.d(TAG, "Add document button clicked");
                accessManager.launchDocumentPicker();
            });
        }
        
        if (chooseFolderButton != null) {
            chooseFolderButton.setOnClickListener(v -> {
                Log.d(TAG, "Choose folder button clicked");
                accessManager.launchFolderPicker();
            });
        }
    }
    
    /**
     * Start professional document scanning
     */
    private void startDocumentScanning() {
        Log.d(TAG, "Starting professional document scanning");
        
        // Check if we have cached documents first
        if (!isInitialLoad && !mediaStoreScanner.isCacheEmpty()) {
            List<MediaStoreDocumentScanner.DocumentInfo> cachedDocs = mediaStoreScanner.getCachedDocuments();
            updateDocumentList(cachedDocs);
            Log.d(TAG, "Using cached documents: " + cachedDocs.size());
            return;
        }
        
        // Check permissions and start scan
        if (accessManager.hasRequiredPermissions()) {
            mediaStoreScanner.startScan();
        } else {
            accessManager.checkAndRequestPermissions();
        }
    }
    
    /**
     * Update document list with professional document info
     */
    private void updateDocumentList(List<MediaStoreDocumentScanner.DocumentInfo> documents) {
        List<File> fileList = new ArrayList<>();
        for (MediaStoreDocumentScanner.DocumentInfo doc : documents) {
            fileList.add(doc.toFile());
        }
        
        adapter.updateDocuments(fileList);
        updateUIState(fileList.isEmpty());
    }
    
    /**
     * Update UI state based on document availability
     */
    private void updateUIState(boolean isEmpty) {
        if (loadingStateLayout != null) {
            loadingStateLayout.setVisibility(View.GONE);
        }
        
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.GONE);
        }
        
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Show loading state during scanning
     */
    private void showLoadingState() {
        if (isInitialLoad && loadingStateLayout != null) {
            loadingStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
        } else if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
    }
    
    // === MediaStoreDocumentScanner.DocumentScanListener ===
    
    @Override
    public void onScanStarted() {
        Log.d(TAG, "Document scan started");
        showLoadingState();
    }
    
    @Override
    public void onDocumentsFound(List<MediaStoreDocumentScanner.DocumentInfo> documents) {
        Log.d(TAG, "Found " + documents.size() + " documents via MediaStore");
        updateDocumentList(documents);
        isInitialLoad = false;
    }
    
    @Override
    public void onScanCompleted(int totalFound) {
        Log.d(TAG, "Document scan completed. Total: " + totalFound);
        updateUIState(totalFound == 0);
        isInitialLoad = false;
    }
    
    @Override
    public void onScanError(String error) {
        Log.e(TAG, "Document scan error: " + error);
        updateUIState(true);
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "Error scanning documents: " + error, Toast.LENGTH_SHORT).show();
        }
        isInitialLoad = false;
    }
    
    @Override
    public void onPermissionRequired() {
        Log.d(TAG, "Permission required for MediaStore access");
        
        // Show graceful fallback UI
        updateUIState(true);
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "Choose documents manually using the buttons below", Toast.LENGTH_LONG).show();
        }
        isInitialLoad = false;
    }
    
    // === DocumentAccessManager.DocumentAccessListener ===
    
    @Override
    public void onPermissionGranted() {
        Log.d(TAG, "Permission granted, starting MediaStore scan");
        mediaStoreScanner.startScan();
    }
    
    @Override
    public void onPermissionDenied() {
        Log.d(TAG, "Permission denied, showing SAF fallback");
        updateUIState(true);
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "Use the buttons below to access your documents", Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onDocumentPicked(Uri uri) {
        Log.d(TAG, "Document picked via SAF: " + uri);
        
        // Open the picked document directly
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), DocumentViewerActivity.class);
            intent.setData(uri);
            startActivity(intent);
        }
    }
    
    @Override
    public void onFolderPicked(Uri uri) {
        Log.d(TAG, "Folder picked via SAF: " + uri);
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "Folder access feature coming soon!", Toast.LENGTH_SHORT).show();
        }
        
        // TODO: Implement folder scanning via SAF
        // accessManager.getDocumentsFromFolder(uri, documents -> {
        //     // Update UI with documents from picked folder
        // });
    }
    
    @Override
    public void onAccessError(String error) {
        Log.e(TAG, "Document access error: " + error);
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "Error accessing documents: " + error, Toast.LENGTH_SHORT).show();
        }
    }
    
    // === Document Click Handlers (keeping existing functionality) ===
    
    @Override
    public void onDocumentClick(File document) {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), DocumentViewerActivity.class);
            intent.putExtra("file_path", document.getAbsolutePath());
            intent.putExtra("file_name", document.getName());
            startActivity(intent);
        }
    }
    
    @Override
    public void onDocumentLongClick(File document, View view) {
        DocumentActionsBottomSheet bottomSheet = DocumentActionsBottomSheet.newInstance(document);
        bottomSheet.setOnDocumentActionListener(this);
        bottomSheet.show(getParentFragmentManager(), "DocumentActions");
    }
    
    // === DocumentActionsBottomSheet.OnDocumentActionListener (keeping existing) ===
    
    @Override
    public void onDocumentDeleted(File document) {
        // Refresh the list after deletion
        mediaStoreScanner.clearCache();
        startDocumentScanning();
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "Document deleted", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDocumentRenamed(File oldFile, File newFile) {
        // Refresh the list after rename
        mediaStoreScanner.clearCache();
        startDocumentScanning();
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "Document renamed", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDocumentOpened(File document) {
        // Open the document
        onDocumentClick(document);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Professional apps refresh on resume, but with smart caching
        if (!isInitialLoad && !mediaStoreScanner.isCacheEmpty()) {
            // Quick refresh from cache
            List<MediaStoreDocumentScanner.DocumentInfo> cachedDocs = mediaStoreScanner.getCachedDocuments();
            updateDocumentList(cachedDocs);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clean up resources
        if (mediaStoreScanner != null) {
            mediaStoreScanner.cleanup();
        }
        
        Log.d(TAG, "DocsViewerFragment destroyed");
    }
    
    /**
     * Show sort menu for documents
     */
    public void showSortMenu() {
        // For now, just clear cache and refresh documents
        Log.d(TAG, "Sort menu requested - clearing cache and refreshing");
        if (mediaStoreScanner != null) {
            mediaStoreScanner.clearCache();
            startDocumentScanning();
        }
    }
}
