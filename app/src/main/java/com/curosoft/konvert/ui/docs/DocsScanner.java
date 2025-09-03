package com.curosoft.konvert.ui.docs;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DocsScanner {
    private static final String[] SUPPORTED_EXTENSIONS = {".pdf", ".docx", ".txt", ".rtf", ".odt"};
    public static List<File> scanForDocuments(Context context) {
        List<File> docs = new ArrayList<>();
        File root = Environment.getExternalStorageDirectory();
        scanDir(root, docs);
        return docs;
    }
    private static void scanDir(File dir, List<File> docs) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDir(file, docs);
            } else {
                for (String ext : SUPPORTED_EXTENSIONS) {
                    if (file.getName().toLowerCase().endsWith(ext)) {
                        docs.add(file);
                        break;
                    }
                }
            }
        }
    }
}
