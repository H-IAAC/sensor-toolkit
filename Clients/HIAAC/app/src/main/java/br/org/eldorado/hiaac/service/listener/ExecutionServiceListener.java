package br.org.eldorado.hiaac.service.listener;

import br.org.eldorado.hiaac.model.DataTrack;

public interface ExecutionServiceListener {

    DataTrack getDataTrack();
    void onRunning(long remainingTime);
    void onStopped();
    void onStarted();
}
