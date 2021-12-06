package br.org.eldorado.hiaac.service.listener;

public interface ExecutionServiceListener {

    public void onRunning(long remainingTime);
    public void onStopped();
    public void onStarted();
}
