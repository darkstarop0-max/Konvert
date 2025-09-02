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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for converting PDF files to ODT (OpenDocument Text) format
 */
public class PdfToOdtConverter {
    private static final String TAG = "PdfToOdtConverter";
    
    /**
     * Convert a PDF file to ODT format
     *
     * @param context The context
     * @param pdfUri  The URI of the PDF file
     * @return The path to the converted ODT file, or null if conversion failed
     */
    public static String convertPdfToOdt(Context context, Uri pdfUri) {
        Log.d(TAG, "Starting PDF to ODT conversion");
        
        try {
            // Get the PDF file name
            String pdfFileName = EnhancedFilePickerUtils.getFileName(context, pdfUri);
            Log.d(TAG, "Converting PDF: " + pdfFileName);
            
            // Create output file name based on the input name
            String odtFileName = getOutputFileName(pdfFileName);
            
            // Extract text from PDF
            String extractedText = extractTextFromPdf(context, pdfUri);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                Log.e(TAG, "Failed to extract text from PDF");
                return null;
            }
            
            // Create a temporary directory for ODT structure
            File tempDir = new File(context.getCacheDir(), "odt_temp_" + System.currentTimeMillis());
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            try {
                // Create ODT structure
                createOdtStructure(tempDir, extractedText);
                
                // Get output directory using FileStorageUtils
                File outputDir = FileStorageUtils.getOutputDirectory(context);
                File outputFile = new File(outputDir, odtFileName);
                
                // Create the ODT file (zip the structure)
                createOdtFile(tempDir, outputFile);
                
                // Make the file visible in the MediaStore
                addToMediaStore(context, outputFile, "application/vnd.oasis.opendocument.text");
                
                Log.d(TAG, "Conversion successful. Output file: " + outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath();
            } finally {
                // Clean up temporary directory
                deleteRecursive(tempDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting PDF to ODT", e);
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
     * Create the basic ODT directory structure
     *
     * @param tempDir   The temporary directory to create the structure in
     * @param textContent The text content to include in the ODT file
     * @throws IOException if there's an error creating the structure
     */
    private static void createOdtStructure(File tempDir, String textContent) throws IOException {
        Log.d(TAG, "Creating ODT structure");
        
        // Create the basic ODT structure
        File metaInfDir = new File(tempDir, "META-INF");
        metaInfDir.mkdir();
        
        // Create mimetype file (must be first in the archive, uncompressed)
        File mimetypeFile = new File(tempDir, "mimetype");
        try (FileOutputStream fos = new FileOutputStream(mimetypeFile)) {
            fos.write("application/vnd.oasis.opendocument.text".getBytes());
        }
        
        // Create manifest file
        File manifestFile = new File(metaInfDir, "manifest.xml");
        try (FileOutputStream fos = new FileOutputStream(manifestFile)) {
            String manifest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\">\n" +
                    " <manifest:file-entry manifest:media-type=\"application/vnd.oasis.opendocument.text\" manifest:full-path=\"/\"/>\n" +
                    " <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"content.xml\"/>\n" +
                    " <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"meta.xml\"/>\n" +
                    " <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"styles.xml\"/>\n" +
                    "</manifest:manifest>";
            fos.write(manifest.getBytes());
        }
        
        // Create meta.xml
        File metaFile = new File(tempDir, "meta.xml");
        try (FileOutputStream fos = new FileOutputStream(metaFile)) {
            String meta = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<office:document-meta xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" " +
                    "xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\">\n" +
                    " <office:meta>\n" +
                    "  <meta:generator>Konvert App</meta:generator>\n" +
                    "  <meta:creation-date>" + java.time.OffsetDateTime.now().toString() + "</meta:creation-date>\n" +
                    " </office:meta>\n" +
                    "</office:document-meta>";
            fos.write(meta.getBytes());
        }
        
        // Create styles.xml
        File stylesFile = new File(tempDir, "styles.xml");
        try (FileOutputStream fos = new FileOutputStream(stylesFile)) {
            String styles = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<office:document-styles xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\">\n" +
                    "</office:document-styles>";
            fos.write(styles.getBytes());
        }
        
        // Create content.xml with the extracted text
        File contentFile = new File(tempDir, "content.xml");
        try (FileOutputStream fos = new FileOutputStream(contentFile)) {
            String content = createContentXml(textContent);
            fos.write(content.getBytes());
        }
    }
    
    /**
     * Create content.xml with the extracted text
     *
     * @param textContent The text content to include
     * @return The content.xml as a string
     */
    private static String createContentXml(String textContent) {
        StringBuilder content = new StringBuilder();
        
        // Start the content.xml
        content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        content.append("<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ");
        content.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ");
        content.append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\">\n");
        content.append("  <office:body>\n");
        content.append("    <office:text>\n");
        
        // Add the text content as paragraphs
        String[] paragraphs = textContent.split("\\r?\\n");
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                content.append("      <text:p>");
                // Escape XML special characters
                String escapedText = paragraph.replace("&", "&amp;")
                                          .replace("<", "&lt;")
                                          .replace(">", "&gt;")
                                          .replace("\"", "&quot;")
                                          .replace("'", "&apos;");
                content.append(escapedText);
                content.append("</text:p>\n");
            } else {
                // Empty paragraph
                content.append("      <text:p/>\n");
            }
        }
        
        // Close the content.xml
        content.append("    </office:text>\n");
        content.append("  </office:body>\n");
        content.append("</office:document-content>");
        
        return content.toString();
    }
    
    /**
     * Create the final ODT file by zipping the structure
     *
     * @param tempDir    The temporary directory containing the ODT structure
     * @param outputFile The output ODT file
     * @throws IOException if there's an error creating the ODT file
     */
    private static void createOdtFile(File tempDir, File outputFile) throws IOException {
        Log.d(TAG, "Creating final ODT file");
        
        // Make sure parent directory exists
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // The mimetype file must be first and uncompressed
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
            
            // First add the mimetype file (uncompressed)
            File mimetypeFile = new File(tempDir, "mimetype");
            if (mimetypeFile.exists()) {
                ZipEntry mimetypeEntry = new ZipEntry("mimetype");
                mimetypeEntry.setMethod(ZipEntry.STORED);
                
                // Set size, compressed size, and CRC for STORED method
                mimetypeEntry.setSize(mimetypeFile.length());
                mimetypeEntry.setCompressedSize(mimetypeFile.length());
                
                // Calculate CRC
                long crc = calculateCrc(mimetypeFile);
                mimetypeEntry.setCrc(crc);
                
                zos.putNextEntry(mimetypeEntry);
                
                try (FileInputStream fis = new FileInputStream(mimetypeFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                }
                
                zos.closeEntry();
            }
            
            // Add all other files
            addFilesToZip(tempDir, "", zos, "mimetype");
        }
    }
    
    /**
     * Add files to ZIP recursively
     *
     * @param directory Directory to add
     * @param path      Path within the ZIP
     * @param zos       ZIP output stream
     * @param skipFile  File to skip
     * @throws IOException if there's an error adding files
     */
    private static void addFilesToZip(File directory, String path, ZipOutputStream zos, String skipFile) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            String name = file.getName();
            
            // Skip the specified file
            if (name.equals(skipFile) && path.isEmpty()) {
                continue;
            }
            
            String entryPath = path.isEmpty() ? name : path + "/" + name;
            
            if (file.isDirectory()) {
                // For directories, add a directory entry and process recursively
                ZipEntry entry = new ZipEntry(entryPath + "/");
                zos.putNextEntry(entry);
                zos.closeEntry();
                
                addFilesToZip(file, entryPath, zos, skipFile);
            } else {
                // For files, add file entry
                ZipEntry entry = new ZipEntry(entryPath);
                zos.putNextEntry(entry);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                }
                
                zos.closeEntry();
            }
        }
    }
    
    /**
     * Calculate CRC-32 for a file
     *
     * @param file The file to calculate CRC for
     * @return The CRC-32 value
     * @throws IOException if there's an error reading the file
     */
    private static long calculateCrc(File file) throws IOException {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
        }
        
        return crc.getValue();
    }
    
    /**
     * Delete a directory and its contents recursively
     *
     * @param fileOrDir The file or directory to delete
     */
    private static void deleteRecursive(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] files = fileOrDir.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDir.delete();
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
        
        return baseName + ".odt";
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
