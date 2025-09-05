package com.curosoft.konvert.ui.docs;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import com.curosoft.konvert.R;
import java.io.File;
import java.io.IOException;

public class DocumentViewerActivity extends AppCompatActivity {
    
    // UI Components
    private Toolbar toolbar;
    private ScrollView nestedScrollView;
    private CardView textContentCard, pdfContentCard;
    private TextView documentContent, textSizeIndicator, pdfZoomIndicator, pageIndicator;
    private ImageView pdfImageView;
    private LinearLayout pdfNavigationBar, textControls, pdfControls;
    private AppCompatButton btnTextSizeDecrease, btnTextSizeIncrease;
    private AppCompatButton btnPdfZoomOut, btnPdfZoomIn, btnPrevPage, btnNextPage;
    private ImageButton fabDarkMode;
    private ScrollView pdfVerticalScroll;
    
    // Document state
    private String fileName;
    private Uri fileUri;
    private String documentType; // "pdf", "docx", "txt"
    
    // PDF state
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private int currentPageIndex = 0;
    private int totalPages = 0;
    private Bitmap originalPdfBitmap;
    
    // Zoom and display state
    private float currentTextSize = 14f;
    private float currentPdfZoom = 1.0f;
    private final float MIN_TEXT_SIZE = 8f;
    private final float MAX_TEXT_SIZE = 24f;
    private final float MIN_PDF_ZOOM = 0.5f;
    private final float MAX_PDF_ZOOM = 3.0f;
    private Matrix pdfMatrix = new Matrix();
    private ScaleGestureDetector scaleGestureDetector;
    
    // Dark mode state
    private boolean isDarkMode = false;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_viewer);
        
        initializeViews();
        setupToolbar();
        setupGestureDetector();
        setupClickListeners();
        
        // Get document info from intent
        fileUri = getIntent().getData();
        fileName = getIntent().getStringExtra("fileName");
        
        // Fallback: try to get file path from extras and convert to URI
        if (fileUri == null) {
            String filePath = getIntent().getStringExtra("file_path");
            if (filePath != null) {
                try {
                    File file = new File(filePath);
                    fileUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        file
                    );
                } catch (Exception e) {
                    // If FileProvider fails, use regular file URI
                    File file = new File(filePath);
                    fileUri = Uri.fromFile(file);
                }
            }
        }
        
        if (fileUri == null) {
            showError("Unable to open document: missing file URI");
            return;
        }
        
        // Set toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName != null ? fileName : "Document");
        }
        
        // Determine document type and display
        determineDocumentTypeAndDisplay();
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        nestedScrollView = findViewById(R.id.nestedScrollView);
        textContentCard = findViewById(R.id.textContentCard);
        pdfContentCard = findViewById(R.id.pdfContentCard);
        documentContent = findViewById(R.id.documentContent);
        textSizeIndicator = findViewById(R.id.textSizeIndicator);
        pdfZoomIndicator = findViewById(R.id.pdfZoomIndicator);
        pageIndicator = findViewById(R.id.pageIndicator);
        pdfImageView = findViewById(R.id.pdfImageView);
        pdfNavigationBar = findViewById(R.id.pdfNavigationBar);
        textControls = findViewById(R.id.textControls);
        pdfControls = findViewById(R.id.pdfControls);
        btnTextSizeDecrease = findViewById(R.id.btnTextSizeDecrease);
        btnTextSizeIncrease = findViewById(R.id.btnTextSizeIncrease);
        btnPdfZoomOut = findViewById(R.id.btnPdfZoomOut);
        btnPdfZoomIn = findViewById(R.id.btnPdfZoomIn);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        fabDarkMode = findViewById(R.id.fabDarkMode);
        pdfVerticalScroll = findViewById(R.id.pdfVerticalScroll);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void setupGestureDetector() {
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (documentType != null && documentType.equals("pdf") && originalPdfBitmap != null) {
                    float scaleFactor = detector.getScaleFactor();
                    currentPdfZoom *= scaleFactor;
                    currentPdfZoom = Math.max(MIN_PDF_ZOOM, Math.min(currentPdfZoom, MAX_PDF_ZOOM));
                    applyPdfZoom();
                    return true;
                }
                return false;
            }
        });
    }
    
    private void setupClickListeners() {
        // Text size controls
        btnTextSizeDecrease.setOnClickListener(v -> {
            if (currentTextSize > MIN_TEXT_SIZE) {
                currentTextSize -= 2f;
                applyTextSize();
            }
        });
        
        btnTextSizeIncrease.setOnClickListener(v -> {
            if (currentTextSize < MAX_TEXT_SIZE) {
                currentTextSize += 2f;
                applyTextSize();
            }
        });
        
        // PDF zoom controls
        btnPdfZoomOut.setOnClickListener(v -> {
            if (currentPdfZoom > MIN_PDF_ZOOM) {
                currentPdfZoom -= 0.25f;
                applyPdfZoom();
            }
        });
        
        btnPdfZoomIn.setOnClickListener(v -> {
            if (currentPdfZoom < MAX_PDF_ZOOM) {
                currentPdfZoom += 0.25f;
                applyPdfZoom();
            }
        });
        
        // PDF navigation
        btnPrevPage.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                renderPdfPage();
            }
        });
        
        btnNextPage.setOnClickListener(v -> {
            if (currentPageIndex < totalPages - 1) {
                currentPageIndex++;
                renderPdfPage();
            }
        });
        
        // Dark mode toggle
        fabDarkMode.setOnClickListener(v -> toggleDarkMode());
        
        // PDF touch listener for gestures
        pdfImageView.setOnTouchListener((v, event) -> {
            if (documentType != null && documentType.equals("pdf")) {
                scaleGestureDetector.onTouchEvent(event);
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.document_viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_share) {
            shareDocument();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (documentType != null && documentType.equals("pdf")) {
            scaleGestureDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
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
        hideAllCards();
        pdfContentCard.setVisibility(View.VISIBLE);
        pdfNavigationBar.setVisibility(View.VISIBLE);
        
        try (ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(fileUri, "r")) {
            if (fd == null) {
                showError("Cannot access PDF file");
                return;
            }
            
            pdfRenderer = new PdfRenderer(fd);
            totalPages = pdfRenderer.getPageCount();
            currentPageIndex = 0;
            
            renderPdfPage();
            updatePdfNavigation();
            
        } catch (IOException | SecurityException e) {
            showError("Failed to open PDF: " + e.getMessage());
        } catch (Exception e) {
            showError("Unexpected error opening PDF: " + e.getMessage());
        }
    }

    private void renderPdfPage() {
        if (pdfRenderer == null || currentPageIndex < 0 || currentPageIndex >= totalPages) {
            return;
        }
        
        try {
            if (currentPage != null) {
                currentPage.close();
            }
            
            currentPage = pdfRenderer.openPage(currentPageIndex);
            
            // Create bitmap with appropriate size
            int width = currentPage.getWidth();
            int height = currentPage.getHeight();
            
            // Scale down if too large to avoid memory issues
            if (width > 2048 || height > 2048) {
                float scale = Math.min(2048f / width, 2048f / height);
                width = Math.round(width * scale);
                height = Math.round(height * scale);
            }
            
            originalPdfBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            currentPage.render(originalPdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            
            applyPdfZoom();
            updatePdfNavigation();
            
        } catch (Exception e) {
            showError("Failed to render PDF page: " + e.getMessage());
        }
    }
    
    private void displayTextFile() {
        hideAllCards();
        textContentCard.setVisibility(View.VISIBLE);
        
        try (java.io.InputStream is = getContentResolver().openInputStream(fileUri)) {
            if (is == null) {
                showError("Cannot access text file");
                return;
            }
            
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            int lineCount = 0;
            
            while ((line = br.readLine()) != null && lineCount < 2000) { // Increased limit
                sb.append(line).append("\n");
                lineCount++;
            }
            br.close();
            
            String content = sb.toString();
            if (lineCount >= 2000) {
                content += "\n\n... (File truncated - showing first 2000 lines)";
            }
            
            // Format content with header
            String headerText = String.format("📄 Text Document\n\nFile: %s\nLines: %d\n\n", 
                fileName != null ? fileName : "Unknown", lineCount);
            
            String fullContent = headerText + (content.isEmpty() ? "File appears to be empty" : content);
            
            // Apply monospace font for TXT files
            documentContent.setTypeface(Typeface.MONOSPACE);
            documentContent.setText(fullContent);
            applyTextSize();
            
        } catch (Exception e) {
            showError("Failed to open text file: " + e.getMessage());
        }
    }
    
    private void displayDocxFile() {
        hideAllCards();
        textContentCard.setVisibility(View.VISIBLE);
        
        try {
            String text = com.curosoft.konvert.utils.DocxToTxtConverter.convertDocxToTxt(this, fileUri);
            
            if (text == null || text.trim().isEmpty()) {
                String emptyMessage = String.format("📄 DOCX Document\n\nFile: %s\n\nDocument appears to be empty or contains no readable text", 
                    fileName != null ? fileName : "Unknown");
                documentContent.setText(createFormattedText(emptyMessage));
            } else {
                // Format content with header
                String headerText = String.format("📄 DOCX Document\n\nFile: %s\nCharacters: %d\n\n", 
                    fileName != null ? fileName : "Unknown", text.length());
                
                String fullContent = headerText + text;
                
                // Apply regular font for DOCX files
                documentContent.setTypeface(Typeface.DEFAULT);
                documentContent.setText(createFormattedText(fullContent));
                applyTextSize();
            }
            
        } catch (Exception e) {
            showError("Failed to open DOCX: " + e.getMessage());
        }
    }
    
    private void hideAllCards() {
        textContentCard.setVisibility(View.GONE);
        pdfContentCard.setVisibility(View.GONE);
        pdfNavigationBar.setVisibility(View.GONE);
    }
    
    private void applyTextSize() {
        documentContent.setTextSize(currentTextSize);
        textSizeIndicator.setText(Math.round(currentTextSize) + "sp");
        
        // Update button states
        btnTextSizeDecrease.setEnabled(currentTextSize > MIN_TEXT_SIZE);
        btnTextSizeIncrease.setEnabled(currentTextSize < MAX_TEXT_SIZE);
    }
    
    private void applyPdfZoom() {
        if (originalPdfBitmap != null) {
            pdfMatrix.reset();
            pdfMatrix.setScale(currentPdfZoom, currentPdfZoom);
            pdfImageView.setScaleType(ImageView.ScaleType.MATRIX);
            pdfImageView.setImageMatrix(pdfMatrix);
            pdfImageView.setImageBitmap(originalPdfBitmap);
            
            pdfZoomIndicator.setText(Math.round(currentPdfZoom * 100) + "%");
            
            // Update button states
            btnPdfZoomOut.setEnabled(currentPdfZoom > MIN_PDF_ZOOM);
            btnPdfZoomIn.setEnabled(currentPdfZoom < MAX_PDF_ZOOM);
        }
    }
    
    private void updatePdfNavigation() {
        pageIndicator.setText((currentPageIndex + 1) + " of " + totalPages);
        btnPrevPage.setEnabled(currentPageIndex > 0);
        btnNextPage.setEnabled(currentPageIndex < totalPages - 1);
    }
    
    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Toast.makeText(this, "Dark mode enabled", Toast.LENGTH_SHORT).show();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, "Light mode enabled", Toast.LENGTH_SHORT).show();
        }
        
        // Recreate activity to apply theme
        recreate();
    }
    
    private void shareDocument() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing: " + (fileName != null ? fileName : "Document"));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share document"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share document", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showError(String message) {
        hideAllCards();
        textContentCard.setVisibility(View.VISIBLE);
        documentContent.setText(message);
        documentContent.setTypeface(Typeface.DEFAULT);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private SpannableString createFormattedText(String text) {
        SpannableString spannable = new SpannableString(text);
        
        // Find and style document type header (e.g., "📄 PDF Document")
        int typeStart = text.indexOf("📄");
        if (typeStart != -1) {
            int typeEnd = text.indexOf("\n", typeStart);
            if (typeEnd != -1) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), typeStart, typeEnd, 0);
                spannable.setSpan(new ForegroundColorSpan(0xFF2196F3), typeStart, typeEnd, 0);
            }
        }
        
        // Style labels
        String[] labels = {"File:", "Lines:", "Characters:", "Pages:"};
        for (String label : labels) {
            int labelStart = text.indexOf(label);
            if (labelStart != -1) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), labelStart, labelStart + label.length(), 0);
            }
        }
        
        return spannable;
    }
    
    @Override
    protected void onDestroy() {
        cleanup();
        super.onDestroy();
    }
    
    private void cleanup() {
        try {
            if (currentPage != null) {
                currentPage.close();
                currentPage = null;
            }
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }
            if (originalPdfBitmap != null && !originalPdfBitmap.isRecycled()) {
                originalPdfBitmap.recycle();
                originalPdfBitmap = null;
            }
        } catch (Exception e) {
            // Silent cleanup
        }
    }
    
    // Gesture detector for PDF pinch-to-zoom
    private class PdfScaleGestureDetector extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (originalPdfBitmap != null) {
                float scaleFactor = detector.getScaleFactor();
                currentPdfZoom *= scaleFactor;
                currentPdfZoom = Math.max(MIN_PDF_ZOOM, Math.min(currentPdfZoom, MAX_PDF_ZOOM));
                applyPdfZoom();
            }
            return true;
        }
    }
}
