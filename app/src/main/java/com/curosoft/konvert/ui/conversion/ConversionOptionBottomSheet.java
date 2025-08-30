package com.curosoft.konvert.ui.conversion;

import android.app.Dialog;
import android.os.Bundle;
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
            public void onFileSelected(File file, String originalName, String mimeType) {
                selectedFile = file;
                selectedFileName = originalName;
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
            filePicker.launch(mimeType);
        });
        
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnProceed.setOnClickListener(v -> {
            if (selectedFile != null && selectedFormat != null) {
                // In a real app, we would start the conversion process here
                String message = String.format("Converting %s to %s format", 
                        selectedFileName, selectedFormat);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
        
        // Listen for format selection
        formatAdapter.setOnFormatSelectedListener(format -> {
            selectedFormat = format;
            updateProceedButtonState();
        });
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
        btnProceed.setEnabled(selectedFile != null && selectedFormat != null);
    }
    
    private List<String> getFormatsForCategory(String category) {
        switch (category.toLowerCase()) {
            case "docs":
                return Arrays.asList("DOCX", "PDF", "TXT", "RTF", "ODT", "EPUB", "MOBI");
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
}
