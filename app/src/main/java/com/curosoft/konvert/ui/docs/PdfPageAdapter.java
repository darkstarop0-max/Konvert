package com.curosoft.konvert.ui.docs;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.curosoft.konvert.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageViewHolder> {
    
    private PdfRenderer pdfRenderer;
    private List<Bitmap> pages;
    private float zoomLevel = 1.0f;
    private int pageWidth = 595; // Default A4 width in points
    private int pageHeight = 842; // Default A4 height in points
    
    public PdfPageAdapter() {
        pages = new ArrayList<>();
    }
    
    public void setPdfRenderer(PdfRenderer renderer) {
        this.pdfRenderer = renderer;
        this.pages.clear();
        
        if (renderer != null) {
            // Pre-render all pages for smooth scrolling
            for (int i = 0; i < renderer.getPageCount(); i++) {
                pages.add(null); // Placeholder for lazy loading
            }
            notifyDataSetChanged();
        }
    }
    
    public void setZoomLevel(float zoomLevel) {
        this.zoomLevel = zoomLevel;
        // Clear cached pages to force re-render at new zoom
        for (int i = 0; i < pages.size(); i++) {
            pages.set(i, null);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pdf_page, parent, false);
        return new PageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        if (pdfRenderer == null) return;
        
        Bitmap pageBitmap = pages.get(position);
        if (pageBitmap == null) {
            // Render page if not cached
            pageBitmap = renderPage(position);
            pages.set(position, pageBitmap);
        }
        
        if (pageBitmap != null) {
            holder.pageImageView.setImageBitmap(pageBitmap);
        }
    }
    
    @Override
    public int getItemCount() {
        return pages.size();
    }
    
    private Bitmap renderPage(int pageIndex) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pdfRenderer.getPageCount()) {
            return null;
        }
        
        try {
            PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);
            
            // Calculate dimensions based on zoom level
            int width = (int) (pageWidth * zoomLevel * 2); // 2x for high DPI
            int height = (int) (pageHeight * zoomLevel * 2);
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(0xFFFFFFFF); // White background
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            
            return bitmap;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public void onViewRecycled(@NonNull PageViewHolder holder) {
        super.onViewRecycled(holder);
        holder.pageImageView.setImageBitmap(null);
    }
    
    public void cleanup() {
        if (pdfRenderer != null) {
            try {
                pdfRenderer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Clear bitmaps to free memory
        for (Bitmap bitmap : pages) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        pages.clear();
    }
    
    static class PageViewHolder extends RecyclerView.ViewHolder {
        ImageView pageImageView;
        
        PageViewHolder(View itemView) {
            super(itemView);
            pageImageView = itemView.findViewById(R.id.pageImageView);
        }
    }
}
