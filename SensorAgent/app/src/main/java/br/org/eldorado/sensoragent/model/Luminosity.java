package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class Luminosity extends SensorBase {
    private static final String TAG = "Luminosity";
    private Sensor sensor;

    public Luminosity() {
        super(TAG, TYPE_LIGHT);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        registerListener(sensor);
    }
}