package br.org.eldorado.hiaac.datacollector.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SimpleSQLiteQuery;

@Database(entities = {LabelConfig.class, SensorFrequency.class, LabeledData.class, ExperimentStatistics.class}, version = 3, exportSchema = false)
@TypeConverters({SensorConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "main_db";

    public abstract LabelConfigDao labelConfigDao();
    public abstract DbConfig dbConfig();
    private static AppDatabase INSTANCE;

    static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                                    AppDatabase.class, DATABASE_NAME)
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration().build();
                    SimpleSQLiteQuery query = new SimpleSQLiteQuery("PRAGMA synchronous = NORMAL");
                    INSTANCE.dbConfig().setPragma(query);
                    query = new SimpleSQLiteQuery("PRAGMA cache_size = 10000");
                    INSTANCE.dbConfig().setPragma(query);
                }
            }
        }
        return INSTANCE;
    }
}
