package com.curosoft.konvert.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class to convert DOCX files to OpenDocument Text (ODT) format
 */
public class DocxToOdtConverter {
    private static final String TAG = "DocxToOdtConverter";

    /**
     * Convert a DOCX file to ODT
     * 
     * @param context Application context
     * @param docxUri Uri of the DOCX file to convert
     * @return Path to the generated ODT file
     * @throws Exception If conversion fails
     */
    public static String convertDocxToOdt(Context context, Uri docxUri) throws Exception {
        Log.d(TAG, "Starting DOCX to ODT conversion");
        
        // Get the file name from the URI
        String fileName = getFileName(context, docxUri);
        String outputFileName = getOutputFileName(fileName);
        
        // Get the output directory using the FileStorageUtils
        File outputDir = FileStorageUtils.getOutputDirectory(context);
        
        // Create temporary input and output files
        File tempInput = new File(context.getCacheDir(), "temp_input.docx");
        File outputFile = new File(outputDir, outputFileName);
        
        // Create temp directory for extraction
        File tempDir = new File(context.getCacheDir(), "temp_docx_extraction");
        if (tempDir.exists()) {
            deleteRecursive(tempDir);
        }
        tempDir.mkdirs();
        
        try {
            // Copy input stream to temporary file
            copyInputStreamToFile(context.getContentResolver().openInputStream(docxUri), tempInput);
            
            // Convert DOCX to ODT
            convertDocxToOdtFile(tempInput, outputFile, tempDir);
            
            // Clean up temporary files
            tempInput.delete();
            deleteRecursive(tempDir);
            
            // Make the file visible in the media store
            addToMediaStore(context, outputFile);
            
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error converting DOCX to ODT", e);
            
            // Clean up temporary files
            tempInput.delete();
            deleteRecursive(tempDir);
            if (outputFile.exists()) {
                outputFile.delete();
            }
            
            throw e;
        }
    }
    
    /**
     * Convert DOCX file to ODT format
     * 
     * @param docxFile Input DOCX file
     * @param odtFile Output ODT file
     * @param tempDir Temporary directory for processing
     * @throws IOException If conversion fails
     */
    private static void convertDocxToOdtFile(File docxFile, File odtFile, File tempDir) throws IOException {
        Log.d(TAG, "Converting DOCX to ODT");
        
        // Extract DOCX content
        extractDocx(docxFile, tempDir);
        
        // Create ODT structure
        File odtStructureDir = new File(tempDir, "odt_structure");
        odtStructureDir.mkdir();
        
        // Create the basic ODT directory structure
        createOdtStructure(odtStructureDir);
        
        // Convert DOCX content to ODT format
        convertContent(tempDir, odtStructureDir);
        
        // Create the final ODT file (zip the structure)
        createOdtFile(odtStructureDir, odtFile);
        
        Log.d(TAG, "DOCX to ODT conversion completed");
    }
    
    /**
     * Extract DOCX file contents (which is a ZIP file)
     * 
     * @param docxFile Input DOCX file
     * @param outputDir Output directory for extracted files
     * @throws IOException If extraction fails
     */
    private static void extractDocx(File docxFile, File outputDir) throws IOException {
        Log.d(TAG, "Extracting DOCX content");
        
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(docxFile)))) {
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                File outputFile = new File(outputDir, entryName);
                
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                    continue;
                }
                
                // Create parent directories if they don't exist
                File parent = outputFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                
                // Extract the file
                try (FileOutputStream fos = new FileOutputStream(outputFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos, 8192)) {
                    
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, read);
                    }
                    bos.flush();
                }
                
                zis.closeEntry();
            }
        }
    }
    
    /**
     * Create the basic ODT directory structure
     * 
     * @param odtStructureDir Directory to create the structure in
     * @throws IOException If creation fails
     */
    private static void createOdtStructure(File odtStructureDir) throws IOException {
        Log.d(TAG, "Creating ODT structure");
        
        // Create the basic ODT structure
        File metaInfDir = new File(odtStructureDir, "META-INF");
        metaInfDir.mkdir();
        
        // Create mimetype file (must be first in the archive, uncompressed)
        File mimetypeFile = new File(odtStructureDir, "mimetype");
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
        
        // Create other required files
        createBasicXmlFile(odtStructureDir, "meta.xml", "meta");
        createBasicXmlFile(odtStructureDir, "styles.xml", "styles");
    }
    
    /**
     * Create a basic XML file for ODT structure
     * 
     * @param dir Directory to create the file in
     * @param fileName Name of the file
     * @param rootElement Root element name
     * @throws IOException If creation fails
     */
    private static void createBasicXmlFile(File dir, String fileName, String rootElement) throws IOException {
        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<office:document-" + rootElement + " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\">\n" +
                    "</office:document-" + rootElement + ">";
            fos.write(content.getBytes());
        }
    }
    
    /**
     * Convert DOCX content to ODT format
     * 
     * @param docxDir Directory containing extracted DOCX
     * @param odtDir Directory for ODT structure
     * @throws IOException If conversion fails
     */
    private static void convertContent(File docxDir, File odtDir) throws IOException {
        Log.d(TAG, "Converting document content");
        
        // Simplified conversion - extract text from document.xml and create a basic content.xml
        File documentXml = new File(docxDir, "word/document.xml");
        String contentXml = "";
        
        if (documentXml.exists()) {
            // Read document.xml
            StringBuilder xmlContent = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(documentXml)) {
                byte[] buffer = new byte[(int) documentXml.length()];
                fis.read(buffer);
                xmlContent.append(new String(buffer));
            }
            
            // Create a simplified content.xml
            contentXml = createContentXml(xmlContent.toString());
        } else {
            // If document.xml doesn't exist, create an empty content.xml
            contentXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" " +
                    "xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\">\n" +
                    "  <office:body>\n" +
                    "    <office:text>\n" +
                    "      <text:p>Document conversion failed - no content found</text:p>\n" +
                    "    </office:text>\n" +
                    "  </office:body>\n" +
                    "</office:document-content>";
        }
        
        // Write content.xml
        File contentXmlFile = new File(odtDir, "content.xml");
        try (FileOutputStream fos = new FileOutputStream(contentXmlFile)) {
            fos.write(contentXml.getBytes());
        }
    }
    
    /**
     * Create content.xml by extracting text from document.xml
     * 
     * @param documentXml The DOCX document.xml content
     * @return ODT content.xml
     */
    private static String createContentXml(String documentXml) {
        StringBuilder content = new StringBuilder();
        
        // Start the content.xml
        content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        content.append("<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ");
        content.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ");
        content.append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\">\n");
        content.append("  <office:body>\n");
        content.append("    <office:text>\n");
        
        // Extract text from w:p and w:t tags and convert to ODT format
        // This is a simplified conversion - a full conversion would be more complex
        int paragraphIndex = 0;
        while (true) {
            int startP = documentXml.indexOf("<w:p", paragraphIndex);
            if (startP == -1) break;
            
            int endP = documentXml.indexOf("</w:p>", startP);
            if (endP == -1) break;
            
            String paragraph = documentXml.substring(startP, endP + 6);
            content.append("      <text:p>");
            
            // Extract text
            int textIndex = 0;
            while (true) {
                int startT = paragraph.indexOf("<w:t", textIndex);
                if (startT == -1) break;
                
                int endT = paragraph.indexOf("</w:t>", startT);
                if (endT == -1) break;
                
                int contentStart = paragraph.indexOf(">", startT) + 1;
                if (contentStart > 0 && contentStart < endT) {
                    String text = paragraph.substring(contentStart, endT);
                    // Escape XML special characters
                    text = text.replace("&", "&amp;")
                               .replace("<", "&lt;")
                               .replace(">", "&gt;")
                               .replace("\"", "&quot;")
                               .replace("'", "&apos;");
                    content.append(text);
                }
                
                textIndex = endT + 5;
            }
            
            content.append("</text:p>\n");
            paragraphIndex = endP + 6;
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
     * @param odtStructureDir Directory containing ODT structure
     * @param odtFile Output ODT file
     * @throws IOException If zipping fails
     */
    private static void createOdtFile(File odtStructureDir, File odtFile) throws IOException {
        Log.d(TAG, "Creating final ODT file");
        
        // Make sure parent directory exists
        File parentDir = odtFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // The mimetype file must be first and uncompressed
        try (FileOutputStream fos = new FileOutputStream(odtFile);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
            
            // First add the mimetype file (uncompressed)
            File mimetypeFile = new File(odtStructureDir, "mimetype");
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
            addFilesToZip(odtStructureDir, "", zos, "mimetype");
        }
    }
    
    /**
     * Add files to ZIP recursively
     * 
     * @param directory Directory to add
     * @param path Path within the ZIP
     * @param zos ZIP output stream
     * @param skipFile File to skip
     * @throws IOException If zipping fails
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
     * @param file Input file
     * @return CRC-32 value
     * @throws IOException If reading fails
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
     * @param fileOrDir File or directory to delete
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
     * Generate the output file name by replacing the extension with .odt
     * 
     * @param inputFileName Original file name
     * @return Output file name with .odt extension
     */
    private static String getOutputFileName(String inputFileName) {
        String baseName = inputFileName;
        
        // Remove the .docx extension if present
        if (baseName.toLowerCase().endsWith(".docx")) {
            baseName = baseName.substring(0, baseName.length() - 5);
        }
        
        return baseName + ".odt";
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
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.oasis.opendocument.text");
            
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
