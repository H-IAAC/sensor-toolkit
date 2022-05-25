package br.org.eldorado.hiaac.datacollector.data;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

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

    public LiveData<List<LabelConfig>> getAllLabels() {
        return mAllLabels;
    }

    public LiveData<List<SensorFrequency>> getAllSensorFrequencies() {
        return mAllSensorFrequencies;
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

    public void insertLabeledData(LabeledData labeledData) {
        mRepository.insertLabeledData(labeledData);
    }

    public List<LabeledData> getLabeledData(String label, int type) {
        return mRepository.getLabeledData(label, type);
    }

    public void updateLabeledData(List<LabeledData> dt) {
        mRepository.updateLabeledData(dt);
    }

    public void deleteLabeledData(LabeledData label) {
        mRepository.deleteLabeledData(label);
    }
}
