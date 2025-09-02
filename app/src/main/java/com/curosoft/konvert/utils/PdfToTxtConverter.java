package com.curosoft.konvert.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Utility class for converting PDF files to TXT format
 */
public class PdfToTxtConverter {
    private static final String TAG = "PdfToTxtConverter";
    
    /**
     * Convert a PDF file to TXT format
     *
     * @param context The context
     * @param pdfUri  The URI of the PDF file
     * @return The path to the converted TXT file, or null if conversion failed
     */
    public static String convertPdfToTxt(Context context, Uri pdfUri) {
        Log.d(TAG, "Starting PDF to TXT conversion");
        
        try {
            // Get the PDF file name
            String pdfFileName = EnhancedFilePickerUtils.getFileName(context, pdfUri);
            Log.d(TAG, "Converting PDF: " + pdfFileName);
            
            // Create output file name based on the input name
            String txtFileName = getOutputFileName(pdfFileName);
            
            // Extract text from PDF
            String extractedText = extractTextFromPdf(context, pdfUri);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                Log.e(TAG, "Failed to extract text from PDF");
                return null;
            }
            
            // Save the extracted text to a TXT file
            File outputFile = saveTxtFile(context, extractedText, txtFileName);
            if (outputFile != null) {
                Log.d(TAG, "Conversion successful. Output file: " + outputFile.getAbsolutePath());
                
                // Make the file visible in the MediaStore
                addToMediaStore(context, outputFile, "text/plain");
                
                return outputFile.getAbsolutePath();
            } else {
                Log.e(TAG, "Failed to save TXT file");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting PDF to TXT", e);
            return null;
        }
    }
    
    /**
     * Extract text from a PDF file using Android's PdfRenderer
     *
     * @param context The context
     * @param pdfUri  The URI of the PDF file
     * @return The extracted text
     * @throws IOException if there's an error accessing the PDF
     */
    private static String extractTextFromPdf(Context context, Uri pdfUri) throws IOException {
        // For Android's PdfRenderer, we need to create a temporary file
        File tempFile = createTempFileFromUri(context, pdfUri);
        if (tempFile == null) {
            return null;
        }
        
        StringBuilder textBuilder = new StringBuilder();
        
        try {
            // Open the PDF file
            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(
                    tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(fileDescriptor);
            
            final int pageCount = renderer.getPageCount();
            Log.d(TAG, "PDF has " + pageCount + " pages");
            
            // We can't extract text directly with PdfRenderer, but we can use a third-party
            // library for actual text extraction. For demonstration, we'll use a placeholder.
            textBuilder.append("PDF Document: ").append(EnhancedFilePickerUtils.getFileName(context, pdfUri))
                      .append("\n\nNumber of pages: ").append(pageCount).append("\n\n");
            
            // Use a third-party library like pdfbox-android for actual text extraction
            // For this example, we'll use a basic extraction method to demonstrate the flow
            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                
                // For a real app, replace this with actual text extraction
                // This is just a placeholder
                Bitmap bitmap = Bitmap.createBitmap(
                        page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                
                textBuilder.append("Content from page ").append(i + 1).append(":\n");
                
                // In a real app, you would extract text from the page here
                // using a library like pdfbox-android
                textBuilder.append("[Text content would be extracted here in a real app]\n\n");
                
                bitmap.recycle();
                page.close();
            }
            
            renderer.close();
            fileDescriptor.close();
        } finally {
            // Clean up the temporary file
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
        
        return textBuilder.toString();
    }
    
    /**
     * Create a temporary file from a URI
     *
     * @param context The context
     * @param uri     The URI of the file
     * @return A temporary File object
     * @throws IOException if there's an error creating the file
     */
    private static File createTempFileFromUri(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        
        // Create a temporary file
        File tempFile = File.createTempFile("pdf_temp", ".pdf", context.getCacheDir());
        
        // Copy the content from the URI to the temporary file
        try (OutputStream os = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            try (java.io.InputStream is = resolver.openInputStream(uri)) {
                if (is == null) {
                    throw new IOException("Could not open input stream from URI");
                }
                
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }
        
        return tempFile;
    }
    
    /**
     * Save extracted text to a TXT file
     *
     * @param context The context
     * @param text    The text to save
     * @param fileName The name of the output file
     * @return The created File object, or null if creation failed
     */
    private static File saveTxtFile(Context context, String text, String fileName) {
        try {
            // Get output directory using FileStorageUtils
            File outputDir = FileStorageUtils.getOutputDirectory(context);
            File outputFile = new File(outputDir, fileName);
            
            // Create parent directories if needed
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            
            // Write the text to the file
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile))) {
                writer.write(text);
            }
            
            return outputFile;
        } catch (IOException e) {
            Log.e(TAG, "Error saving TXT file", e);
            return null;
        }
    }
    
    /**
     * Generate an output file name based on the input file name
     *
     * @param inputFileName The input file name
     * @return The output file name
     */
    private static String getOutputFileName(String inputFileName) {
        String baseName = inputFileName;
        
        // Remove the .pdf extension if present
        if (baseName.toLowerCase().endsWith(".pdf")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        
        return baseName + ".txt";
    }
    
    /**
     * Add the file to the MediaStore so it's visible in file browsers
     *
     * @param context  The context
     * @param file     The file to add
     * @param mimeType The MIME type of the file
     */
    private static void addToMediaStore(Context context, File file, String mimeType) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+ (API 29+), use relative path and is_pending flag
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Konvert/Converted");
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                
                // On Android 10+, we should use the MediaStore API to make files visible
                ContentResolver resolver = context.getContentResolver();
                Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);
                
                if (uri != null) {
                    // If we're using app-specific storage, copy the file to the MediaStore
                    if (file.getAbsolutePath().contains(context.getExternalFilesDir(null).getAbsolutePath())) {
                        try (OutputStream os = resolver.openOutputStream(uri);
                             FileInputStream fis = new FileInputStream(file)) {
                            
                            if (os != null) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = fis.read(buffer)) != -1) {
                                    os.write(buffer, 0, bytesRead);
                                }
                                os.flush();
                            }
                        }
                    }
                    
                    Log.d(TAG, "Added file to MediaStore: " + uri);
                } else {
                    Log.w(TAG, "Failed to add file to MediaStore");
                }
            } else {
                // For older Android versions, use DATA field with absolute path
                values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
                
                ContentResolver resolver = context.getContentResolver();
                Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);
                
                if (uri != null) {
                    Log.d(TAG, "Added file to MediaStore: " + uri);
                } else {
                    Log.w(TAG, "Failed to add file to MediaStore");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding file to MediaStore", e);
            // Don't throw the exception, just log it
            // The conversion is still successful even if the file isn't added to MediaStore
        }
    }
}
