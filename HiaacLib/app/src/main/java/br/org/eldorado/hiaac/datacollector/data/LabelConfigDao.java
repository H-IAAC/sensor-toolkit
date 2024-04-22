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
    @Query("SELECT * FROM LabelConfig ORDER BY experiment DESC")
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

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLabeledData(List<LabeledData> data);

    @Query("DELETE from LabeledData where `config-id`=:configId")
    void deleteLabeledData(long configId);

    @Transaction
    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateLabeledData(List<LabeledData> dt);

    @Query("SELECT * from LabeledData where `config-id`=:configId ORDER BY `sensor-name`, `sensor-timestamp` LIMIT 300000 OFFSET :offset")
    List<LabeledData> getLabeledData(long configId, long offset);

    @Query("SELECT * from LabeledData where `config-id`=:configId and `data-used`=0 ORDER BY `sensor-name`, `sensor-timestamp` LIMIT 300000")
    List<LabeledData> getLabeledDataCsv(long configId);

    @Query("SELECT * from LabeledData where `config-id`=:configId LIMIT 1")
    LabeledData getLabeledData(long configId);

    @Query("SELECT count(*) from LabeledData where `config-id`=:configId and `data-used`=0")
    Integer countLabeledDataCsv(long configId);

    @Query("SELECT uid from LabeledData where `config-id`=:configId and `data-used`=0 LIMIT 1")
    String getLabeledDataUidCsv(long configId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertExperimentStatistics(List<ExperimentStatistics> experiments);

    @Query("SELECT * from ExperimentStatistics where `config-id`=:configId and `start-time` LIKE :startTime")
    LiveData<List<ExperimentStatistics>> getStatisticsByExpId(long configId, String startTime);

    @Query("SELECT * from ExperimentStatistics where `config-id`=:configId and `start-time` LIKE :startTime")
    List<ExperimentStatistics> getStatistics(long configId, String startTime);

    @Query("DELETE from ExperimentStatistics where `config-id`=:configId")
    void deleteExperimentStatistics(long configId);

    @Query("DELETE from ExperimentStatistics where `config-id`=:configId and `start-time` LIKE :startTime")
    void deleteExperimentStatistics(long configId, String startTime);
}
