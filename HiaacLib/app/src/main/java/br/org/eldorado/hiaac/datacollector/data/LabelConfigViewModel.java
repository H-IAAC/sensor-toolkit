package br.org.eldorado.hiaac.datacollector.data;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LabelConfigViewModel extends AndroidViewModel {
    private LabelConfigRepository mRepository;
    private LiveData<List<LabelConfig>> mAllLabels;
    private LiveData<List<SensorFrequency>> mAllSensorFrequencies;

    public LabelConfigViewModel(Application application) {
        super(application);
        mRepository = new LabelConfigRepository(application);
        mAllLabels = mRepository.getAllLabels();
        mAllSensorFrequencies = mRepository.getAllSensorFrequencies();
    }

    public LabelConfigRepository getLabelConfigRepository() {
        return mRepository;
    }

    public LiveData<List<LabelConfig>> getAllLabels() {
        return mAllLabels;
    }

    public LiveData<List<SensorFrequency>> getAllSensorFrequencies() {
        return mAllSensorFrequencies;
    }

    public LiveData<LabelConfig> getLabelConfigById(long id) {
        return mRepository.getLabelConfigById(id);
    }

    public long insertNewConfig(LabelConfig config) throws ExecutionException, InterruptedException {
        return mRepository.insertNewConfig(config);
    }

    public void updateConfig(LabelConfig config) {
        mRepository.updateConfig(config);
    }

    public void deleteConfig(LabelConfig config) {
        mRepository.deleteConfig(config);
        // When a config is deleted:
        // 1) remove related statistics must be removed
        deleteExperimentsStatistics(config.id);
        // 2) remove related labeled data
        deleteLabeledData(config.id);
    }

    public LiveData<List<SensorFrequency>> getAllSensorsFromLabel(long id) {
        return mRepository.getAllSensorsFromLabel(id);
    }

    public void deleteSensorsFromLabel(LabelConfig label) {
        mRepository.deleteSensorFromLabel(label);
    }

    public void insertAllSensorFrequencies(List<SensorFrequency> sensorFrequencies) {
        mRepository.insertAllSensorFrequencies(sensorFrequencies);
    }

    public void deleteAllSensorFrequencies(List<SensorFrequency> sensorFrequencies) {
        mRepository.deleteAllSensorFrequencies(sensorFrequencies);
    }

    public void insertLabeledData(List<LabeledData> labeledData) {
        mRepository.insertLabeledData(labeledData);
    }

    public void insertExperimentStatistics(List<ExperimentStatistics> statistics) {
        mRepository.insertExperimentStatistics(statistics);
    }

    public void deleteExperimentsStatistics(long configId) {
        mRepository.deleteExperimentStatistics(configId);
    }

    public List<LabeledData> getLabeledData(long labelId, int type, long offset) {
        return mRepository.getLabeledData(labelId, type, offset);
    }

    public LiveData<List<ExperimentStatistics>> getExperimentStatistics(long expId, String startTime) {
        return mRepository.getExperimentStatisticsByExpId(expId, startTime);
    }

    public void updateLabeledData(List<LabeledData> dt) {
        mRepository.updateLabeledData(dt);
    }

    public void deleteLabeledData(LabeledData label) {
        mRepository.deleteLabeledData(label);
    }

    public void deleteLabeledData(long configId) {
        mRepository.deleteLabeledData(configId);
    }
}
