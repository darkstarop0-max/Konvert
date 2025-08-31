package com.curosoft.konvert.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for converting PDF files to DOCX format using iText7 and Apache POI
 */
public class PdfToDocxConverter {
    private static final String TAG = "PdfToDocxConverter";

    /**
     * Convert a PDF file to DOCX format
     *
     * @param context The context
     * @param pdfUri  The URI of the PDF file
     * @return The path to the converted DOCX file, or null if conversion failed
     */
    public static String convertPdfToDocx(Context context, Uri pdfUri) {
        try {
            // Get the PDF file name
            String pdfFileName = getFileName(context, pdfUri);
            Log.d(TAG, "Converting PDF: " + pdfFileName);
            
            // Create output file name based on the input name
            String docxFileName = getOutputFileName(pdfFileName);
            
            // Extract text from PDF using iText7
            String pdfText = extractTextFromPdf(context, pdfUri);
            if (pdfText == null || pdfText.trim().isEmpty()) {
                Log.e(TAG, "Failed to extract text from PDF");
                return null;
            }
            
            // Create DOCX file
            String outputPath = createDocxFile(context, pdfText, docxFileName);
            Log.d(TAG, "Conversion successful. Output file: " + outputPath);
            
            return outputPath;
        } catch (Exception e) {
            Log.e(TAG, "Error converting PDF to DOCX", e);
            return null;
        }
    }
    
    /**
     * Extract text from a PDF file using iText7
     *
     * @param context The context
     * @param pdfUri  The URI of the PDF file
     * @return The extracted text
     * @throws IOException if there's an error accessing the PDF
     */
    private static String extractTextFromPdf(Context context, Uri pdfUri) throws IOException {
        PdfDocument pdfDoc = null;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(pdfUri);
            if (inputStream == null) {
                throw new IOException("Could not open PDF file: input stream is null");
            }
            
            PdfReader reader = new PdfReader(inputStream);
            pdfDoc = new PdfDocument(reader);
            
            StringBuilder textBuilder = new StringBuilder();
            int numberOfPages = pdfDoc.getNumberOfPages();
            
            // Extract text from each page
            for (int i = 1; i <= numberOfPages; i++) {
                LocationTextExtractionStrategy strategy = new LocationTextExtractionStrategy();
                String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i), strategy);
                textBuilder.append(pageText).append("\n\n"); // Add extra line breaks between pages
            }
            
            return textBuilder.toString();
        } finally {
            if (pdfDoc != null) {
                try {
                    pdfDoc.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing PDF document", e);
                    // Don't rethrow as we're in finally block
                }
            }
        }
    }
    
    /**
     * Create a DOCX file with the extracted text
     *
     * @param context     The context
     * @param text        The text to write to the DOCX file
     * @param fileName    The name of the output file
     * @return The path to the created DOCX file
     * @throws IOException if there's an error creating the DOCX file
     */
    private static String createDocxFile(Context context, String text, String fileName) throws IOException {
        XWPFDocument document = new XWPFDocument();
        
        // Split text by lines and create paragraphs
        String[] paragraphs = text.split("\\r?\\n");
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                XWPFParagraph p = document.createParagraph();
                p.setAlignment(ParagraphAlignment.LEFT);
                
                XWPFRun run = p.createRun();
                run.setText(paragraph);
                run.setFontFamily("Calibri");
                run.setFontSize(11);
            }
        }
        
        // Save the document
        String filePath;
        OutputStream outputStream = null;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, use MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Konvert");
                
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    outputStream = context.getContentResolver().openOutputStream(uri);
                    document.write(outputStream);
                    filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() 
                             + "/Konvert/" + fileName;
                } else {
                    throw new IOException("Failed to create output file");
                }
            } else {
                // For Android 9 and below, use direct file access
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File konvertDir = new File(downloadsDir, "Konvert");
                if (!konvertDir.exists()) {
                    if (!konvertDir.mkdirs()) {
                        throw new IOException("Failed to create directory: " + konvertDir.getAbsolutePath());
                    }
                }
                
                File outputFile = new File(konvertDir, fileName);
                outputStream = new FileOutputStream(outputFile);
                document.write(outputStream);
                filePath = outputFile.getAbsolutePath();
            }
            
            return filePath;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                    // Don't rethrow as we're in finally block
                }
            }
            try {
                document.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing document", e);
                // Don't rethrow as we're in finally block
            }
        }
    }
    
    /**
     * Get the name of a file from its URI
     *
     * @param context The context
     * @param uri     The URI of the file
     * @return The name of the file
     */
    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from content URI", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            } else {
                // Fallback filename if all else fails
                result = "document.pdf";
            }
        }
        return result;
    }
    
    /**
     * Generate an output file name based on the input file name
     *
     * @param inputFileName The input file name
     * @return The output file name
     */
    private static String getOutputFileName(String inputFileName) {
        // Remove .pdf extension if present
        String baseName = inputFileName.toLowerCase().endsWith(".pdf") 
                ? inputFileName.substring(0, inputFileName.length() - 4) 
                : inputFileName;
        
        // Add timestamp to ensure uniqueness
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String timestamp = sdf.format(new Date());
        
        return baseName + "_" + timestamp + ".docx";
    }
}
