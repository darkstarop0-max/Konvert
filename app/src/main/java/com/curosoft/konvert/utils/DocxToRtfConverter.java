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
 * Utility class to convert DOCX files to Rich Text Format (RTF)
 */
public class DocxToRtfConverter {
    private static final String TAG = "DocxToRtfConverter";

    /**
     * Convert a DOCX file to RTF
     * 
     * @param context Application context
     * @param docxUri Uri of the DOCX file to convert
     * @return Path to the generated RTF file
     * @throws Exception If conversion fails
     */
    public static String convertDocxToRtf(Context context, Uri docxUri) throws Exception {
        Log.d(TAG, "Starting DOCX to RTF conversion");
        
        // Get the file name from the URI
        String fileName = getFileName(context, docxUri);
        String outputFileName = getOutputFileName(fileName);
        
        // Create the output directory based on Android version
        File outputDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ (API 29+), use app-specific directory due to Scoped Storage
            outputDir = new File(context.getExternalFilesDir(null), "Konvert/Converted");
        } else {
            // For older Android versions, use public storage
            outputDir = new File(Environment.getExternalStorageDirectory(), "Konvert/Converted");
        }
        
        // Create directory if it doesn't exist
        if (!outputDir.exists()) {
            boolean dirCreated = outputDir.mkdirs();
            Log.d(TAG, "Created output directory: " + dirCreated + " at " + outputDir.getAbsolutePath());
            if (!dirCreated) {
                Log.w(TAG, "Failed to create output directory, falling back to app-specific storage");
                // Fallback to app-specific storage if public directory creation fails
                outputDir = new File(context.getExternalFilesDir(null), "Konvert/Converted");
                outputDir.mkdirs();
            }
        }
        
        // Create temporary input and output files
        File tempInput = new File(context.getCacheDir(), "temp_input.docx");
        File outputFile = new File(outputDir, outputFileName);
        
        try {
            // Copy input stream to temporary file
            copyInputStreamToFile(context.getContentResolver().openInputStream(docxUri), tempInput);
            
            // Extract content from DOCX and save as RTF
            String textContent = extractContentFromDocx(tempInput);
            String rtfContent = convertToRtf(textContent);
            saveRtfFile(rtfContent, outputFile);
            
            // Clean up temporary files
            tempInput.delete();
            
            // Make the file visible in the media store
            addToMediaStore(context, outputFile);
            
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error converting DOCX to RTF", e);
            
            // Clean up temporary files
            tempInput.delete();
            if (outputFile.exists()) {
                outputFile.delete();
            }
            
            throw e;
        }
    }
    
    /**
     * Extract content from a DOCX file
     * 
     * DOCX files are ZIP archives containing XML files
     * The main content is in word/document.xml
     * 
     * @param docxFile Input DOCX file
     * @return Extracted text content with basic style markers
     * @throws IOException If reading fails
     */
    private static String extractContentFromDocx(File docxFile) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(docxFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (zipEntry.getName().equals("word/document.xml")) {
                    content.append(extractStyledContentFromXML(zis));
                    break;
                }
            }
        }
        
        return content.toString();
    }
    
    /**
     * Extract text with basic style information from XML content in the DOCX file
     * 
     * @param zis ZipInputStream positioned at the XML entry
     * @return Extracted text with style markers
     * @throws IOException If reading fails
     */
    private static String extractStyledContentFromXML(ZipInputStream zis) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(zis));
        String line;
        
        boolean inBold = false;
        boolean inItalic = false;
        boolean inUnderline = false;
        
        while ((line = br.readLine()) != null) {
            // Track styles
            if (line.contains("<w:b/>") || line.contains("<w:b w:val=\"true\"")) {
                inBold = true;
            }
            if (line.contains("</w:b>") || line.contains("<w:b w:val=\"false\"")) {
                inBold = false;
            }
            
            if (line.contains("<w:i/>") || line.contains("<w:i w:val=\"true\"")) {
                inItalic = true;
            }
            if (line.contains("</w:i>") || line.contains("<w:i w:val=\"false\"")) {
                inItalic = false;
            }
            
            if (line.contains("<w:u ") || line.contains("<w:u/>")) {
                inUnderline = true;
            }
            if (line.contains("</w:u>")) {
                inUnderline = false;
            }
            
            // Extract text from <w:t> tags
            int startIndex = 0;
            while (true) {
                int startTag = line.indexOf("<w:t", startIndex);
                if (startTag == -1) break;
                
                int endTag = line.indexOf("</w:t>", startTag);
                if (endTag == -1) break;
                
                int contentStart = line.indexOf(">", startTag) + 1;
                if (contentStart > 0 && contentStart < endTag) {
                    String text = line.substring(contentStart, endTag);
                    
                    // Add style markers
                    if (inBold) result.append("{{BOLD}}");
                    if (inItalic) result.append("{{ITALIC}}");
                    if (inUnderline) result.append("{{UNDERLINE}}");
                    
                    result.append(text).append(" ");
                    
                    // Close style markers
                    if (inUnderline) result.append("{{/UNDERLINE}}");
                    if (inItalic) result.append("{{/ITALIC}}");
                    if (inBold) result.append("{{/BOLD}}");
                }
                
                startIndex = endTag + 5;
            }
            
            // Look for paragraph breaks
            if (line.contains("<w:p ") || line.contains("</w:p>")) {
                result.append("\n");
            }
            
            // Look for heading styles
            if (line.contains("<w:pStyle w:val=\"Heading1\"")) {
                result.append("{{HEADING1}}");
            } else if (line.contains("<w:pStyle w:val=\"Heading2\"")) {
                result.append("{{HEADING2}}");
            } else if (line.contains("<w:pStyle w:val=\"Heading3\"")) {
                result.append("{{HEADING3}}");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Convert the extracted content with style markers to RTF format
     * 
     * @param content Content with style markers
     * @return RTF formatted content
     */
    private static String convertToRtf(String content) {
        // RTF header
        StringBuilder rtfBuilder = new StringBuilder();
        rtfBuilder.append("{\\rtf1\\ansi\\ansicpg1252\\cocoartf2580\\cocoasubrtf220\n");
        rtfBuilder.append("{\\fonttbl\\f0\\fswiss\\fcharset0 Helvetica;}\n");
        rtfBuilder.append("{\\colortbl;\\red0\\green0\\blue0;}\n");
        rtfBuilder.append("\\vieww12000\\viewh15840\\viewkind0\n");
        rtfBuilder.append("\\deftab720\n");
        
        // Process the content and replace style markers with RTF commands
        String rtfContent = content
            .replace("{{BOLD}}", "{\\b ")
            .replace("{{/BOLD}}", "}")
            .replace("{{ITALIC}}", "{\\i ")
            .replace("{{/ITALIC}}", "}")
            .replace("{{UNDERLINE}}", "{\\ul ")
            .replace("{{/UNDERLINE}}", "}")
            .replace("{{HEADING1}}", "{\\f0\\fs36\\b ")
            .replace("{{HEADING2}}", "{\\f0\\fs28\\b ")
            .replace("{{HEADING3}}", "{\\f0\\fs24\\b ")
            .replace("\n", "\\par\n")
            // Escape RTF special characters
            .replace("\\", "\\\\")
            .replace("{", "\\{")
            .replace("}", "\\}")
            // But don't double-escape our RTF commands that we just added
            .replace("\\\\{\\\\b ", "{\\b ")
            .replace("\\\\{\\\\i ", "{\\i ")
            .replace("\\\\{\\\\ul ", "{\\ul ")
            .replace("\\\\{\\\\f0", "{\\f0");
        
        rtfBuilder.append(rtfContent);
        
        // RTF footer
        rtfBuilder.append("}\n");
        
        return rtfBuilder.toString();
    }
    
    /**
     * Save RTF content to a file
     * 
     * @param rtfContent RTF content to write
     * @param rtfFile Output RTF file
     * @throws Exception If file creation fails
     */
    private static void saveRtfFile(String rtfContent, File rtfFile) throws Exception {
        Log.d(TAG, "Creating RTF file at: " + rtfFile.getAbsolutePath());
        
        // Ensure parent directory exists
        File parentDir = rtfFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            Log.d(TAG, "Created parent directory: " + created);
            if (!created) {
                throw new IOException("Failed to create parent directory for RTF file");
            }
        }
        
        // Create the output stream for the RTF file
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(rtfFile))) {
            writer.write(rtfContent);
            Log.d(TAG, "RTF file created successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error creating RTF file", e);
            throw new Exception("Failed to create RTF file: " + e.getMessage(), e);
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
     * Generate the output file name by replacing the extension with .rtf
     * 
     * @param inputFileName Original file name
     * @return Output file name with .rtf extension
     */
    private static String getOutputFileName(String inputFileName) {
        String baseName = inputFileName;
        
        // Remove the .docx extension if present
        if (baseName.toLowerCase().endsWith(".docx")) {
            baseName = baseName.substring(0, baseName.length() - 5);
        }
        
        return baseName + ".rtf";
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
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/rtf");
            
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
