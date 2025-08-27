package br.org.eldorado.hiaac.datacollector.service.listener;

import java.util.List;
import java.util.Map;

import br.org.eldorado.hiaac.datacollector.data.LabeledData;
import br.org.eldorado.hiaac.datacollector.model.DataTrack;

public class ExecutionServiceListenerAdapter implements ExecutionServiceListener {

    private final DataTrack dataTrack;

    public ExecutionServiceListenerAdapter(DataTrack dt) {
        this.dataTrack = dt;
    }

    public DataTrack getDataTrack() {
        return dataTrack;
    }

    @Override
    public void onRunning(long remainingTime) {}

    @Override
    public void onStopped() {}

    @Override
    public void onStarted() {}

    @Override
    public void onError(String message){}

    @Override
    public void onExtraSensoryConversion(Map<Integer, List<LabeledData>> extraSensoryData) {}
}
