package com.curosoft.konvert.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;

import com.curosoft.konvert.data.database.AppDatabase;
import com.curosoft.konvert.data.database.dao.ConvertedFileDao;
import com.curosoft.konvert.data.database.entities.ConvertedFile;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConvertedFileRepository {
    
    private ConvertedFileDao convertedFileDao;
    private ExecutorService executorService;
    private LiveData<List<ConvertedFile>> recentFilesLiveData;

    public ConvertedFileRepository(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        convertedFileDao = database.convertedFileDao();
        executorService = Executors.newFixedThreadPool(2);
        recentFilesLiveData = convertedFileDao.getRecentFilesLiveData();
    }

    // Insert a new converted file (background thread)
    public void insertConvertedFile(ConvertedFile convertedFile) {
        executorService.execute(() -> convertedFileDao.insertFile(convertedFile));
    }

    // Get LiveData for reactive updates
    public LiveData<List<ConvertedFile>> getRecentFilesLiveData() {
        return recentFilesLiveData;
    }

    // Get recent files synchronously (for legacy code)
    public List<ConvertedFile> getRecentFiles() {
        return convertedFileDao.getRecentFiles();
    }

    // Get recent files asynchronously (for legacy code)
    public void getRecentFilesAsync(RecentFilesCallback callback) {
        executorService.execute(() -> {
            List<ConvertedFile> files = convertedFileDao.getRecentFiles();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                callback.onResult(files)
            );
        });
    }

    // Get count of converted files
    public void getConvertedFilesCountAsync(CountCallback callback) {
        executorService.execute(() -> {
            int count = convertedFileDao.getConvertedFilesCount();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                callback.onResult(count)
            );
        });
    }

    public interface RecentFilesCallback {
        void onResult(List<ConvertedFile> files);
    }

    public interface CountCallback {
        void onResult(int count);
    }
}
