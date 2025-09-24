package br.org.eldorado.hiaac.datacollector.service.listener;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import br.org.eldorado.hiaac.datacollector.data.LabeledData;
import br.org.eldorado.hiaac.datacollector.model.DataTrack;

public interface ExecutionServiceListener {

    DataTrack getDataTrack();
    void onRunning(long remainingTime);
    void onStopped();
    void onStarted();
    void onError(String errorMessage);
    void onExtraSensoryConversion(Map<Integer, List<LabeledData>> extraSensoryData, ByteArrayInputStream audioData);
}
