package com.curosoft.konvert.ui.conversion;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;
import com.curosoft.konvert.utils.DocxToPdfConverter;
import com.curosoft.konvert.utils.EnhancedFilePickerUtils;
import com.curosoft.konvert.utils.PdfToDocxConverter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConversionOptionBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CATEGORY = "category";
    
    private String category;
    private Button btnSelectFile;
    private Button btnCancel;
    private Button btnProceed;
    private RecyclerView rvFormats;
    private FormatAdapter formatAdapter;
    private TextView titleText;
    private TextView fileNameText;
    
    private ActivityResultLauncher<String[]> filePicker;
    private File selectedFile;
    private String selectedFormat;
    private String selectedFileName;
    private String selectedMimeType;
    private Uri originalFileUri;
    
    public static ConversionOptionBottomSheet newInstance(String category) {
        ConversionOptionBottomSheet fragment = new ConversionOptionBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogStyle);
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
        }
        
        // Register file picker
        filePicker = EnhancedFilePickerUtils.registerFilePicker(this, category, new EnhancedFilePickerUtils.FileSelectionCallback() {
            @Override
            public void onFileSelected(File file, String originalName, String mimeType, Uri uri) {
                selectedFile = file;
                selectedFileName = originalName;
                selectedMimeType = mimeType;
                // Store the original Uri that was passed to the FilePickerUtils
                originalFileUri = uri;
                
                updateFileNameDisplay(originalName);
                updateProceedButtonState();
            }

            @Override
            public void onFileSelectionCancelled() {
                Toast.makeText(requireContext(), "File selection cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFileSelectionError(Exception e) {
                Toast.makeText(requireContext(), "Error selecting file: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_conversion, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        titleText = view.findViewById(R.id.title_text);
        fileNameText = view.findViewById(R.id.file_name_text);
        btnSelectFile = view.findViewById(R.id.btn_select_file);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnProceed = view.findViewById(R.id.btn_proceed);
        rvFormats = view.findViewById(R.id.rv_formats);
        
        // Set up title with category name
        titleText.setText(String.format("Convert %s", category));
        
        // Initially hide the file name text and disable proceed button
        fileNameText.setVisibility(View.GONE);
        btnProceed.setEnabled(false);
        
        // Set up RecyclerView
        rvFormats.setLayoutManager(new LinearLayoutManager(requireContext()));
        formatAdapter = new FormatAdapter(getFormatsForCategory(category));
        rvFormats.setAdapter(formatAdapter);
        
        // Check and request permissions
        EnhancedFilePickerUtils.checkAndRequestPermissions(requireActivity());
        
        // Set up click listeners
        btnSelectFile.setOnClickListener(v -> {
            // Launch file picker with appropriate MIME types
            String[] mimeTypes = EnhancedFilePickerUtils.SupportedFileTypes.getMimeTypesForCategory(category);
            filePicker.launch(mimeTypes);
        });
        
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnProceed.setOnClickListener(v -> {
            if (selectedFile != null && selectedFormat != null) {
                // Handle PDF to DOCX and DOCX to PDF conversions
                if (category.equalsIgnoreCase("docs")) {
                    // PDF to DOCX conversion
                    if ((selectedMimeType != null && selectedMimeType.contains("pdf") || 
                         selectedFileName.toLowerCase().endsWith(".pdf")) &&
                        selectedFormat.equalsIgnoreCase("DOCX")) {
                        
                        performPdfToDocxConversion();
                    } 
                    // DOCX to PDF conversion
                    else if ((selectedMimeType != null && 
                             (selectedMimeType.contains("docx") || 
                              selectedMimeType.contains("wordprocessingml")) || 
                             selectedFileName.toLowerCase().endsWith(".docx")) &&
                            selectedFormat.equalsIgnoreCase("PDF")) {
                        
                        performDocxToPdfConversion();
                    } else {
                        Toast.makeText(requireContext(), 
                                "Only PDF to DOCX and DOCX to PDF conversions are currently supported", 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), 
                            "Only document conversions are currently supported", 
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Listen for format selection
        formatAdapter.setOnFormatSelectedListener(format -> {
            selectedFormat = format;
            updateProceedButtonState();
        });
    }
    
    private void performPdfToDocxConversion() {
        new PdfToDocxConversionTask(requireContext(), originalFileUri).execute();
    }
    
    private void performDocxToPdfConversion() {
        new DocxToPdfConversionTask(requireContext(), originalFileUri).execute();
    }
    
    private void updateFileNameDisplay(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            fileNameText.setText(fileName);
            fileNameText.setVisibility(View.VISIBLE);
            btnSelectFile.setText(R.string.change_file);
        } else {
            fileNameText.setVisibility(View.GONE);
            btnSelectFile.setText(R.string.select_file);
        }
    }
    
    private void updateProceedButtonState() {
        // Check if it's a PDF file and the selected format is DOCX
        boolean isPdfToDocx = category.equalsIgnoreCase("docs") && 
                              selectedFile != null &&
                              (selectedMimeType != null && selectedMimeType.contains("pdf") || 
                               selectedFileName != null && selectedFileName.toLowerCase().endsWith(".pdf")) &&
                              selectedFormat != null &&
                              selectedFormat.equalsIgnoreCase("DOCX");
        
        // Check if it's a DOCX file and the selected format is PDF
        boolean isDocxToPdf = category.equalsIgnoreCase("docs") && 
                              selectedFile != null &&
                              (selectedMimeType != null && 
                               (selectedMimeType.contains("docx") || selectedMimeType.contains("wordprocessingml")) || 
                               selectedFileName != null && selectedFileName.toLowerCase().endsWith(".docx")) &&
                              selectedFormat != null &&
                              selectedFormat.equalsIgnoreCase("PDF");
        
        btnProceed.setEnabled(selectedFile != null && selectedFormat != null && (isPdfToDocx || isDocxToPdf));
    }
    
    private List<String> getFormatsForCategory(String category) {
        // Use the formats defined in EnhancedFilePickerUtils.SupportedFileTypes
        List<String> formats = new ArrayList<>();
        
        switch (category.toLowerCase()) {
            case "docs":
                // Convert all extensions to uppercase for display
                for (String ext : EnhancedFilePickerUtils.SupportedFileTypes.DOCS) {
                    formats.add(ext.toUpperCase());
                }
                break;
            case "images":
                for (String ext : EnhancedFilePickerUtils.SupportedFileTypes.IMAGES) {
                    formats.add(ext.toUpperCase());
                }
                break;
            case "audio":
                for (String ext : EnhancedFilePickerUtils.SupportedFileTypes.AUDIO) {
                    formats.add(ext.toUpperCase());
                }
                break;
            case "video":
                for (String ext : EnhancedFilePickerUtils.SupportedFileTypes.VIDEO) {
                    formats.add(ext.toUpperCase());
                }
                break;
            case "archives":
                for (String ext : EnhancedFilePickerUtils.SupportedFileTypes.ARCHIVES) {
                    formats.add(ext.toUpperCase());
                }
                break;
            default:
                return new ArrayList<>();
        }
        
        return formats;
    }
    
    /**
     * AsyncTask to perform the PDF to DOCX conversion in the background
     */
    private class PdfToDocxConversionTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private Uri pdfUri;
        private ProgressDialog progressDialog;
        
        public PdfToDocxConversionTask(Context context, Uri pdfUri) {
            this.context = context;
            this.pdfUri = pdfUri;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting PDF to DOCX...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                return PdfToDocxConverter.convertPdfToDocx(context, pdfUri);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String outputPath) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            
            if (outputPath != null) {
                Toast.makeText(context, 
                        "Conversion successful! File saved to:\n" + outputPath, 
                        Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Toast.makeText(context, 
                        "Conversion failed. Please try again.", 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * AsyncTask to perform the DOCX to PDF conversion in the background
     */
    private class DocxToPdfConversionTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private Uri docxUri;
        private ProgressDialog progressDialog;
        
        public DocxToPdfConversionTask(Context context, Uri docxUri) {
            this.context = context;
            this.docxUri = docxUri;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting DOCX to PDF...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                return DocxToPdfConverter.convertDocxToPdf(context, docxUri);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String outputPath) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            
            if (outputPath != null) {
                Toast.makeText(context, 
                        "Conversion successful! File saved to:\n" + outputPath, 
                        Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Toast.makeText(context, 
                        "Conversion failed. Please try again.", 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
