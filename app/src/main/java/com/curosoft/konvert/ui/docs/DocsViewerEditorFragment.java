package com.curosoft.konvert.ui.docs;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
    
    // Permission handling
    private static final long SCAN_DEBOUNCE_DELAY = 5000; // 5 seconds
    private long lastScanTime = 0;
    private ActivityResultLauncher<String[]> permissionLauncher;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize permission launcher
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    // Permissions granted, start scanning
                    startDocumentScanning();
                } else {
                    // Some permissions denied, show limited functionality
                    Toast.makeText(requireContext(), 
                        "Storage permissions needed to scan all documents", 
                        Toast.LENGTH_LONG).show();
                    startDocumentScanning(); // Still try to scan accessible areas
                }
            }
        );
    }
    
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
        
        // Check permissions and start scanning
        checkPermissionsAndStartScanning();
    }
    
    /**
     * Check storage permissions and request if needed
     */
    private void checkPermissionsAndStartScanning() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires granular media permissions
            if (ContextCompat.checkSelfPermission(requireContext(), 
                    "android.permission.READ_MEDIA_DOCUMENTS") != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add("android.permission.READ_MEDIA_DOCUMENTS");
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+ requires READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            // Request permissions
            permissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
        } else {
            // Permissions already granted
            startDocumentScanning();
        }
    }
    
    /**
     * Start document scanning
     */
    private void startDocumentScanning() {
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
    
    @Override
    public void onScanStarted() {
        if (getActivity() != null) {
            showLoadingState();
        }
    }
    
    @Override
    public void onScanProgress(int current, int total) {
        // Optional: Could show progress indicator here if needed
        // For now, just keep showing loading state
    }
    
    @Override
    public void onScanCompleted() {
        // Scanning completed, final results will come through onFilesChanged
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
        try {
            Intent intent = new Intent(requireContext(), DocumentViewerActivity.class);
            
            // Create a content URI using FileProvider
            Uri documentUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                document
            );
            
            // Set the URI as data and add extras as fallback
            intent.setData(documentUri);
            intent.putExtra("file_path", document.getAbsolutePath());
            intent.putExtra("fileName", document.getName());
            
            // Grant read permission for the URI
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(intent);
        } catch (Exception e) {
            // Fallback: use file path only
            Intent intent = new Intent(requireContext(), DocumentViewerActivity.class);
            intent.putExtra("file_path", document.getAbsolutePath());
            intent.putExtra("fileName", document.getName());
            startActivity(intent);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh documents when returning to the fragment, but not too frequently
        long currentTime = System.currentTimeMillis();
        if (fileScanner != null && (currentTime - lastScanTime) > SCAN_DEBOUNCE_DELAY) {
            lastScanTime = currentTime;
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
