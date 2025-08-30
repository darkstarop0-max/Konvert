package com.curosoft.konvert.ui.conversion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;

import java.util.List;

public class FormatAdapter extends RecyclerView.Adapter<FormatAdapter.FormatViewHolder> {

    private final List<String> formats;
    private int selectedPosition = -1;
    private OnFormatSelectedListener listener;
    
    public interface OnFormatSelectedListener {
        void onFormatSelected(String format);
    }
    
    public FormatAdapter(List<String> formats) {
        this.formats = formats;
    }
    
    public void setOnFormatSelectedListener(OnFormatSelectedListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FormatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_format, parent, false);
        return new FormatViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FormatViewHolder holder, int position) {
        holder.bind(formats.get(position), position == selectedPosition);
    }
    
    @Override
    public int getItemCount() {
        return formats.size();
    }
    
    class FormatViewHolder extends RecyclerView.ViewHolder {
        private final TextView formatText;
        private final ConstraintLayout container;
        
        public FormatViewHolder(@NonNull View itemView) {
            super(itemView);
            formatText = itemView.findViewById(R.id.format_text);
            container = itemView.findViewById(R.id.format_container);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    int previousSelected = selectedPosition;
                    selectedPosition = position;
                    
                    // Update the UI for the previously selected item and the newly selected item
                    if (previousSelected != -1) {
                        notifyItemChanged(previousSelected);
                    }
                    notifyItemChanged(selectedPosition);
                    
                    // Notify the listener
                    if (listener != null) {
                        listener.onFormatSelected(formats.get(position));
                    }
                }
            });
        }
        
        public void bind(String format, boolean isSelected) {
            formatText.setText(format);
            
            // Update the UI based on selection state
            if (isSelected) {
                container.setBackgroundResource(R.drawable.bg_format_selected);
                formatText.setTextColor(itemView.getContext().getResources().getColor(R.color.text_on_primary));
            } else {
                container.setBackgroundResource(R.drawable.bg_format_normal);
                formatText.setTextColor(itemView.getContext().getResources().getColor(R.color.text_primary));
            }
        }
    }
}
