package br.org.eldorado.hiaac.datacollector.data;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutionException;

import br.org.eldorado.hiaac.datacollector.util.AlarmConfig;

public class LabelConfigViewModel extends AndroidViewModel {
    private final Repository repository;
    private final LiveData<List<LabelConfig>> mAllLabels;
    private final LiveData<List<SensorFrequency>> mAllSensorFrequencies;

    public LabelConfigViewModel(Application application) {
        super(application);
        repository = new Repository(application);
        mAllLabels = repository.getLabelConfigRepositoryInstance().getAllLabels();
        mAllSensorFrequencies = repository.getLabelConfigRepositoryInstance().getAllSensorFrequencies();
    }

    public LiveData<List<LabelConfig>> getAllLabels() {
        return mAllLabels;
    }

    public LiveData<List<SensorFrequency>> getAllSensorFrequencies() {
        return mAllSensorFrequencies;
    }

    public LiveData<LabelConfig> getLabelConfigById(long id) {
        return Repository.getLabelConfigRepositoryInstance().getLabelConfigById(id);
    }

    public long insertNewConfig(LabelConfig config) throws ExecutionException, InterruptedException {
        return Repository.getLabelConfigRepositoryInstance().insertNewConfig(config);
    }

    public void updateConfig(LabelConfig config) {
        Repository.getLabelConfigRepositoryInstance().updateConfig(config);
    }

    public void deleteConfig(LabelConfig config) {
        Repository.getLabelConfigRepositoryInstance().deleteConfig(config);
        // When a config is deleted:
        // 1) remove related statistics must be removed
        deleteExperimentsStatistics(config.id);
        // 2) remove related labeled data
        deleteLabeledData(config.id);
        // 3) check if alarm is configured/enabled for this config
        if (AlarmConfig.getActiveConfigure().idConfigured == config.id)
            AlarmConfig.cancelAlarm();
    }

    public LiveData<List<SensorFrequency>> getAllSensorsFromLabel(long id) {
        return Repository.getLabelConfigRepositoryInstance().getAllSensorsFromLabel(id);
    }

    public void deleteSensorsFromLabel(LabelConfig label) {
        Repository.getLabelConfigRepositoryInstance().deleteSensorFromLabel(label);
    }

    public void insertAllSensorFrequencies(List<SensorFrequency> sensorFrequencies) {
        Repository.getLabelConfigRepositoryInstance().insertAllSensorFrequencies(sensorFrequencies);
    }

    public void deleteAllSensorFrequencies(List<SensorFrequency> sensorFrequencies) {
        Repository.getLabelConfigRepositoryInstance().deleteAllSensorFrequencies(sensorFrequencies);
    }

    public void insertLabeledData(List<LabeledData> labeledData) {
        Repository.getLabelConfigRepositoryInstance().insertLabeledData(labeledData);
    }

    public void insertExperimentStatistics(List<ExperimentStatistics> statistics) {
        Repository.getLabelConfigRepositoryInstance().insertExperimentStatistics(statistics);
    }

    public void deleteExperimentsStatistics(long configId) {
        Repository.getLabelConfigRepositoryInstance().deleteExperimentStatistics(configId);
    }

    public List<LabeledData> getLabeledData(long labelId, int type, long offset) {
        return Repository.getLabelConfigRepositoryInstance().getLabeledData(labelId, type, offset);
    }

    public LabeledData getLabeledData(long labelId) {
        return Repository.getLabelConfigRepositoryInstance().getLabeledData(labelId);
    }

    public Integer countLabeledDataCsv(long labelId) {
        return Repository.getLabelConfigRepositoryInstance().countLabeledDataCsv(labelId);
    }

    public Boolean labeledDataExists(long labelId) {
        return Repository.getLabelConfigRepositoryInstance().labeledDataExists(labelId);
    }

    public String getLabeledDataUidCsv(long labelId) {
        return Repository.getLabelConfigRepositoryInstance().getLabeledDataUidCsv(labelId);
    }

    public LiveData<List<ExperimentStatistics>> getExperimentStatistics(long expId, String startTime) {
        return Repository.getLabelConfigRepositoryInstance().getExperimentStatisticsByExpId(expId, startTime);
    }

    public void updateLabeledData(List<LabeledData> dt) {
        Repository.getLabelConfigRepositoryInstance().updateLabeledData(dt);
    }

    public void deleteLabeledData(LabeledData label) {
        Repository.getLabelConfigRepositoryInstance().deleteLabeledData(label);
    }

    public void deleteLabeledData(long configId) {
        Repository.getLabelConfigRepositoryInstance().deleteLabeledData(configId);
    }
}
