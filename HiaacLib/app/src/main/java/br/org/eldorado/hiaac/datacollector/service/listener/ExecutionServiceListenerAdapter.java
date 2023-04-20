package br.org.eldorado.hiaac.datacollector.service.listener;

import br.org.eldorado.hiaac.datacollector.model.DataTrack;

public class ExecutionServiceListenerAdapter implements ExecutionServiceListener {

    private DataTrack dataTrack;

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
}
