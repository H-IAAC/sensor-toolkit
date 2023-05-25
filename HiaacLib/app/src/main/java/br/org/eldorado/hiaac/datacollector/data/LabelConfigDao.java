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
    @Query("SELECT * FROM LabelConfig ORDER BY experiment ASC")
    LiveData<List<LabelConfig>> getAllLabels();

    @Query("SELECT * FROM LabelConfig WHERE id=:id")
    LiveData<LabelConfig> getLabelConfigById(long id);

    @Insert
    long insert(LabelConfig labelConfig);

    @Update
    void update(LabelConfig labelConfig);

    @Delete
    void delete(LabelConfig labelConfig);

    @Query("SELECT * FROM sensorfrequency WHERE config_id=:configId")
    LiveData<List<SensorFrequency>> getAllSensorsFromLabel(long configId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSensorFrequencies(List<SensorFrequency> sensorFrequencies);

    @Delete
    void deleteAllSensorFrequencies(List<SensorFrequency> sensorFrequencies);

    @Query("DELETE FROM sensorfrequency WHERE config_id=:configId")
    void deleteSensorFromLabel(long configId);

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
    List<LabeledData> getLabeledData(long labelId, long offset);

    //@Query("SELECT * from LabeledData where `label-id`=:labelId and `data-used`=0 ORDER BY `sensor-name`, `sensor-timestamp` LIMIT 300000 OFFSET 0")
    @Transaction @Query("SELECT * from LabeledData where `label-id`=:labelId and `data-used`=0 ORDER BY `sensor-name`, `sensor-timestamp` LIMIT 300000 OFFSET 0")
    List<LabeledData> getLabeledDataCsv(long labelId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertExperimentStatistics(List<ExperimentStatistics> experiments);

    @Query("SELECT * from ExperimentStatistics where `experiment-id`=:expId")
    LiveData<List<ExperimentStatistics>> getStatisticsByExpId(long expId);

    @Query("DELETE from ExperimentStatistics where `experiment-id`=:expId")
    void deleteExperimentStatistics(long expId);
}
