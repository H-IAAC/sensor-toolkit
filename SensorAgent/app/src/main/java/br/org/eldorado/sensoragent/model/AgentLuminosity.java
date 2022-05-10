package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class AgentLuminosity extends AgentSensorBase {
    private static final String TAG = "Luminosity";
    private Sensor sensor;

    public AgentLuminosity() {
        super(TAG, TYPE_LIGHT);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        registerListener(sensor);
        values = new float[1];
        values[0] = 0;
    }
}