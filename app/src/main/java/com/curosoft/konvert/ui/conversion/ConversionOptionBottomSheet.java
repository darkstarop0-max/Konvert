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
import com.curosoft.konvert.utils.FilePickerUtils;
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
    
    private ActivityResultLauncher<String> filePicker;
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
        filePicker = FilePickerUtils.registerFilePicker(this, category, new FilePickerUtils.FileSelectionCallback() {
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
        FilePickerUtils.checkAndRequestPermissions(requireActivity());
        
        // Set up click listeners
        btnSelectFile.setOnClickListener(v -> {
            String mimeType = FilePickerUtils.getMimeTypeForCategory(category);
            
            // For PDF to DOCX conversion specifically
            if (category.equalsIgnoreCase("docs")) {
                mimeType = "application/pdf";
            }
            
            filePicker.launch(mimeType);
        });
        
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnProceed.setOnClickListener(v -> {
            if (selectedFile != null && selectedFormat != null) {
                // Only handle PDF to DOCX for now as per requirements
                if (category.equalsIgnoreCase("docs") && 
                    selectedMimeType != null && 
                    (selectedMimeType.contains("pdf") || selectedFileName.toLowerCase().endsWith(".pdf")) &&
                    selectedFormat.equalsIgnoreCase("DOCX")) {
                    
                    performPdfToDocxConversion();
                } else {
                    Toast.makeText(requireContext(), 
                            "Only PDF to DOCX conversion is currently supported", 
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
        new ConversionTask(requireContext(), originalFileUri).execute();
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
        
        btnProceed.setEnabled(selectedFile != null && selectedFormat != null && isPdfToDocx);
    }
    
    private List<String> getFormatsForCategory(String category) {
        // For the docs category, only enable PDF to DOCX conversion as per requirements
        if (category.equalsIgnoreCase("docs")) {
            return Arrays.asList("DOCX");
        }
        
        switch (category.toLowerCase()) {
            case "images":
                return Arrays.asList("JPG", "PNG", "WEBP", "HEIC", "BMP", "GIF");
            case "audio":
                return Arrays.asList("MP3", "WAV", "AAC", "OGG", "M4A");
            case "video":
                return Arrays.asList("MP4", "MOV", "MKV", "AVI", "WebM");
            case "archives":
                return Arrays.asList("ZIP", "RAR", "7Z", "TAR.GZ");
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * AsyncTask to perform the PDF to DOCX conversion in the background
     */
    private class ConversionTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private Uri pdfUri;
        private ProgressDialog progressDialog;
        
        public ConversionTask(Context context, Uri pdfUri) {
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
}
