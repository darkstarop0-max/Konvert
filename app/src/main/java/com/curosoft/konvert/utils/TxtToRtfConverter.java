package com.curosoft.konvert.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
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
import java.nio.charset.StandardCharsets;

/**
 * Utility class to convert TXT files to RTF format
 */
public class TxtToRtfConverter {
    private static final String TAG = "TxtToRtfConverter";

    /**
     * Convert a TXT file to RTF format
     * 
     * @param context Application context
     * @param txtUri URI of the TXT file to convert
     * @return Path to the generated RTF file or null if conversion failed
     */
    public static String convertTxtToRtf(Context context, Uri txtUri) {
        Log.d(TAG, "Starting TXT to RTF conversion");

        try {
            // Get the file name from the URI
            String fileName = EnhancedFilePickerUtils.getFileName(context, txtUri);
            Log.d(TAG, "Converting TXT file: " + fileName);

            // Create output file name
            String outputFileName = getOutputFileName(fileName);

            // Get the output directory
            File outputDir = FileStorageUtils.getOutputDirectory(context);
            File outputFile = new File(outputDir, outputFileName);

            // Make sure the output directory exists
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Read text content from the TXT file
            String textContent = readTextFromUri(context, txtUri);
            if (textContent == null) {
                Log.e(TAG, "Failed to read text content from TXT file");
                return null;
            }

            // Convert to RTF and save
            boolean success = convertToRtfAndSave(textContent, outputFile);
            if (!success) {
                Log.e(TAG, "Failed to create RTF file");
                return null;
            }

            // Add the file to MediaStore so it appears in Gallery apps
            addToMediaStore(context, outputFile, "application/rtf");

            Log.d(TAG, "TXT to RTF conversion completed successfully");
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error converting TXT to RTF", e);
            return null;
        }
    }

    /**
     * Read text from a URI pointing to a text file
     * 
     * @param context Application context
     * @param uri URI of the text file
     * @return String containing the text content
     */
    private static String readTextFromUri(Context context, Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error reading text from URI", e);
            return null;
        }
    }

    /**
     * Convert text to RTF format and save to file
     * 
     * @param textContent The text content to convert
     * @param outputFile The output RTF file
     * @return true if successful, false otherwise
     */
    private static boolean convertToRtfAndSave(String textContent, File outputFile) {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            
            // Create the RTF header
            StringBuilder rtfContent = new StringBuilder();
            rtfContent.append("{\\rtf1\\ansi\\ansicpg1252\\cocoartf1671\\cocoasubrtf600\n");
            rtfContent.append("\\cocoascreenfonts1{\\fonttbl\\f0\\fswiss\\fcharset0 Helvetica;}\n");
            rtfContent.append("{\\colortbl;\\red255\\green255\\blue255;}\n");
            rtfContent.append("\\margl1440\\margr1440\\vieww10800\\viewh8400\\viewkind0\n");
            rtfContent.append("\\pard\\tx720\\tx1440\\tx2160\\tx2880\\tx3600\\tx4320\\tx5040\\tx5760\\tx6480\\tx7200\\tx7920\\tx8640\\pardirnatural\\partightenfactor0\n\n");
            rtfContent.append("\\f0\\fs24 \\cf0 ");
            
            // Process the text content, escaping special characters and handling line breaks
            String[] lines = textContent.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                
                // Escape special RTF characters
                line = line.replace("\\", "\\\\")
                           .replace("{", "\\{")
                           .replace("}", "\\}")
                           .replace("\t", "\\tab ")
                           .replace("&", "\\&")
                           .replace("<", "\\<")
                           .replace(">", "\\>");
                
                rtfContent.append(line);
                
                // Add line break if not the last line
                if (i < lines.length - 1) {
                    rtfContent.append("\\par\n");
                }
            }
            
            // Close the RTF document
            rtfContent.append("}");
            
            // Write the RTF content to the file
            writer.write(rtfContent.toString());
            writer.flush();
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error creating RTF file", e);
            return false;
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
        
        // Remove the .txt extension if present
        if (baseName.toLowerCase().endsWith(".txt")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        
        return baseName + ".rtf";
    }

    /**
     * Add the file to the MediaStore so it's visible in file browsers
     * 
     * @param context The context
     * @param file The file to add
     * @param mimeType The MIME type of the file
     */
    private static void addToMediaStore(Context context, File file, String mimeType) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ (API 29+), use relative path and is_pending flag
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/Konvert/Converted");
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            
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
                    } catch (IOException e) {
                        Log.e(TAG, "Error copying file to MediaStore", e);
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
    }
}
