package br.org.eldorado.sensorsdk.listener;

public interface SensorSDKListener {

    public void onSensorStarted();
    public void onSensorStopped();
    public void onSensorChanged();
}
