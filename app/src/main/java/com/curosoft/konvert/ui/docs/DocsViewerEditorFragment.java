package com.curosoft.konvert.ui.docs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.curosoft.konvert.R;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DocsViewerEditorFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private DocsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_docs_viewer_editor, container, false);
        recyclerView = view.findViewById(R.id.docs_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DocsAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        loadDocuments();
        return view;
    }

    private void loadDocuments() {
        List<File> docs = DocsScanner.scanForDocuments(getContext());
        if (docs.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
            adapter.setFiles(docs);
        }
    }
}
