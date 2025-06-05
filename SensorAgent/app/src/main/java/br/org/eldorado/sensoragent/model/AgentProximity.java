package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class AgentProximity extends AgentSensorBase {
    private static final String TAG = "Proximity";
    private Sensor sensor;

    public AgentProximity() {
        super(TAG, TYPE_PROXIMITY);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        registerListener(sensor);
        values = new float[1];
        values[0] = 0;
    }
}
