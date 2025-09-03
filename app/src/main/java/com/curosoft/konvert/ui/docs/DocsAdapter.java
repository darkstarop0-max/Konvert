package com.curosoft.konvert.ui.docs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.curosoft.konvert.R;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class DocsAdapter extends RecyclerView.Adapter<DocsAdapter.DocViewHolder> {
    private List<File> files;
    public DocsAdapter(List<File> files) {
        this.files = files;
    }
    public void setFiles(List<File> files) {
        this.files = files;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public DocViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doc, parent, false);
        return new DocViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull DocViewHolder holder, int position) {
        File file = files.get(position);
        holder.fileName.setText(file.getName());
        holder.filePath.setText(file.getAbsolutePath());
        holder.fileDate.setText(DateFormat.getDateTimeInstance().format(new Date(file.lastModified())));
        holder.fileIcon.setImageResource(getIconForFile(file.getName()));
        holder.itemView.setOnClickListener(v ->
            Toast.makeText(v.getContext(), "Open: " + file.getName(), Toast.LENGTH_SHORT).show()
        );
    }
    @Override
    public int getItemCount() {
        return files == null ? 0 : files.size();
    }
    static class DocViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileName, filePath, fileDate;
        DocViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileName = itemView.findViewById(R.id.file_name);
            filePath = itemView.findViewById(R.id.file_path);
            fileDate = itemView.findViewById(R.id.file_date);
        }
    }
    private int getIconForFile(String name) {
        // Use a built-in Android icon for all file types for now
        return android.R.drawable.ic_menu_save;
    }
}
