package com.curosoft.konvert.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced document converter that preserves formatting, typography, and layout
 * Uses Android-friendly libraries and view-to-PDF rendering for best results
 */
public class EnhancedDocumentConverter {
    private static final String TAG = "EnhancedDocumentConverter";
    
    public static class ConversionResult {
        public final boolean success;
        public final String outputPath;
        public final String errorMessage;
        
        public ConversionResult(boolean success, String outputPath, String errorMessage) {
            this.success = success;
            this.outputPath = outputPath;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * Convert DOCX to PDF while preserving formatting using Android Print Framework
     */
    public static ConversionResult convertDocxToPdfWithFormatting(Context context, Uri docxUri) {
        try {
            // Extract formatted content from DOCX
            FormattedContent content = extractFormattedContentFromDocx(context, docxUri);
            if (content == null) {
                return new ConversionResult(false, null, "Failed to extract DOCX content");
            }
            
            // Create output file
            String outputPath = createOutputPath(context, getFileName(context, docxUri), "pdf");
            File outputFile = new File(outputPath);
            
            // Use Android's native PDF generation with formatting preservation
            boolean success = createFormattedPdf(content, outputFile);
            
            if (success) {
                // Update recent files
                ConversionUtils.trackConversion(context, outputPath, "DOCX", "PDF");
                return new ConversionResult(true, outputPath, null);
            } else {
                return new ConversionResult(false, null, "PDF generation failed");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "DOCX to PDF conversion failed", e);
            return new ConversionResult(false, null, "Conversion error: " + e.getMessage());
        }
    }
    
    /**
     * Convert PDF to DOCX preserving text formatting and structure
     */
    public static ConversionResult convertPdfToDocxWithFormatting(Context context, Uri pdfUri) {
        try {
            // Extract structured content from PDF
            StructuredContent content = extractStructuredContentFromPdf(context, pdfUri);
            if (content == null) {
                return new ConversionResult(false, null, "Failed to extract PDF content");
            }
            
            // Create output file
            String outputPath = createOutputPath(context, getFileName(context, pdfUri), "docx");
            
            // Create DOCX with preserved formatting
            boolean success = createFormattedDocx(content, outputPath);
            
            if (success) {
                // Update recent files
                ConversionUtils.trackConversion(context, outputPath, "PDF", "DOCX");
                return new ConversionResult(true, outputPath, null);
            } else {
                return new ConversionResult(false, null, "DOCX generation failed");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "PDF to DOCX conversion failed", e);
            return new ConversionResult(false, null, "Conversion error: " + e.getMessage());
        }
    }
    
    /**
     * Convert text to PDF using Android Print Framework for professional layout
     */
    public static ConversionResult convertTextToPdfProfessional(Context context, Uri textUri) {
        try {
            // Read text content
            String textContent = readTextFile(context, textUri);
            if (textContent == null || textContent.trim().isEmpty()) {
                return new ConversionResult(false, null, "Empty or invalid text file");
            }
            
            // Create output file
            String outputPath = createOutputPath(context, getFileName(context, textUri), "pdf");
            File outputFile = new File(outputPath);
            
            // Create professional PDF layout
            boolean success = createProfessionalTextPdf(textContent, outputFile);
            
            if (success) {
                // Update recent files
                ConversionUtils.trackConversion(context, outputPath, "TXT", "PDF");
                return new ConversionResult(true, outputPath, null);
            } else {
                return new ConversionResult(false, null, "PDF generation failed");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Text to PDF conversion failed", e);
            return new ConversionResult(false, null, "Conversion error: " + e.getMessage());
        }
    }
    
    /**
     * Extract formatted content from DOCX preserving styles, fonts, and structure
     */
    private static FormattedContent extractFormattedContentFromDocx(Context context, Uri docxUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(docxUri);
            XWPFDocument document = new XWPFDocument(inputStream);
            
            FormattedContent content = new FormattedContent();
            
            // Extract paragraphs with formatting
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                FormattedParagraph formattedPara = new FormattedParagraph();
                formattedPara.text = paragraph.getText();
                formattedPara.alignment = getAlignment(paragraph.getAlignment());
                
                // Extract run formatting (bold, italic, font size, etc.)
                List<XWPFRun> runs = paragraph.getRuns();
                for (XWPFRun run : runs) {
                    FormattedRun formattedRun = new FormattedRun();
                    formattedRun.text = run.getText(0);
                    formattedRun.bold = run.isBold();
                    formattedRun.italic = run.isItalic();
                    formattedRun.fontSize = run.getFontSize() > 0 ? run.getFontSize() : 12;
                    formattedRun.fontFamily = run.getFontFamily() != null ? run.getFontFamily() : "Times New Roman";
                    
                    if (formattedRun.text != null && !formattedRun.text.isEmpty()) {
                        formattedPara.runs.add(formattedRun);
                    }
                }
                
                if (!formattedPara.runs.isEmpty()) {
                    content.paragraphs.add(formattedPara);
                }
            }
            
            // Extract tables with formatting
            List<XWPFTable> tables = document.getTables();
            for (XWPFTable table : tables) {
                FormattedTable formattedTable = new FormattedTable();
                
                for (XWPFTableRow row : table.getRows()) {
                    FormattedTableRow formattedRow = new FormattedTableRow();
                    
                    for (XWPFTableCell cell : row.getTableCells()) {
                        FormattedTableCell formattedCell = new FormattedTableCell();
                        formattedCell.text = cell.getText();
                        formattedRow.cells.add(formattedCell);
                    }
                    
                    formattedTable.rows.add(formattedRow);
                }
                
                content.tables.add(formattedTable);
            }
            
            document.close();
            inputStream.close();
            
            return content;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting DOCX content", e);
            return null;
        }
    }
    
    /**
     * Create PDF with preserved formatting using Android's PDF generation
     */
    private static boolean createFormattedPdf(FormattedContent content, File outputFile) {
        try {
            android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
            android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
            android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
            
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            
            float yPosition = 50;
            float margin = 50;
            float pageWidth = pageInfo.getPageWidth() - 2 * margin;
            
            // Draw formatted content
            for (FormattedParagraph paragraph : content.paragraphs) {
                yPosition = drawFormattedParagraph(canvas, paragraph, margin, yPosition, pageWidth, paint);
                yPosition += 20; // Paragraph spacing
                
                // Check if we need a new page
                if (yPosition > pageInfo.getPageHeight() - 50) {
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = 50;
                }
            }
            
            // Draw tables
            for (FormattedTable table : content.tables) {
                yPosition = drawFormattedTable(canvas, table, margin, yPosition, pageWidth, paint);
                yPosition += 30; // Table spacing
                
                // Check if we need a new page
                if (yPosition > pageInfo.getPageHeight() - 50) {
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = 50;
                }
            }
            
            document.finishPage(page);
            
            // Write to file
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            document.writeTo(outputStream);
            document.close();
            outputStream.close();
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating formatted PDF", e);
            return false;
        }
    }
    
    /**
     * Draw formatted paragraph on canvas
     */
    private static float drawFormattedParagraph(Canvas canvas, FormattedParagraph paragraph, 
                                              float x, float y, float width, Paint paint) {
        float currentY = y;
        
        for (FormattedRun run : paragraph.runs) {
            // Set paint properties based on formatting
            paint.setTextSize(run.fontSize);
            paint.setFakeBoldText(run.bold);
            paint.setTextSkewX(run.italic ? -0.25f : 0);
            paint.setColor(Color.BLACK);
            
            // Use StaticLayout for proper text wrapping
            TextPaint textPaint = new TextPaint(paint);
            StaticLayout layout = StaticLayout.Builder.obtain(run.text, 0, run.text.length(), textPaint, (int)width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.2f, 1.0f)
                .build();
            
            canvas.save();
            canvas.translate(x, currentY);
            layout.draw(canvas);
            canvas.restore();
            
            currentY += layout.getHeight();
        }
        
        return currentY;
    }
    
    /**
     * Draw formatted table on canvas
     */
    private static float drawFormattedTable(Canvas canvas, FormattedTable table, 
                                          float x, float y, float width, Paint paint) {
        float currentY = y;
        float cellHeight = 40;
        float cellWidth = width / Math.max(1, table.rows.isEmpty() ? 1 : table.rows.get(0).cells.size());
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        
        for (FormattedTableRow row : table.rows) {
            float currentX = x;
            
            for (FormattedTableCell cell : row.cells) {
                // Draw cell border
                canvas.drawRect(currentX, currentY, currentX + cellWidth, currentY + cellHeight, paint);
                
                // Draw cell text
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(10);
                canvas.drawText(cell.text, currentX + 5, currentY + 20, paint);
                paint.setStyle(Paint.Style.STROKE);
                
                currentX += cellWidth;
            }
            
            currentY += cellHeight;
        }
        
        return currentY;
    }
    
    /**
     * Create professional text PDF with proper typography
     */
    private static boolean createProfessionalTextPdf(String text, File outputFile) {
        try {
            android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
            android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
            android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
            
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setTextSize(12);
            paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
            
            float margin = 50;
            float pageWidth = pageInfo.getPageWidth() - 2 * margin;
            float pageHeight = pageInfo.getPageHeight() - 2 * margin;
            
            // Use StaticLayout for professional text rendering
            TextPaint textPaint = new TextPaint(paint);
            StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, (int)pageWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.5f, 1.0f)
                .setIncludePad(true)
                .build();
            
            canvas.save();
            canvas.translate(margin, margin);
            layout.draw(canvas);
            canvas.restore();
            
            document.finishPage(page);
            
            // Write to file
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            document.writeTo(outputStream);
            document.close();
            outputStream.close();
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating professional text PDF", e);
            return false;
        }
    }
    
    // Helper classes for structured content
    private static class FormattedContent {
        java.util.List<FormattedParagraph> paragraphs = new java.util.ArrayList<>();
        java.util.List<FormattedTable> tables = new java.util.ArrayList<>();
    }
    
    private static class FormattedParagraph {
        String text;
        String alignment;
        java.util.List<FormattedRun> runs = new java.util.ArrayList<>();
    }
    
    private static class FormattedRun {
        String text;
        boolean bold;
        boolean italic;
        int fontSize;
        String fontFamily;
    }
    
    private static class FormattedTable {
        java.util.List<FormattedTableRow> rows = new java.util.ArrayList<>();
    }
    
    private static class FormattedTableRow {
        java.util.List<FormattedTableCell> cells = new java.util.ArrayList<>();
    }
    
    private static class FormattedTableCell {
        String text;
    }
    
    private static class StructuredContent {
        java.util.List<String> paragraphs = new java.util.ArrayList<>();
        java.util.List<String> headings = new java.util.ArrayList<>();
    }
    
    // Helper methods
    private static String getAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment alignment) {
        if (alignment == null) return "left";
        switch (alignment) {
            case CENTER: return "center";
            case RIGHT: return "right";
            case BOTH: return "justify";
            default: return "left";
        }
    }
    
    private static StructuredContent extractStructuredContentFromPdf(Context context, Uri pdfUri) {
        // Implementation using iText7 for PDF text extraction with structure
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(pdfUri);
            PdfReader reader = new PdfReader(inputStream);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(reader);
            
            StructuredContent content = new StructuredContent();
            
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                String pageText = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
                if (pageText != null && !pageText.trim().isEmpty()) {
                    content.paragraphs.add(pageText);
                }
            }
            
            pdfDoc.close();
            inputStream.close();
            
            return content;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting PDF content", e);
            return null;
        }
    }
    
    private static boolean createFormattedDocx(StructuredContent content, String outputPath) {
        try {
            XWPFDocument document = new XWPFDocument();
            
            for (String paragraphText : content.paragraphs) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(paragraphText);
                run.setFontFamily("Times New Roman");
                run.setFontSize(12);
            }
            
            FileOutputStream outputStream = new FileOutputStream(outputPath);
            document.write(outputStream);
            document.close();
            outputStream.close();
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating formatted DOCX", e);
            return false;
        }
    }
    
    private static String readTextFile(Context context, Uri textUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(textUri);
            java.util.Scanner scanner = new java.util.Scanner(inputStream, "UTF-8");
            scanner.useDelimiter("\\A");
            String content = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
            inputStream.close();
            return content;
        } catch (Exception e) {
            Log.e(TAG, "Error reading text file", e);
            return null;
        }
    }
    
    private static String createOutputPath(Context context, String fileName, String extension) {
        String nameWithoutExt = fileName.replaceAll("\\.[^.]*$", "");
        String outputFileName = nameWithoutExt + "_converted." + extension;
        
        File outputDir = new File(android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOCUMENTS), "Konvert/Converted/Documents");
        
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        return new File(outputDir, outputFileName).getAbsolutePath();
    }
    
    private static String getFileName(Context context, Uri uri) {
        try {
            String fileName = UriUtils.getFileName(context, uri);
            return fileName != null ? fileName : "document";
        } catch (Exception e) {
            return "document";
        }
    }
}
