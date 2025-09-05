package com.curosoft.konvert.ui.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.curosoft.konvert.R;
import com.curosoft.konvert.data.database.entities.ConvertedFile;
import com.curosoft.konvert.data.repository.ConvertedFileRepository;
import com.curosoft.konvert.ui.dashboard.models.RecentFile;
import com.curosoft.konvert.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class DashboardViewModel extends AndroidViewModel {
    
    private ConvertedFileRepository repository;
    private LiveData<List<RecentFile>> recentFilesLiveData;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        repository = new ConvertedFileRepository(application);
        
        // Transform database entities to UI models
        recentFilesLiveData = Transformations.map(
            repository.getRecentFilesLiveData(),
            this::convertDatabaseFilesToRecentFiles
        );
    }

    public LiveData<List<RecentFile>> getRecentFiles() {
        return recentFilesLiveData;
    }

    public void addConvertedFile(String fileName, String filePath, String fromFormat, String toFormat) {
        String conversionType = fromFormat.toUpperCase() + "_TO_" + toFormat.toUpperCase();
        ConvertedFile convertedFile = new ConvertedFile(
            fileName,
            filePath,
            conversionType,
            System.currentTimeMillis()
        );
        repository.insertConvertedFile(convertedFile);
    }

    private List<RecentFile> convertDatabaseFilesToRecentFiles(List<ConvertedFile> convertedFiles) {
        List<RecentFile> recentFiles = new ArrayList<>();
        
        for (ConvertedFile convertedFile : convertedFiles) {
            // Determine icon based on file type
            int iconRes = getIconForFile(convertedFile.getFileName());
            
            // Create RecentFile from ConvertedFile
            RecentFile recentFile = new RecentFile(convertedFile, iconRes);
            recentFiles.add(recentFile);
        }
        
        return recentFiles;
    }

    private int getIconForFile(String fileName) {
        if (fileName == null) return R.drawable.ic_description;
        
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        
        switch (extension) {
            case "pdf":
                return R.drawable.ic_description;
            case "docx":
            case "doc":
                return R.drawable.ic_description;
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                return R.drawable.ic_image_outline;
            case "mp3":
            case "wav":
            case "aac":
                return R.drawable.ic_audio_outline;
            case "mp4":
            case "avi":
            case "mov":
                return R.drawable.ic_video_outline;
            default:
                return R.drawable.ic_description;
        }
    }
}
