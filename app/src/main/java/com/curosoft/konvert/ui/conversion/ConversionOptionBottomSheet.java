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
import com.curosoft.konvert.utils.ConversionUtils;
import com.curosoft.konvert.utils.DocxToPdfConverter;
import com.curosoft.konvert.utils.DocxToTxtConverter;
import com.curosoft.konvert.utils.EnhancedDocumentConverter;
import com.curosoft.konvert.utils.EnhancedFilePickerUtils;
import com.curosoft.konvert.utils.EnhancedImageConverter;
import com.curosoft.konvert.utils.ImageConverter;
import com.curosoft.konvert.utils.PdfToDocxConverter;
import com.curosoft.konvert.utils.PdfToTxtConverter;
import com.curosoft.konvert.utils.TxtToDocxConverter;
import com.curosoft.konvert.utils.TxtToPdfConverter;
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
                behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
                behavior.setHideable(true);
                behavior.setDraggable(true);
                
                // Set to expanded state after a brief delay to ensure smooth animation
                bottomSheet.post(() -> behavior.setState(BottomSheetBehavior.STATE_EXPANDED));
                
                // Add smooth animation
                behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        // Handle state changes for smooth animations
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        // Handle slide animations if needed
                    }
                });
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
                if (category.equalsIgnoreCase("images")) {
                    boolean isJpgFile = (selectedMimeType != null && (selectedMimeType.contains("jpg") || selectedMimeType.contains("jpeg")) ||
                            selectedFileName != null && (selectedFileName.toLowerCase().endsWith(".jpg") || selectedFileName.toLowerCase().endsWith(".jpeg")));
                    boolean isPngFile = (selectedMimeType != null && selectedMimeType.contains("png") ||
                            selectedFileName != null && selectedFileName.toLowerCase().endsWith(".png"));
                    boolean isWebpFile = (selectedMimeType != null && selectedMimeType.contains("webp") ||
                            selectedFileName != null && selectedFileName.toLowerCase().endsWith(".webp"));
                    List<String> imageTargets = Arrays.asList("PNG", "WEBP", "JPG");
                    if (isJpgFile && imageTargets.contains(selectedFormat.toUpperCase())) {
                        new EnhancedImageConversionTask(requireContext(), originalFileUri, selectedFormat.toUpperCase()).execute();
                        return;
                    }
                    if (isPngFile && (selectedFormat.equalsIgnoreCase("JPG") || selectedFormat.equalsIgnoreCase("WEBP"))) {
                        new EnhancedImageConversionTask(requireContext(), originalFileUri, selectedFormat.toUpperCase()).execute();
                        return;
                    }
                    if (isWebpFile && (selectedFormat.equalsIgnoreCase("JPG") || selectedFormat.equalsIgnoreCase("PNG"))) {
                        new EnhancedImageConversionTask(requireContext(), originalFileUri, selectedFormat.toUpperCase()).execute();
                        return;
                    }
                }
                // Handle document conversions
                if (category.equalsIgnoreCase("docs")) {
                    // PDF to DOCX conversion
                    if ((selectedMimeType != null && selectedMimeType.contains("pdf") || 
                         selectedFileName.toLowerCase().endsWith(".pdf")) &&
                        selectedFormat.equalsIgnoreCase("DOCX")) {
                        
                        new EnhancedPdfToDocxConversionTask(requireContext(), originalFileUri).execute();
                    } 
                    // DOCX to PDF conversion
                    else if ((selectedMimeType != null && 
                             (selectedMimeType.contains("docx") || 
                              selectedMimeType.contains("wordprocessingml")) || 
                             selectedFileName.toLowerCase().endsWith(".docx")) &&
                            selectedFormat.equalsIgnoreCase("PDF")) {
                        
                        new EnhancedDocxToPdfConversionTask(requireContext(), originalFileUri).execute();
                    }
                    // DOCX to TXT conversion
                    else if ((selectedMimeType != null && 
                             (selectedMimeType.contains("docx") || 
                              selectedMimeType.contains("wordprocessingml")) || 
                             selectedFileName.toLowerCase().endsWith(".docx")) &&
                            selectedFormat.equalsIgnoreCase("TXT")) {
                        
                        performDocxToTxtConversion();
                    }
                    // PDF to TXT conversion
                    else if ((selectedMimeType != null && selectedMimeType.contains("pdf") || 
                             selectedFileName.toLowerCase().endsWith(".pdf")) &&
                            selectedFormat.equalsIgnoreCase("TXT")) {
                        
                        performPdfToTxtConversion();
                    }
                    // TXT to DOCX conversion
                    else if ((selectedMimeType != null && 
                             (selectedMimeType.contains("text/plain") || 
                              selectedMimeType.contains("text/txt")) || 
                             selectedFileName.toLowerCase().endsWith(".txt")) &&
                            selectedFormat.equalsIgnoreCase("DOCX")) {
                        
                        performTxtToDocxConversion();
                    }
                    // TXT to PDF conversion
                    else if ((selectedMimeType != null && 
                             (selectedMimeType.contains("text/plain") || 
                              selectedMimeType.contains("text/txt")) || 
                             selectedFileName.toLowerCase().endsWith(".txt")) &&
                            selectedFormat.equalsIgnoreCase("PDF")) {
                        
                        new EnhancedTextToPdfConversionTask(requireContext(), originalFileUri).execute();
                    } else {
                        Log.w("ConversionBottomSheet", "Unsupported conversion type selected");
                        Toast.makeText(requireContext(), 
                                "This conversion is not supported yet", 
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
    
    private void performDocxToTxtConversion() {
        new DocxToTxtConversionTask(requireContext(), originalFileUri).execute();
    }
    
    private void performPdfToTxtConversion() {
        new PdfToTxtConversionTask(requireContext(), originalFileUri).execute();
    }
    
    private void performTxtToDocxConversion() {
        new TxtToDocxConversionTask(requireContext(), originalFileUri).execute();
    }
    
    private void performTxtToPdfConversion() {
        new TxtToPdfConversionTask(requireContext(), originalFileUri).execute();
    }
    
    private void updateFileNameDisplay(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            fileNameText.setText(fileName);
            fileNameText.setVisibility(View.VISIBLE);
            btnSelectFile.setText(R.string.change_file);
            
            // Update the file display card to show selected state
            View fileDisplayCard = getView().findViewById(R.id.file_selection_card);
            if (fileDisplayCard != null) {
                fileDisplayCard.setBackground(getResources().getDrawable(R.drawable.bg_file_display));
            }
        } else {
            fileNameText.setVisibility(View.GONE);
            btnSelectFile.setText(R.string.select_file);
            
            // Reset the file display card to default state
            View fileDisplayCard = getView().findViewById(R.id.file_selection_card);
            if (fileDisplayCard != null) {
                fileDisplayCard.setBackground(getResources().getDrawable(R.drawable.bg_file_display));
            }
        }
    }
    
    private void updateProceedButtonState() {
        if (selectedFile == null || selectedFormat == null) {
            btnProceed.setEnabled(false);
            return;
        }

        if (category.equalsIgnoreCase("docs")) {
            boolean isDocxFile = (selectedMimeType != null &&
                (selectedMimeType.contains("docx") || selectedMimeType.contains("wordprocessingml")) ||
                selectedFileName != null && selectedFileName.toLowerCase().endsWith(".docx"));
            boolean isPdfFile = (selectedMimeType != null && selectedMimeType.contains("pdf") ||
                selectedFileName != null && selectedFileName.toLowerCase().endsWith(".pdf"));
            boolean isTxtFile = (selectedMimeType != null &&
                (selectedMimeType.contains("text/plain") || selectedMimeType.contains("text/txt")) ||
                selectedFileName != null && selectedFileName.toLowerCase().endsWith(".txt"));
            boolean isPdfToDocx = isPdfFile && selectedFormat.equalsIgnoreCase("DOCX");
            boolean isPdfToTxt = isPdfFile && selectedFormat.equalsIgnoreCase("TXT");
            boolean isDocxToPdf = isDocxFile && selectedFormat.equalsIgnoreCase("PDF");
            boolean isDocxToTxt = isDocxFile && selectedFormat.equalsIgnoreCase("TXT");
            boolean isTxtToDocx = isTxtFile && selectedFormat.equalsIgnoreCase("DOCX");
            boolean isTxtToPdf = isTxtFile && selectedFormat.equalsIgnoreCase("PDF");
            btnProceed.setEnabled(isPdfToDocx || isPdfToTxt ||
                isDocxToPdf || isDocxToTxt ||
                isTxtToDocx || isTxtToPdf);
            return;
        }

        if (category.equalsIgnoreCase("images")) {
            boolean isJpgFile = (selectedMimeType != null && (selectedMimeType.contains("jpg") || selectedMimeType.contains("jpeg")) ||
                selectedFileName != null && (selectedFileName.toLowerCase().endsWith(".jpg") || selectedFileName.toLowerCase().endsWith(".jpeg")));
            boolean isPngFile = (selectedMimeType != null && selectedMimeType.contains("png") ||
                selectedFileName != null && selectedFileName.toLowerCase().endsWith(".png"));
            boolean isWebpFile = (selectedMimeType != null && selectedMimeType.contains("webp") ||
                selectedFileName != null && selectedFileName.toLowerCase().endsWith(".webp"));
            List<String> imageTargets = Arrays.asList("PNG", "WEBP", "JPG");
            boolean isSupportedJpg = isJpgFile && imageTargets.contains(selectedFormat.toUpperCase());
            boolean isSupportedPng = isPngFile && (selectedFormat.equalsIgnoreCase("JPG") || selectedFormat.equalsIgnoreCase("WEBP"));
            boolean isSupportedWebp = isWebpFile && (selectedFormat.equalsIgnoreCase("JPG") || selectedFormat.equalsIgnoreCase("PNG"));
            btnProceed.setEnabled(isSupportedJpg || isSupportedPng || isSupportedWebp);
            return;
        }

        btnProceed.setEnabled(false);
    }
    
    private List<String> getFormatsForCategory(String category) {
        List<String> formats = new ArrayList<>();
        switch (category.toLowerCase()) {
            case "docs":
                for (String ext : EnhancedFilePickerUtils.SupportedFileTypes.DOCS) {
                    formats.add(ext.toUpperCase());
                }
                break;
            case "images":
                // Only show valid output formats for each input type in UI logic (handled above)
                formats.add("PNG");
                formats.add("WEBP");
                formats.add("JPG");
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
                // Track the successful conversion
                ConversionUtils.trackConversion(context, outputPath, "PDF", "DOCX");
                
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
                // Track the successful conversion
                ConversionUtils.trackConversion(context, outputPath, "DOCX", "PDF");
                
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
     * AsyncTask to perform the DOCX to TXT conversion in the background
     */
    private class DocxToTxtConversionTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private Uri docxUri;
        private ProgressDialog progressDialog;
        
        public DocxToTxtConversionTask(Context context, Uri docxUri) {
            this.context = context;
            this.docxUri = docxUri;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting DOCX to TXT...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                return DocxToTxtConverter.convertDocxToTxt(context, docxUri);
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
                // Track the successful conversion
                ConversionUtils.trackConversion(context, outputPath, "DOCX", "TXT");
                
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
     * AsyncTask to perform the PDF to TXT conversion in the background
     */
    private class PdfToTxtConversionTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private Uri pdfUri;
        private ProgressDialog progressDialog;
        
        public PdfToTxtConversionTask(Context context, Uri pdfUri) {
            this.context = context;
            this.pdfUri = pdfUri;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting PDF to TXT...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                return PdfToTxtConverter.convertPdfToTxt(context, pdfUri);
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
                // Track the successful conversion
                ConversionUtils.trackConversion(context, outputPath, "PDF", "TXT");
                
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
     * AsyncTask to perform the TXT to DOCX conversion in the background
     */
    private class TxtToDocxConversionTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private Uri txtUri;
        private ProgressDialog progressDialog;
        
        public TxtToDocxConversionTask(Context context, Uri txtUri) {
            this.context = context;
            this.txtUri = txtUri;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting TXT to DOCX...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                return TxtToDocxConverter.convertTxtToDocx(context, txtUri);
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
                // Track the successful conversion
                ConversionUtils.trackConversion(context, outputPath, "TXT", "DOCX");
                
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
     * AsyncTask to perform the TXT to PDF conversion in the background
     */
    private class TxtToPdfConversionTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private Uri txtUri;
        private ProgressDialog progressDialog;
        
        public TxtToPdfConversionTask(Context context, Uri txtUri) {
            this.context = context;
            this.txtUri = txtUri;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting TXT to PDF...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                return TxtToPdfConverter.convertTxtToPdf(context, txtUri);
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
                // Track the successful conversion
                ConversionUtils.trackConversion(context, outputPath, "TXT", "PDF");
                
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
     * AsyncTask to perform the TXT to RTF conversion in the background
     */

    /**
     * AsyncTask for JPG image conversion
     */
    private class JpgImageConversionTask extends AsyncTask<Void, Void, Boolean> {
        private Context context;
        private Uri inputUri;
        private String targetFormat;
        private ProgressDialog progressDialog;
        
        public JpgImageConversionTask(Context context, Uri inputUri, String targetFormat) {
            this.context = context;
            this.inputUri = inputUri;
            this.targetFormat = targetFormat;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting JPG to " + targetFormat + "...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            return ImageConverter.convertImage(context, inputUri, targetFormat);
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            if (result) {
                // Track the successful conversion (using generic path since ImageConverter doesn't return specific path)
                String fileName = "converted_image." + targetFormat.toLowerCase();
                String outputPath = "/storage/emulated/0/Documents/Konvert/Converted/" + fileName;
                ConversionUtils.trackConversion(context, outputPath, "JPG", targetFormat.toUpperCase());
                
                Toast.makeText(context, "Conversion successful! Saved to Documents/Konvert/Converted/Images/", Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Toast.makeText(context, "Conversion failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * AsyncTask for PNG image conversion
     */
    private class PngImageConversionTask extends AsyncTask<Void, Void, Boolean> {
        private Context context;
        private Uri inputUri;
        private String targetFormat;
        private ProgressDialog progressDialog;
        
        public PngImageConversionTask(Context context, Uri inputUri, String targetFormat) {
            this.context = context;
            this.inputUri = inputUri;
            this.targetFormat = targetFormat;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting PNG to " + targetFormat + "...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            return ImageConverter.convertImage(context, inputUri, targetFormat);
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            // Success/failure toast is handled in ImageConverter
            if (result) {
                // Track the successful conversion (using generic path since ImageConverter doesn't return specific path)
                String fileName = "converted_image." + targetFormat.toLowerCase();
                String outputPath = "/storage/emulated/0/Documents/Konvert/Converted/" + fileName;
                ConversionUtils.trackConversion(context, outputPath, "PNG", targetFormat.toUpperCase());
                
                Toast.makeText(context, "Conversion successful! Saved to Documents/Konvert/Converted/", Toast.LENGTH_LONG).show();
                dismiss();
            }
        }
    }
    
    /**
     * AsyncTask for WEBP image conversion
     */
    private class WebpImageConversionTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private Uri inputUri;
        private String targetFormat;
        private ProgressDialog progressDialog;
        
        public WebpImageConversionTask(Context context, Uri inputUri, String targetFormat) {
            this.context = context;
            this.inputUri = inputUri;
            this.targetFormat = targetFormat;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting WEBP to " + targetFormat.toUpperCase() + "...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            return com.curosoft.konvert.utils.WebpConverter.convertWebpToJpgOrPng(context, inputUri, targetFormat);
        }
        
        @Override
        protected void onPostExecute(String outputPath) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            if (outputPath != null) {
                // Track the successful conversion
                ConversionUtils.trackConversion(context, outputPath, "WEBP", targetFormat.toUpperCase());
                
                Toast.makeText(context, "Conversion successful! Saved to Documents/Konvert/Converted/", Toast.LENGTH_LONG).show();
                dismiss();
            }
        }
    }
    
    /**
     * Enhanced AsyncTask for image conversion with EXIF preservation
     */
    private class EnhancedImageConversionTask extends AsyncTask<Void, Void, EnhancedImageConverter.ConversionResult> {
        private final Context context;
        private final Uri inputUri;
        private final String targetFormat;
        private ProgressDialog progressDialog;
        
        public EnhancedImageConversionTask(Context context, Uri inputUri, String targetFormat) {
            this.context = context;
            this.inputUri = inputUri;
            this.targetFormat = targetFormat;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting image to " + targetFormat + "...");
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected EnhancedImageConverter.ConversionResult doInBackground(Void... voids) {
            return EnhancedImageConverter.convertImage(context, inputUri, targetFormat);
        }
        
        @Override
        protected void onPostExecute(EnhancedImageConverter.ConversionResult result) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            
            if (result.success) {
                Toast.makeText(context, 
                    "High-quality conversion successful! Saved to: " + result.outputPath, 
                    Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Toast.makeText(context, 
                    "Conversion failed: " + (result.errorMessage != null ? result.errorMessage : "Unknown error"), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Enhanced AsyncTask for PDF to DOCX conversion with formatting preservation
     */
    private class EnhancedPdfToDocxConversionTask extends AsyncTask<Void, Void, EnhancedDocumentConverter.ConversionResult> {
        private final Context context;
        private final Uri pdfUri;
        private ProgressDialog progressDialog;
        
        public EnhancedPdfToDocxConversionTask(Context context, Uri pdfUri) {
            this.context = context;
            this.pdfUri = pdfUri;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting PDF to DOCX with formatting preservation...");
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected EnhancedDocumentConverter.ConversionResult doInBackground(Void... voids) {
            return EnhancedDocumentConverter.convertPdfToDocxWithFormatting(context, pdfUri);
        }
        
        @Override
        protected void onPostExecute(EnhancedDocumentConverter.ConversionResult result) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            
            if (result.success) {
                Toast.makeText(context, 
                    "Document conversion successful with preserved formatting! Saved to: " + result.outputPath, 
                    Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Toast.makeText(context, 
                    "Conversion failed: " + (result.errorMessage != null ? result.errorMessage : "Unknown error"), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Enhanced AsyncTask for DOCX to PDF conversion with formatting preservation
     */
    private class EnhancedDocxToPdfConversionTask extends AsyncTask<Void, Void, EnhancedDocumentConverter.ConversionResult> {
        private final Context context;
        private final Uri docxUri;
        private ProgressDialog progressDialog;
        
        public EnhancedDocxToPdfConversionTask(Context context, Uri docxUri) {
            this.context = context;
            this.docxUri = docxUri;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting DOCX to PDF with formatting preservation...");
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected EnhancedDocumentConverter.ConversionResult doInBackground(Void... voids) {
            return EnhancedDocumentConverter.convertDocxToPdfWithFormatting(context, docxUri);
        }
        
        @Override
        protected void onPostExecute(EnhancedDocumentConverter.ConversionResult result) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            
            if (result.success) {
                Toast.makeText(context, 
                    "Document conversion successful with preserved formatting! Saved to: " + result.outputPath, 
                    Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Toast.makeText(context, 
                    "Conversion failed: " + (result.errorMessage != null ? result.errorMessage : "Unknown error"), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Enhanced AsyncTask for Text to PDF conversion with professional layout
     */
    private class EnhancedTextToPdfConversionTask extends AsyncTask<Void, Void, EnhancedDocumentConverter.ConversionResult> {
        private final Context context;
        private final Uri textUri;
        private ProgressDialog progressDialog;
        
        public EnhancedTextToPdfConversionTask(Context context, Uri textUri) {
            this.context = context;
            this.textUri = textUri;
        }
        
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Converting text to professional PDF...");
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        
        @Override
        protected EnhancedDocumentConverter.ConversionResult doInBackground(Void... voids) {
            return EnhancedDocumentConverter.convertTextToPdfProfessional(context, textUri);
        }
        
        @Override
        protected void onPostExecute(EnhancedDocumentConverter.ConversionResult result) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            
            if (result.success) {
                Toast.makeText(context, 
                    "Professional PDF created successfully! Saved to: " + result.outputPath, 
                    Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Toast.makeText(context, 
                    "Conversion failed: " + (result.errorMessage != null ? result.errorMessage : "Unknown error"), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
}
