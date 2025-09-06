package com.curosoft.konvert.ui.docs;

import android.content.Intent;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.SeekBar;

import com.curosoft.konvert.R;
import com.curosoft.konvert.utils.EnhancedDocumentConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DocumentViewerActivity extends AppCompatActivity {
    
    // UI Components
    private Toolbar toolbar;
    private LinearLayout textContainer, controlsBottomSheet;
    private TextView documentTitle, documentContent, pageIndicator;
    private RecyclerView pdfPagesRecyclerView;
    private WebView docxWebView;
    private FloatingActionButton fabShare;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    
    // Controls
    private LinearLayout textSizeControls, pdfZoomControls, pageNavigationControls;
    private SeekBar textSizeSlider, pdfZoomSlider;
    private ImageButton btnPrevPage, btnNextPage;
    
    // Document state
    private String fileName;
    private Uri fileUri;
    private String documentType; // "pdf", "docx", "txt"
    
    // Gesture detection for pinch-to-zoom
    private ScaleGestureDetector scaleGestureDetector;
    private float textScaleFactor = 1.0f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;
    
    // PDF components
    private PdfPageAdapter pdfAdapter;
    private PdfRenderer pdfRenderer;
    private int totalPages = 0;
    private int currentPageIndex = 0;
    
    // Document display state
    private float currentTextSize = 16f;
    private float currentZoom = 1.0f;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_viewer);
        
        initializeViews();
        setupBottomSheet();
        setupClickListeners();
        
        // Get document info from intent
        fileUri = getIntent().getData();
        fileName = getIntent().getStringExtra("fileName");
        
        // Setup toolbar after getting fileName
        setupToolbar();
        
        // Fallback: try to get file path from extras and convert to URI
        if (fileUri == null) {
            String filePath = getIntent().getStringExtra("file_path");
            if (filePath != null) {
                try {
                    File file = new File(filePath);
                    if (file.exists() && file.canRead()) {
                        fileUri = androidx.core.content.FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".provider",
                            file
                        );
                    } else {
                        showError("File not found or cannot be read: " + filePath);
                        finish();
                        return;
                    }
                } catch (Exception e) {
                    try {
                        File file = new File(filePath);
                        if (file.exists() && file.canRead()) {
                            fileUri = Uri.fromFile(file);
                        } else {
                            showError("File not found: " + filePath);
                            finish();
                            return;
                        }
                    } catch (Exception ex) {
                        showError("Unable to access file: " + ex.getMessage());
                        finish();
                        return;
                    }
                }
            }
        }
        
        if (fileUri == null) {
            showError("Unable to open document: No file specified");
            finish();
            return;
        }
        
        // Set document title
        if (fileName != null) {
            documentTitle.setText(fileName);
        }
        
        // Validate document access before proceeding
        if (!validateDocumentAccess()) {
            finish();
            return;
        }
        
        // Determine document type and display
        determineDocumentTypeAndDisplay();
    }
    
    /**
     * Validate that we can access the document URI
     */
    private boolean validateDocumentAccess() {
        try {
            // Try to get the input stream to verify access
            getContentResolver().openInputStream(fileUri).close();
            return true;
        } catch (Exception e) {
            showError("Cannot access document: " + e.getMessage());
            return false;
        }
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        textContainer = findViewById(R.id.textContainer);
        documentTitle = findViewById(R.id.documentTitle);
        documentContent = findViewById(R.id.documentContent);
        pdfPagesRecyclerView = findViewById(R.id.pdfPagesRecyclerView);
        docxWebView = findViewById(R.id.docxWebView);
        fabShare = findViewById(R.id.fabShare);
        controlsBottomSheet = findViewById(R.id.controlsBottomSheet);
        
        // Controls
        textSizeControls = findViewById(R.id.textSizeControls);
        pdfZoomControls = findViewById(R.id.pdfZoomControls);
        pageNavigationControls = findViewById(R.id.pageNavigationControls);
        textSizeSlider = findViewById(R.id.textSizeSlider);
        pdfZoomSlider = findViewById(R.id.pdfZoomSlider);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        pageIndicator = findViewById(R.id.pageIndicator);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            // Set document filename as title
            if (fileName != null) {
                getSupportActionBar().setTitle(fileName);
            } else {
                getSupportActionBar().setTitle("Document");
            }
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(controlsBottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setPeekHeight(0);
    }
    
    private void setupClickListeners() {
        fabShare.setOnClickListener(v -> shareDocument());
        
        // Setup pinch-to-zoom for text content
        setupPinchToZoom();
        
        // Text size slider
        textSizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTextSize = 12 + progress; // Range 12-24
                    documentContent.setTextSize(currentTextSize);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // PDF zoom slider
        pdfZoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentZoom = 0.5f + (progress * 0.1f); // Range 0.5-3.0
                    if (pdfAdapter != null) {
                        pdfAdapter.setZoomLevel(currentZoom);
                    }
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Page navigation
        btnPrevPage.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                scrollToPage(currentPageIndex);
                updatePageIndicator();
            }
        });
        
        btnNextPage.setOnClickListener(v -> {
            if (currentPageIndex < totalPages - 1) {
                currentPageIndex++;
                scrollToPage(currentPageIndex);
                updatePageIndicator();
            }
        });
    }
    
    private void setupPinchToZoom() {
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if ("txt".equals(documentType)) {
                    textScaleFactor *= detector.getScaleFactor();
                    textScaleFactor = Math.max(MIN_SCALE, Math.min(textScaleFactor, MAX_SCALE));
                    
                    // Apply scale to text size
                    float newTextSize = currentTextSize * textScaleFactor;
                    documentContent.setTextSize(newTextSize);
                    
                    // Update slider to reflect new size
                    int sliderProgress = Math.max(0, Math.min(12, (int)(newTextSize - 12)));
                    textSizeSlider.setProgress(sliderProgress);
                    
                    return true;
                }
                return false;
            }
        });
        
        // Set touch listener on text container for pinch-to-zoom
        textContainer.setOnTouchListener((v, event) -> {
            if ("txt".equals(documentType)) {
                scaleGestureDetector.onTouchEvent(event);
                return true;
            }
            return false;
        });
        
        // Setup pinch-to-zoom for PDF RecyclerView
        ScaleGestureDetector pdfScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if ("pdf".equals(documentType) && pdfAdapter != null) {
                    currentZoom *= detector.getScaleFactor();
                    currentZoom = Math.max(0.5f, Math.min(currentZoom, 3.0f));
                    
                    pdfAdapter.setZoomLevel(currentZoom);
                    
                    // Update zoom slider
                    int sliderProgress = (int)((currentZoom - 0.5f) * 10); // Convert 0.5-3.0 to 0-25
                    pdfZoomSlider.setProgress(sliderProgress);
                    
                    return true;
                }
                return false;
            }
        });
        
        pdfPagesRecyclerView.setOnTouchListener((v, event) -> {
            if ("pdf".equals(documentType)) {
                pdfScaleDetector.onTouchEvent(event);
            }
            return false; // Let RecyclerView handle scrolling
        });
    }
    
    private void determineDocumentTypeAndDisplay() {
        String mimeType = getContentResolver().getType(fileUri);
        if (mimeType == null && fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".pdf")) mimeType = "application/pdf";
            else if (lower.endsWith(".txt")) mimeType = "text/plain";
            else if (lower.endsWith(".docx")) mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        
        if ("application/pdf".equals(mimeType)) {
            documentType = "pdf";
            displayPdf();
        } else if ("text/plain".equals(mimeType)) {
            documentType = "txt";
            displayTextFile();
        } else if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType)) {
            documentType = "docx";
            displayDocxFile();
        } else {
            showError("Unsupported file format");
        }
    }
    
    private void displayPdf() {
        hideAllViews();
        findViewById(R.id.pdfContainer).setVisibility(View.VISIBLE);
        
        // Show PDF controls
        pdfZoomControls.setVisibility(View.VISIBLE);
        pageNavigationControls.setVisibility(View.VISIBLE);
        textSizeControls.setVisibility(View.GONE);
        
        // Reset zoom level for new document
        currentZoom = 1.0f;
        
        try {
            ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(fileUri, "r");
            if (fd == null) {
                showError("Cannot access PDF file");
                return;
            }
            
            pdfRenderer = new PdfRenderer(fd);
            totalPages = pdfRenderer.getPageCount();
            currentPageIndex = 0;
            
            if (totalPages == 0) {
                showError("PDF file has no pages");
                return;
            }
            
            // Setup RecyclerView for PDF pages
            pdfAdapter = new PdfPageAdapter();
            pdfPagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            pdfPagesRecyclerView.setAdapter(pdfAdapter);
            
            pdfAdapter.setPdfRenderer(pdfRenderer);
            updatePageIndicator();
            
            // Show success message for debugging
            Toast.makeText(this, "PDF loaded: " + totalPages + " pages", Toast.LENGTH_SHORT).show();
            
        } catch (IOException | SecurityException e) {
            showError("Failed to open PDF: " + e.getMessage());
        } catch (Exception e) {
            showError("Unexpected error opening PDF: " + e.getMessage());
        }
    }
    
    private void displayTextFile() {
        hideAllViews();
        textContainer.setVisibility(View.VISIBLE);
        
        // Show text controls
        textSizeControls.setVisibility(View.VISIBLE);
        pdfZoomControls.setVisibility(View.GONE);
        pageNavigationControls.setVisibility(View.GONE);
        
        // Reset text scale factor for new document
        textScaleFactor = 1.0f;
        
        try {
            String content = readTextFromUri(fileUri);
            if (content != null) {
                documentContent.setText(content);
                documentContent.setTextSize(currentTextSize);
                textSizeSlider.setProgress((int)(currentTextSize - 12)); // Convert back to 0-12 range
            } else {
                showError("Failed to read text file");
            }
        } catch (Exception e) {
            showError("Error reading text file: " + e.getMessage());
        }
    }
    
    private void displayDocxFile() {
        hideAllViews();
        docxWebView.setVisibility(View.VISIBLE);
        
        // Show text controls for DOCX
        textSizeControls.setVisibility(View.VISIBLE);
        pdfZoomControls.setVisibility(View.GONE);
        pageNavigationControls.setVisibility(View.GONE);
        
        try {
            // Try to get real file path first
            String filePath = getRealPathFromUri(fileUri);
            
            if (filePath != null && new File(filePath).exists()) {
                // Use file path if available
                EnhancedDocumentConverter converter = new EnhancedDocumentConverter();
                String htmlContent = converter.convertDocxToHtml(filePath);
                
                if (htmlContent != null) {
                    setupWebViewAndLoadContent(htmlContent);
                } else {
                    showError("Failed to convert DOCX file");
                }
            } else {
                // Fallback: Copy URI content to temp file and convert
                convertDocxFromUri();
            }
        } catch (Exception e) {
            showError("Error loading DOCX: " + e.getMessage());
        }
    }
    
    private void convertDocxFromUri() {
        try {
            // Create a temporary file to store the DOCX content
            File tempDir = new File(getCacheDir(), "temp_docx");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            File tempFile = new File(tempDir, "temp_document.docx");
            
            // Copy URI content to temp file
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
                 java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile)) {
                
                if (inputStream == null) {
                    showError("Cannot read DOCX file");
                    return;
                }
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            // Convert the temporary file
            EnhancedDocumentConverter converter = new EnhancedDocumentConverter();
            String htmlContent = converter.convertDocxToHtml(tempFile.getAbsolutePath());
            
            if (htmlContent != null) {
                setupWebViewAndLoadContent(htmlContent);
            } else {
                showError("Failed to convert DOCX file");
            }
            
            // Clean up temp file
            tempFile.delete();
            
        } catch (Exception e) {
            showError("Error processing DOCX: " + e.getMessage());
        }
    }
    
    private void setupWebViewAndLoadContent(String htmlContent) {
        docxWebView.getSettings().setJavaScriptEnabled(false);
        docxWebView.getSettings().setLoadWithOverviewMode(true);
        docxWebView.getSettings().setUseWideViewPort(true);
        docxWebView.getSettings().setBuiltInZoomControls(true);
        docxWebView.getSettings().setDisplayZoomControls(false);
        docxWebView.getSettings().setSupportZoom(true);
        
        docxWebView.setWebViewClient(new WebViewClient());
        docxWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
    }
    
    private void hideAllViews() {
        textContainer.setVisibility(View.GONE);
        findViewById(R.id.pdfContainer).setVisibility(View.GONE);
        docxWebView.setVisibility(View.GONE);
    }
    
    private void scrollToPage(int pageIndex) {
        if (pdfPagesRecyclerView != null) {
            pdfPagesRecyclerView.smoothScrollToPosition(pageIndex);
        }
    }
    
    private void updatePageIndicator() {
        if (totalPages > 0) {
            pageIndicator.setText(String.format("%d of %d", currentPageIndex + 1, totalPages));
        }
    }
    
    private String readTextFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            
            reader.close();
            inputStream.close();
            
            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private String getRealPathFromUri(Uri uri) {
        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }
        
        // For FileProvider URIs, try to extract the original file path
        if ("content".equals(uri.getScheme())) {
            String authority = uri.getAuthority();
            if (authority != null && authority.endsWith(".provider")) {
                // This is likely a FileProvider URI
                String path = uri.getPath();
                if (path != null) {
                    // FileProvider paths usually start with /external_files/, /cache_files/, etc.
                    if (path.startsWith("/external_files/")) {
                        // Try to construct the real external storage path
                        String relativePath = path.substring("/external_files/".length());
                        File externalStorage = android.os.Environment.getExternalStorageDirectory();
                        File realFile = new File(externalStorage, relativePath);
                        if (realFile.exists()) {
                            return realFile.getAbsolutePath();
                        }
                    }
                    // For other paths, we can't easily determine the real path
                    // Return null to trigger the URI-based fallback
                }
            }
        }
        
        return null;
    }
    
    private void shareDocument() {
        if (fileUri == null) return;
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("*/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        String title = "Share " + (fileName != null ? fileName : "Document");
        startActivity(Intent.createChooser(shareIntent, title));
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }
    
    private void cleanup() {
        if (pdfAdapter != null) {
            pdfAdapter.cleanup();
        }
        
        if (pdfRenderer != null) {
            try {
                pdfRenderer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (docxWebView != null) {
            docxWebView.destroy();
        }
    }
}
