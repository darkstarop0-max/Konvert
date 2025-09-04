package com.curosoft.konvert.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class to convert DOCX files to plain text (TXT) format
 */
public class DocxToTxtConverter {
    private static final String TAG = "DocxToTxtConverter";

    /**
     * Convert a DOCX file to TXT for display purposes (simplified text extraction)
     * 
     * @param context Application context
     * @param docxUri Uri of the DOCX file to extract text from
     * @return Plain text content from the DOCX file
     * @throws Exception If extraction fails
     */
    public static String convertDocxToTxt(Context context, Uri docxUri) throws Exception {
        Log.d(TAG, "Starting DOCX text extraction for display");
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(docxUri)) {
            if (inputStream == null) {
                throw new IOException("Cannot open input stream for DOCX file");
            }
            
            return extractTextFromDocx(inputStream);
        } catch (Exception e) {
            Log.e(TAG, "Error extracting text from DOCX", e);
            throw new Exception("Failed to extract text from DOCX: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert a DOCX file to TXT and save to file
     * 
     * @param context Application context
     * @param docxUri Uri of the DOCX file to convert
     * @return Path to the generated TXT file
     * @throws Exception If conversion fails
     */
    public static String convertDocxToTxtFile(Context context, Uri docxUri) throws Exception {
        Log.d(TAG, "Starting DOCX to TXT conversion");
        
        // Get the file name from the URI
        String fileName = getFileName(context, docxUri);
        String outputFileName = getOutputFileName(fileName);
        
        // Get the output directory using the FileStorageUtils
        File outputDir = FileStorageUtils.getOutputDirectory(context);
        
        // Create temporary input and output files
        File tempInput = new File(context.getCacheDir(), "temp_input.docx");
        File outputFile = new File(outputDir, outputFileName);
        
        try {
            // Copy input stream to temporary file
            copyInputStreamToFile(context.getContentResolver().openInputStream(docxUri), tempInput);
            
            // Extract text from DOCX and save as TXT
            String textContent = extractTextFromDocx(tempInput);
            saveTxtFile(textContent, outputFile);
            
            // Clean up temporary files
            tempInput.delete();
            
            // Make the file visible in the media store
            addToMediaStore(context, outputFile);
            
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error converting DOCX to TXT", e);
            
            // Clean up temporary files
            tempInput.delete();
            if (outputFile.exists()) {
                outputFile.delete();
            }
            
            throw e;
        }
    }
    
    /**
     * Extract text from a DOCX file
     * 
     * DOCX files are ZIP archives containing XML files
     * The main content is in word/document.xml
     * 
     * @param docxFile Input DOCX file
     * @return Extracted text content
     * @throws IOException If reading fails
     */
    private static String extractTextFromDocx(File docxFile) throws IOException {
        StringBuilder textContent = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(docxFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (zipEntry.getName().equals("word/document.xml")) {
                    textContent.append(extractTextFromXML(zis));
                    break;
                }
            }
        }
        
        return textContent.toString();
    }
    
    /**
     * Extract text from XML content in the DOCX file
     * 
     * @param zis ZipInputStream positioned at the XML entry
     * @return Extracted text
     * @throws IOException If reading fails
     */
    private static String extractTextFromXML(ZipInputStream zis) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(zis));
        String line;
        
        while ((line = br.readLine()) != null) {
            // Simple XML parsing to extract text from <w:t> tags
            int startIndex = 0;
            while (true) {
                int startTag = line.indexOf("<w:t", startIndex);
                if (startTag == -1) break;
                
                int endTag = line.indexOf("</w:t>", startTag);
                if (endTag == -1) break;
                
                int contentStart = line.indexOf(">", startTag) + 1;
                if (contentStart > 0 && contentStart < endTag) {
                    String text = line.substring(contentStart, endTag);
                    result.append(text).append(" ");
                }
                
                startIndex = endTag + 5;
            }
            
            // Look for paragraph breaks
            if (line.contains("<w:p ") || line.contains("</w:p>")) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Save extracted text to a TXT file
     * 
     * @param textContent Text content to write
     * @param txtFile Output TXT file
     * @throws Exception If file creation fails
     */
    private static void saveTxtFile(String textContent, File txtFile) throws Exception {
        Log.d(TAG, "Creating TXT file at: " + txtFile.getAbsolutePath());
        
        // Ensure parent directory exists
        File parentDir = txtFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            Log.d(TAG, "Created parent directory: " + created);
            if (!created) {
                throw new IOException("Failed to create parent directory for TXT file");
            }
        }
        
        // Create the output stream for the TXT file
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(txtFile))) {
            writer.write(textContent);
            Log.d(TAG, "TXT file created successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error creating TXT file", e);
            throw new Exception("Failed to create TXT file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Copy input stream to file
     * 
     * @param inputStream Input stream
     * @param outputFile Output file
     * @throws IOException If copying fails
     */
    private static void copyInputStreamToFile(InputStream inputStream, File outputFile) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input stream", e);
            }
        }
    }
    
    /**
     * Get the original file name from the URI
     * 
     * @param context Application context
     * @param uri Uri of the file
     * @return Original file name
     */
    private static String getFileName(Context context, Uri uri) {
        return EnhancedFilePickerUtils.getFileName(context, uri);
    }
    
    /**
     * Generate the output file name by replacing the extension with .txt
     * 
     * @param inputFileName Original file name
     * @return Output file name with .txt extension
     */
    private static String getOutputFileName(String inputFileName) {
        String baseName = inputFileName;
        
        // Remove the .docx extension if present
        if (baseName.toLowerCase().endsWith(".docx")) {
            baseName = baseName.substring(0, baseName.length() - 5);
        }
        
        return baseName + ".txt";
    }
    
    /**
     * Add the generated file to the MediaStore so it appears in the gallery
     * 
     * @param context Application context
     * @param file File to add
     */
    private static void addToMediaStore(Context context, File file) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            
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
    
    /**
     * Extract text content from DOCX file using InputStream (for display purposes)
     * 
     * @param inputStream InputStream of the DOCX file
     * @return Extracted text content
     * @throws IOException If extraction fails
     */
    private static String extractTextFromDocx(InputStream inputStream) throws IOException {
        StringBuilder textContent = new StringBuilder();
        
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    textContent.append(extractTextFromXML(zis));
                    break;
                }
            }
        }
        
        return textContent.toString().trim();
    }
}
