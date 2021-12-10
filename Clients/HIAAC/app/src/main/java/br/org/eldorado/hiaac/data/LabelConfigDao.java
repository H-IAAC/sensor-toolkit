package br.org.eldorado.hiaac.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LabelConfigDao {
    @Query("SELECT * FROM LabelConfig ORDER BY label ASC")
    LiveData<List<LabelConfig>> getAllLabels();

    @Query("SELECT * FROM LabelConfig WHERE label=:id")
    LiveData<LabelConfig> getLabelConfigById(String id);

    @Insert
    void insert(LabelConfig labelConfig);

    @Update
    void update(LabelConfig labelConfig);

    @Delete
    void delete(LabelConfig labelConfig);

    @Query("SELECT * FROM sensorfrequency WHERE label=:label")
    LiveData<List<SensorFrequency>> getAllSensorsFromLabel(String label);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSensorFrequencies(List<SensorFrequency> sensorFrequencies);

    @Delete
    void deleteAllSensorFrequencies(List<SensorFrequency> sensorFrequencies);
}
