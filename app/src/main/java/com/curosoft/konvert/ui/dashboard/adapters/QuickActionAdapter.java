package com.curosoft.konvert.ui.dashboard.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.curosoft.konvert.R;
import com.curosoft.konvert.ui.dashboard.models.QuickAction;

import java.util.List;

public class QuickActionAdapter extends RecyclerView.Adapter<QuickActionAdapter.ViewHolder> {
    
    private List<QuickAction> quickActions;
    private OnQuickActionClickListener listener;

    public interface OnQuickActionClickListener {
        void onQuickActionClick(QuickAction quickAction);
    }

    public QuickActionAdapter(List<QuickAction> quickActions) {
        this.quickActions = quickActions;
    }

    public void setOnQuickActionClickListener(OnQuickActionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_action, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickAction quickAction = quickActions.get(position);
        holder.bind(quickAction);
    }

    @Override
    public int getItemCount() {
        return quickActions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImageView;
        private TextView titleTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.quick_action_icon);
            titleTextView = itemView.findViewById(R.id.quick_action_label);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onQuickActionClick(quickActions.get(getAdapterPosition()));
                }
            });
        }

        public void bind(QuickAction quickAction) {
            iconImageView.setImageResource(quickAction.getIconRes());
            titleTextView.setText(quickAction.getTitle());
        }
    }
}
