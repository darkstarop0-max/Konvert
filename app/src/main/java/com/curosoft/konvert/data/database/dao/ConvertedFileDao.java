package com.curosoft.konvert.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.curosoft.konvert.data.database.entities.ConvertedFile;

import java.util.List;

@Dao
public interface ConvertedFileDao {
    
    @Insert
    void insertFile(ConvertedFile file);

    @Query("SELECT * FROM converted_files ORDER BY timestamp DESC LIMIT 6")
    LiveData<List<ConvertedFile>> getRecentFilesLiveData();

    @Query("SELECT * FROM converted_files ORDER BY timestamp DESC LIMIT 6")
    List<ConvertedFile> getRecentFiles();

    @Query("SELECT COUNT(*) FROM converted_files")
    int getConvertedFilesCount();

    @Query("DELETE FROM converted_files WHERE id = :id")
    void deleteFile(int id);

    @Query("DELETE FROM converted_files")
    void deleteAllFiles();
}
