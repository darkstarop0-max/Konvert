package com.curosoft.konvert.data.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.curosoft.konvert.data.database.dao.ConvertedFileDao;
import com.curosoft.konvert.data.database.entities.ConvertedFile;

@Database(
    entities = {ConvertedFile.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static volatile AppDatabase INSTANCE;
    
    public abstract ConvertedFileDao convertedFileDao();
    
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "konvert_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
