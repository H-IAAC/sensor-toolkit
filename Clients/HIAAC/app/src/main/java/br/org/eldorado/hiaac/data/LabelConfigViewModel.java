package br.org.eldorado.hiaac.data;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class LabelConfigViewModel extends AndroidViewModel {
    private LabelConfigRepository mRepository;
    private LiveData<List<LabelConfig>> mAllLabels;

    public LabelConfigViewModel(Application application) {
        super(application);
        mRepository = new LabelConfigRepository(application);
        mAllLabels = mRepository.getAllLabels();
    }

    public LiveData<List<LabelConfig>> getAllLabels() {
        return mAllLabels;
    }

    public LiveData<LabelConfig> getLabelConfigById(String id) {
        return mRepository.getLabelConfigById(id);
    }

    public void insertNewConfig(LabelConfig config) {
        mRepository.insertNewConfig(config);
    }

    public void updateConfig(LabelConfig config) {
        mRepository.updateConfig(config);
    }

    public void deleteConfig(LabelConfig config) {
        mRepository.deleteConfig(config);
    }

    public LiveData<List<SensorFrequency>> getAllSensorsFromLabel(String label) {
        return mRepository.getAllSensorsFromLabel(label);
    }

    public void insertAllSensorFrequencies(List<SensorFrequency> sensorFrequencies) {
        mRepository.insertAllSensorFrequencies(sensorFrequencies);
    }

    public void deleteAllSensorFrequencies(List<SensorFrequency> sensorFrequencies) {
        mRepository.deleteAllSensorFrequencies(sensorFrequencies);
    }
}
