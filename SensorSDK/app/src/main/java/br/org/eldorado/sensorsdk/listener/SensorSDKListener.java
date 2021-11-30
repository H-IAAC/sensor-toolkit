package br.org.eldorado.sensorsdk.listener;

import br.org.eldorado.sensoragent.model.SensorBase;

public interface SensorSDKListener {

    public void onSensorStarted(SensorBase sensor);
    public void onSensorStopped(SensorBase sensor);
    public void onSensorChanged(SensorBase sensor);
}
