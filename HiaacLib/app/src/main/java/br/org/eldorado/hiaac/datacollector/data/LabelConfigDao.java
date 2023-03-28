package br.org.eldorado.hiaac.datacollector.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
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

    @Query("SELECT * FROM sensorfrequency WHERE label_id=:label")
    LiveData<List<SensorFrequency>> getAllSensorsFromLabel(String label);

    @Query("DELETE FROM sensorfrequency WHERE label_id=:label")
    void deleteSensorFromLabel(String label);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSensorFrequencies(List<SensorFrequency> sensorFrequencies);

    @Delete
    void deleteAllSensorFrequencies(List<SensorFrequency> sensorFrequencies);

    @Query("SELECT * FROM sensorfrequency")
    LiveData<List<SensorFrequency>> getAllSensorFrequencies();

    /* Table with sensors data for one label config */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLabeledData(LabeledData data);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLabeledData(List<LabeledData> data);

    @Query("DELETE from LabeledData where `label-name`=:label")
    void deleteLabeledData(String label);

    @Update
    void updateLabeledData(List<LabeledData> dt);

    @Transaction @Query("SELECT * from LabeledData where `label-id`=:labelId ORDER BY `sensor-name`, `sensor-timestamp` LIMIT 300000 OFFSET :offset")
    List<LabeledData> getLabeledData(int labelId, long offset);

    //@Query("SELECT * from LabeledData where `label-id`=:labelId and `data-used`=0 ORDER BY `sensor-name`, `sensor-timestamp` LIMIT 300000 OFFSET 0")
    @Transaction @Query("SELECT * from LabeledData where `label-id`=:labelId and `data-used`=0 ORDER BY `sensor-name`, `sensor-timestamp` LIMIT 300000 OFFSET 0")
    List<LabeledData> getLabeledDataCsv(int labelId);
}
