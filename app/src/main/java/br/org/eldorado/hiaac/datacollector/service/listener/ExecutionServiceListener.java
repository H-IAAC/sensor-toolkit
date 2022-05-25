package br.org.eldorado.hiaac.datacollector.service.listener;

import br.org.eldorado.hiaac.datacollector.model.DataTrack;

public interface ExecutionServiceListener {

    DataTrack getDataTrack();
    void onRunning(long remainingTime);
    void onStopped();
    void onStarted();
}
